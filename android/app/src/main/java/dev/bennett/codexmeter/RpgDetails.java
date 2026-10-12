package dev.bennett.codexmeter;

import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import java.util.*;
import org.json.*;

/** Lightweight details from the journal already loaded by GrowthActivity. No network requests. */
final class RpgDetails {
    private RpgDetails() {}
    static LinearLayout body(GrowthActivity a,String kind,JSONObject state,boolean dark){
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(a,20),Ui.dp(a,12),Ui.dp(a,20),Ui.dp(a,20));
        if(kind.equals("plan")){plans(a,body,dark);return body;}
        if(state==null){body.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_unknown),dark));return body;}
        if(kind.equals("level"))level(a,body,state,dark);else if(kind.equals("tokens"))RpgGrowthUi.tokens(a,body,state,dark);else tier(a,body,state,kind,dark);return body;
    }
    static AlertDialog show(GrowthActivity a,String kind,JSONObject state,boolean dark){
        ScrollView scroll=new ScrollView(a);scroll.addView(body(a,kind,state,dark));
        int title=kind.equals("level")?R.string.rpg3_level:kind.equals("plan")?R.string.growth_plan_compare:kind.equals("KR")?R.string.rpg3_korea:R.string.rpg3_world;
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(scroll).setPositiveButton(R.string.growth_close,null).create();dialog.show();
        scroll.post(()->{if(dialog.isShowing()){int available=(int)(a.getResources().getDisplayMetrics().heightPixels*.7);if(scroll.getHeight()>available){scroll.getLayoutParams().height=available;scroll.requestLayout();}}});return dialog;
    }
    private static void line(GrowthActivity a,LinearLayout body,int id,boolean dark,Object...args){body.addView(LedgerUi.caption(a,a.getString(id,args),dark));Ui.addSpacer(body,8);}
    private static void tierEvidence(GrowthActivity a,LinearLayout body,JSONObject state,String region,boolean dark){
        JSONObject rank=state.optJSONObject(region);body.addView(RpgGrowthUi.tier(a,rank,region,dark));
        line(a,body,region.equals("KR")?R.string.growth_kr_note:R.string.growth_world_note,dark);
        LedgerUi.section(body,a.getString(R.string.growth_factors),dark);
        line(a,body,R.string.growth_active,dark,state.optInt("active_days_30"),state.optInt("activity_days"));
        line(a,body,R.string.growth_streak,dark,state.optInt("longest_observed_streak"));line(a,body,R.string.growth_weeks,dark,state.optInt("valid_weeks"));
        JSONObject activity=state.optJSONObject("activity");long latest=0;if(activity!=null)for(String key:AccountProgress.keys(activity))latest=Math.max(latest,activity.optLong(key));
        if(latest>0)line(a,body,R.string.growth_last_activity,dark,LedgerAggregation.day(latest).toString());
        line(a,body,R.string.growth_token_factor,dark,state.optString("token_status").equals("observed")?RpgGrowthUi.number(state.optLong("token_total")):"—");
        line(a,body,R.string.rpg3_token_note,dark);
        String basis=state.optString("quota_score_basis");line(a,body,basis.equals("actual_billing_period_utilization")?R.string.growth_quota_bill:basis.startsWith("certified_week_scores")?R.string.growth_quota_week:R.string.growth_quota_unknown,dark);
        if(rank!=null&&rank.has("latest_score")){
            double score=rank.optDouble("latest_score");line(a,body,R.string.growth_score,dark,LedgerUi.number(score));
            int grade=rank.optInt("tier",-1);ProgressBar bar=Ui.progress(a,dark);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(RpgGrowthUi.rankAccent(grade,dark)));bar.setMax(1000);bar.setProgress((int)(10*Math.max(0,Math.min(100,score))));bar.setContentDescription(a.getString(R.string.growth_score,LedgerUi.number(score)));body.addView(bar);Ui.addSpacer(body,10);
            if(grade>=9)line(a,body,R.string.growth_top,dark);else{
                line(a,body,R.string.growth_next_rank,dark,RpgGrowthUi.rank(a,Math.max(0,grade+1)));
                double next=grade<0?0:RpgTier.BOUNDS[Math.min(8,grade)];line(a,body,R.string.growth_next_gap,dark,LedgerUi.number(Math.max(0,next-score)));
            }
            if(rank.optBoolean("provisional")){line(a,body,R.string.fun_provisional,dark);line(a,body,R.string.ref_promotion_held,dark);}
            String status=state.optString("demotion_status");line(a,body,status.equals("comparable_window_evaluated")?R.string.ref_demotion_checked:status.equals("held_unverified_timezone_or_account_coverage")?R.string.ref_demotion_scope:R.string.ref_demotion_held,dark);
            if(rank.optLong("evaluated_at")>0)line(a,body,R.string.growth_evaluated,dark,V3Display.time(rank.optLong("evaluated_at")));
        }else line(a,body,R.string.rpg3_unknown,dark);
        line(a,body,R.string.growth_protection,dark);
    }
    private static void levelEvidence(GrowthActivity a,LinearLayout body,JSONObject state,boolean dark){
        long xp=state.optLong("xp");int level=RpgEngine.level(xp);body.addView(LedgerUi.amount(a,"Lv. "+level,dark,38));line(a,body,R.string.rpg3_xp,dark,RpgGrowthUi.number(xp));
        line(a,body,R.string.rpg3_next_xp,dark,RpgGrowthUi.number(RpgEngine.threshold(level+1)-xp),level+1);
        long[] sources=RpgGrowthFacts.xpSources(state);int[] labels={R.string.growth_xp_activity,R.string.growth_xp_tokens,R.string.growth_xp_quota,R.string.growth_xp_achievements};
        for(int i=0;i<labels.length;i++)line(a,body,labels[i],dark,RpgGrowthUi.number(sources[i]));
        line(a,body,R.string.ref_xp_milestone,dark);line(a,body,R.string.growth_xp_note,dark);line(a,body,R.string.growth_growth_line,dark);
        LedgerUi.section(body,a.getString(R.string.growth_recent_growth),dark);line(a,body,R.string.growth_no_level_history,dark);
        List<String> recent=RpgGrowthFacts.recentAchievements(state);JSONObject facts=state.optJSONObject("achievements");
        if(recent.isEmpty())line(a,body,R.string.rpg3_unknown,dark);
        for(String id:recent){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);body.addView(RpgGrowthUi.compact(a,e,dark));line(a,body,R.string.rpg3_earned,dark,V3Display.time(facts.optJSONObject(id).optLong("first_earned_at")));}
    }
    private static void tier(GrowthActivity a,LinearLayout body,JSONObject s,String region,boolean dark){
        JSONObject t=s.optJSONObject(region);body.addView(RpgGrowthUi.tier(a,t,region,dark));Ui.addSpacer(body,16);line(a,body,R.string.rpg3_estimate,dark);
        if(t!=null&&t.optBoolean("provisional"))line(a,body,R.string.fun_provisional,dark);
        LedgerUi.section(body,a.getString(R.string.growth_factors),dark);
        line(a,body,R.string.reborn_activity,dark,s.has("active_days_30")?a.getString(R.string.growth_active,s.optInt("active_days_30"),s.optInt("activity_days")):a.getString(R.string.rpg3_unknown));
        line(a,body,R.string.reborn_tokens,dark,"observed".equals(s.optString("token_status"))?RpgGrowthUi.number(s.optLong("token_total")):a.getString(R.string.rpg3_unknown));
        String basis=s.optString("quota_score_basis");line(a,body,R.string.reborn_util,dark,a.getString(basis.equals("actual_billing_period_utilization")?R.string.growth_quota_bill:basis.startsWith("certified_week_scores")?R.string.growth_quota_week:R.string.growth_quota_unknown));
        Ui.addSpacer(body,16);int grade=t==null?-1:t.optInt("tier",-1);if(grade>=9)line(a,body,R.string.growth_top,dark);else line(a,body,R.string.growth_next_rank,dark,RpgGrowthUi.rank(a,Math.max(0,grade+1)));
        if(t==null||t.optBoolean("provisional"))line(a,body,R.string.ref_promotion_held,dark);else if(t.has("latest_score")&&grade<9)line(a,body,R.string.growth_next_gap,dark,LedgerUi.number(Math.max(0,(grade<0?0:RpgTier.BOUNDS[Math.min(8,grade)])-t.optDouble("latest_score"))));
        if(t!=null&&t.optLong("evaluated_at")>0)line(a,body,R.string.growth_evaluated,dark,V3Display.time(t.optLong("evaluated_at")));
        LinearLayout extra=new LinearLayout(a);extra.setOrientation(LinearLayout.VERTICAL);extra.setVisibility(View.GONE);body.addView(LedgerUi.action(a,a.getString(R.string.reborn_criteria),false,dark,()->{if(extra.getChildCount()==0)tierEvidence(a,extra,s,region,dark);extra.setVisibility(extra.getVisibility()==View.GONE?View.VISIBLE:View.GONE);}));body.addView(extra);
    }
    private static void level(GrowthActivity a,LinearLayout body,JSONObject s,boolean dark){long xp=s.optLong("xp");int level=RpgEngine.level(xp);TextView hero=LedgerUi.amount(a,"Lv. "+level,dark,44);hero.setGravity(Gravity.CENTER);body.addView(hero);line(a,body,R.string.rpg3_xp,dark,RpgGrowthUi.number(xp));Ui.addSpacer(body,16);ProgressBar bar=Ui.progress(a,dark);bar.setMax(1000);long base=RpgEngine.threshold(level),next=RpgEngine.threshold(level+1);bar.setProgress((int)Math.min(1000,1000d*(xp-base)/(next-base)));bar.setContentDescription(a.getString(R.string.rpg3_next_xp,RpgGrowthUi.number(next-xp),level+1));body.addView(bar);line(a,body,R.string.rpg3_next_xp,dark,RpgGrowthUi.number(next-xp),level+1);line(a,body,R.string.reborn_level_note,dark);
        LedgerUi.section(body,a.getString(R.string.growth_recent_growth),dark);List<String> recent=RpgGrowthFacts.recentAchievements(s);int count=0;for(String id:recent){JSONObject f=s.optJSONObject("achievements").optJSONObject(id);JSONObject aj=AchievementJourney.journey(s),old=aj==null?null:aj.optJSONObject("legacy").optJSONObject("achievements").optJSONObject(id);if(old!=null&&old.optBoolean("unlocked"))continue;if(f!=null&&f.optLong("first_earned_at")>0&&s.optJSONObject("awards").optLong("achievement:"+id)>0){body.addView(RpgGrowthUi.compact(a,RpgGrowthFacts.entry(id),dark));line(a,body,R.string.rpg3_earned,dark,V3Display.time(f.optLong("first_earned_at")));if(++count>=3)break;}}if(count==0)line(a,body,R.string.reborn_no_xp_log,dark);
        long[] totals=RpgGrowthFacts.xpSources(s);int[] labels={R.string.growth_xp_activity,R.string.growth_xp_tokens,R.string.growth_xp_quota,R.string.growth_xp_achievements};for(int i=0;i<labels.length;i++)line(a,body,labels[i],dark,RpgGrowthUi.number(totals[i]));LinearLayout extra=new LinearLayout(a);extra.setOrientation(LinearLayout.VERTICAL);extra.setVisibility(View.GONE);body.addView(LedgerUi.action(a,a.getString(R.string.reborn_xp_all),false,dark,()->{if(extra.getChildCount()==0)for(String id:AccountProgress.keys(s.optJSONObject("awards")))extra.addView(LedgerUi.caption(a,id+" · "+RpgGrowthUi.number(s.optJSONObject("awards").optLong(id))+" XP",dark));extra.setVisibility(extra.getVisibility()==View.GONE?View.VISIBLE:View.GONE);}));body.addView(extra);
    }
    private static byte[] readAsset(java.io.InputStream stream)throws java.io.IOException{java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];for(int n;(n=stream.read(buffer))!=-1;){if(bytes.size()+n>65536)throw new java.io.IOException("Plan asset bound");bytes.write(buffer,0,n);}return bytes.toByteArray();}
    private static void plans(GrowthActivity a,LinearLayout body,boolean dark){UsageSnapshot snapshot=AppPreferences.loadSnapshot(a);String current=snapshot==null?null:snapshot.planType;body.addView(LedgerUi.heading(a,PlanArtwork.label(current),dark));SubscriptionCost paid=SubscriptionStore.load(a,snapshot);if(paid!=null)line(a,body,R.string.reborn_actual_price,dark,paid.amount.toPlainString(),paid.currency,paid.start.toString(),paid.end.toString());line(a,body,R.string.reborn_price_note,dark);line(a,body,R.string.reborn_plan_unknown,dark);
        try(java.io.InputStream stream=a.getAssets().open("plan-comparison-2026-10.json")){JSONObject table=new JSONObject(new String(readAsset(stream),java.nio.charset.StandardCharsets.UTF_8));JSONArray plans=table.getJSONArray("plans");for(int i=0;i<plans.length();i++){JSONObject p=plans.getJSONObject(i);LinearLayout card=Ui.card(a,dark);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);ImageView image=new ImageView(a);image.setImageResource(RpgPlanSeals.image(p.getString("key").equals("pro500")?"pro20x":p.getString("key")));image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(image,new LinearLayout.LayoutParams(Ui.dp(a,56),Ui.dp(a,56)));LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);text.addView(LedgerUi.heading(a,p.getString("name")+" · $"+p.getInt("monthly"),dark));text.addView(LedgerUi.caption(a,p.getString(RpgGrowthUi.korean(a)?"ko":"en"),dark));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginStart(Ui.dp(a,12));card.addView(text,lp);card.setFocusable(true);card.setContentDescription(p.getString("name")+" · USD "+p.getInt("monthly"));card.setOnClickListener(v->a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(p.optString("source")))));body.addView(card);Ui.addSpacer(body,12);}}catch(Exception e){line(a,body,R.string.rpg3_unknown,dark);}line(a,body,R.string.growth_plan_note,dark);body.addView(LedgerUi.action(a,a.getString(R.string.reborn_official),false,dark,()->a.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://chatgpt.com/pricing/")))));
    }
}
