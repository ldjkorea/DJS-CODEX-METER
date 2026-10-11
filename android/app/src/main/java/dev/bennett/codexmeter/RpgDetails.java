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
        if(kind.equals("level"))level(a,body,state,dark);else tier(a,body,state,kind,dark);return body;
    }
    static AlertDialog show(GrowthActivity a,String kind,JSONObject state,boolean dark){
        ScrollView scroll=new ScrollView(a);scroll.addView(body(a,kind,state,dark));
        int title=kind.equals("level")?R.string.rpg3_level:kind.equals("plan")?R.string.growth_plan_compare:kind.equals("KR")?R.string.rpg3_korea:R.string.rpg3_world;
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(title).setView(scroll).setPositiveButton(R.string.growth_close,null).create();dialog.show();
        scroll.post(()->{if(dialog.isShowing()){int available=(int)(a.getResources().getDisplayMetrics().heightPixels*.7);if(scroll.getHeight()>available){scroll.getLayoutParams().height=available;scroll.requestLayout();}}});return dialog;
    }
    private static void line(GrowthActivity a,LinearLayout body,int id,boolean dark,Object...args){body.addView(LedgerUi.caption(a,a.getString(id,args),dark));Ui.addSpacer(body,8);}
    private static void tier(GrowthActivity a,LinearLayout body,JSONObject state,String region,boolean dark){
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
            if(rank.optBoolean("provisional"))line(a,body,R.string.fun_provisional,dark);
            if(rank.optLong("evaluated_at")>0)line(a,body,R.string.growth_evaluated,dark,V3Display.time(rank.optLong("evaluated_at")));
        }else line(a,body,R.string.rpg3_unknown,dark);
        line(a,body,R.string.growth_protection,dark);
    }
    private static void level(GrowthActivity a,LinearLayout body,JSONObject state,boolean dark){
        long xp=state.optLong("xp");int level=RpgEngine.level(xp);body.addView(LedgerUi.amount(a,"Lv. "+level,dark,38));line(a,body,R.string.rpg3_xp,dark,RpgGrowthUi.number(xp));
        line(a,body,R.string.rpg3_next_xp,dark,RpgGrowthUi.number(RpgEngine.threshold(level+1)-xp),level+1);
        long[] sources=RpgGrowthFacts.xpSources(state);int[] labels={R.string.growth_xp_activity,R.string.growth_xp_tokens,R.string.growth_xp_quota,R.string.growth_xp_achievements};
        for(int i=0;i<labels.length;i++)line(a,body,labels[i],dark,RpgGrowthUi.number(sources[i]));
        line(a,body,R.string.growth_xp_note,dark);line(a,body,R.string.growth_growth_line,dark);
        LedgerUi.section(body,a.getString(R.string.growth_recent_growth),dark);line(a,body,R.string.growth_no_level_history,dark);
        List<String> recent=RpgGrowthFacts.recentAchievements(state);JSONObject facts=state.optJSONObject("achievements");
        if(recent.isEmpty())line(a,body,R.string.rpg3_unknown,dark);
        for(String id:recent){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);body.addView(RpgGrowthUi.compact(a,e,dark));line(a,body,R.string.rpg3_earned,dark,V3Display.time(facts.optJSONObject(id).optLong("first_earned_at")));}
    }
    private static void plans(GrowthActivity a,LinearLayout body,boolean dark){
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(a);String current=snapshot==null?null:snapshot.planType;
        String key=PlanArtwork.key(current);int[] labels={R.string.growth_plan_free,R.string.growth_plan_go,R.string.growth_plan_plus,R.string.growth_plan_prolite,R.string.growth_plan_pro};String[] plans={"free","go","plus","pro5x","pro20x"};
        line(a,body,R.string.growth_plan_note,dark);
        for(int i=0;i<plans.length;i++){
            boolean selected=key.equals(PlanArtwork.key(plans[i]));LinearLayout row=Ui.card(a,dark);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            if(selected)row.setBackground(LedgerUi.shape(a,Ui.controlSurface(a,dark),20));
            ImageView image=new ImageView(a);image.setImageResource(RpgPlanSeals.image(plans[i]));image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(image,new LinearLayout.LayoutParams(Ui.dp(a,64),Ui.dp(a,64)));
            LinearLayout copy=new LinearLayout(a);copy.setOrientation(LinearLayout.VERTICAL);copy.addView(LedgerUi.heading(a,PlanArtwork.label(plans[i]),dark));copy.addView(LedgerUi.caption(a,a.getString(labels[i]),dark));if(selected)copy.addView(LedgerUi.amount(a,a.getString(R.string.growth_current_plan),dark,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginStart(Ui.dp(a,12));row.addView(copy,lp);body.addView(row);Ui.addSpacer(body,10);
        }
    }
}
