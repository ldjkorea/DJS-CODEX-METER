package dev.bennett.codexmeter;

import android.content.Intent;
import android.view.Gravity;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;
import org.json.*;

/** Pure rendering of detached account facts. Never grants XP or re-evaluates a tier. */
final class RpgGrowthUi {
    private RpgGrowthUi(){}
    static String number(long n){return NumberFormat.getIntegerInstance().format(n);}
    static boolean korean(android.content.Context c){return c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko");}
    static String rank(android.content.Context c,int rank){return rank<0?c.getString(R.string.fun_placing):TierPresentation.ENGLISH[Math.min(9,rank)];}
    static LinearLayout tier(androidx.appcompat.app.AppCompatActivity a,JSONObject state,String region,boolean dark){
        int grade=state==null?-1:state.optInt("tier",-1),highest=state==null?-1:state.optInt("highest",-1);
        LinearLayout card=Ui.card(a,dark);TierTheme.frame(card,grade,dark);card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView label=LedgerUi.caption(a,a.getString(region.equals("KR")?R.string.rpg3_korea:R.string.rpg3_world),dark);label.setGravity(Gravity.CENTER);card.addView(label);Ui.addSpacer(card,8);
        ImageView crest=new ImageView(a);crest.setImageResource(TierTheme.EMBLEMS[Math.max(0,grade)]);crest.setScaleType(ImageView.ScaleType.FIT_CENTER);crest.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);if(grade<0)crest.setAlpha(.35f);
        card.addView(crest,new LinearLayout.LayoutParams(-1,Ui.dp(a,124)));
        TextView name=LedgerUi.amount(a,rank(a,grade),dark,18);name.setGravity(Gravity.CENTER);card.addView(name,new LinearLayout.LayoutParams(-1,-2));
        TextView best=LedgerUi.caption(a,a.getString(R.string.rpg3_highest,rank(a,highest)),dark);best.setGravity(Gravity.CENTER);Ui.addSpacer(card,8);card.addView(best);
        TextView note=LedgerUi.caption(a,a.getString(R.string.rpg3_estimate),dark);note.setGravity(Gravity.CENTER);card.addView(note);
        if(state!=null&&state.optBoolean("provisional"))card.addView(LedgerUi.caption(a,a.getString(R.string.fun_provisional),dark));
        return card;
    }
    static void render(GrowthActivity a,LinearLayout parent,JSONObject journal,boolean dark,boolean catalog){
        JSONObject state=journal==null?null:journal.optJSONObject("rpg");JSONObject achievements=state==null?new JSONObject():state.optJSONObject("achievements");if(achievements==null)achievements=new JSONObject();
        if(!catalog){
            parent.addView(LedgerUi.pair(a,tier(a,state==null?null:state.optJSONObject("KR"),"KR",dark),tier(a,state==null?null:state.optJSONObject("WORLD"),"WORLD",dark)));Ui.addSpacer(parent,12);
            parent.addView(LedgerUi.pair(a,level(a,state,dark),plan(a,dark)));Ui.addSpacer(parent,12);
            parent.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_model),dark));
            tokens(a,parent,state,dark);
        }
        LedgerUi.section(parent,a.getString(R.string.rpg3_achievements),dark);
        int shown=0;for(RpgCatalog.Entry e:RpgCatalog.ALL){if(!catalog&&shown>=4)break;parent.addView(achievement(a,e,achievements.optJSONObject(e.id),dark));Ui.addSpacer(parent,8);shown++;}
        if(!catalog)parent.addView(LedgerUi.action(a,a.getString(R.string.rpg3_all),false,dark,()->a.startActivity(new Intent(a,GrowthActivity.class).putExtra("catalog",true))));
        LedgerUi.section(parent,a.getString(R.string.rpg3_titles),dark);
        String equipped=state==null?"":state.optString("equipped_title"),recommend=state==null?"":state.optString("recommended_title");
        parent.addView(LedgerUi.heading(a,equipped.isEmpty()?a.getString(R.string.rpg3_not_equipped):a.getString(R.string.rpg3_equipped,title(a,equipped)),dark));
        if(!equipped.isEmpty())parent.addView(LedgerUi.action(a,a.getString(R.string.rpg3_unequip),false,dark,()->a.equip("")));
        if(!recommend.isEmpty()){LinearLayout suggest=Ui.card(a,dark);suggest.addView(LedgerUi.heading(a,title(a,recommend),dark));suggest.addView(LedgerUi.action(a,a.getString(R.string.rpg3_choose),false,dark,()->a.equip(recommend)));parent.addView(suggest);Ui.addSpacer(parent,8);}
        Button random=LedgerUi.action(a,a.getString(R.string.rpg3_recommend),false,dark,a::recommend);random.setEnabled(achievements.length()>0&&hasEarned(achievements));parent.addView(random);
        if(catalog)for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject fact=achievements.optJSONObject(e.id);boolean earned=fact!=null&&fact.optBoolean("unlocked");Button b=LedgerUi.action(a,(earned?"✓ ":"◇ ")+title(a,e.id),false,dark,()->a.equip(e.id));b.setEnabled(earned);parent.addView(b);}
        else parent.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_no_progress),dark));
    }
    private static boolean hasEarned(JSONObject achievements){for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject a=achievements.optJSONObject(e.id);if(a!=null&&a.optBoolean("unlocked"))return true;}return false;}
    private static String title(android.content.Context c,String id){for(RpgCatalog.Entry e:RpgCatalog.ALL)if(e.id.equals(id))return korean(c)?e.title:e.titleEn;return "—";}
    private static LinearLayout level(GrowthActivity a,JSONObject state,boolean dark){
        LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_level),dark));
        long xp=state==null?0:state.optLong("xp");int level=RpgEngine.level(xp);TextView value=LedgerUi.amount(a,"Lv. "+level,dark,30);card.addView(value);
        card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_xp,number(xp)),dark));
        long base=RpgEngine.threshold(level),next=RpgEngine.threshold(level+1);ProgressBar bar=Ui.progress(a,dark);bar.setMax(1000);bar.setProgress((int)Math.min(1000,1000d*(xp-base)/(next-base)));Ui.addSpacer(card,10);card.addView(bar);
        card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_next_xp,number(next-xp),level+1),dark));
        if(state==null)card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_unknown),dark));
        String account=SubscriptionStore.accountKey(a);android.content.SharedPreferences effects=a.getSharedPreferences("codex_rpg_level_effect",0);int previous=effects.getInt(account,level);
        if(level>previous&&TierTheme.effects(a)&&android.provider.Settings.Global.getFloat(a.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1)>0){value.setAlpha(.65f);value.animate().alpha(1).setDuration(400).start();}
        effects.edit().putInt(account,level).apply();return card;
    }
    private static LinearLayout plan(GrowthActivity a,boolean dark){UsageSnapshot snapshot=AppPreferences.loadSnapshot(a);String plan=snapshot==null?null:snapshot.planType;
        LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_plan),dark));ImageView seal=new ImageView(a);seal.setImageResource(RpgPlanSeals.image(plan));seal.setScaleType(ImageView.ScaleType.FIT_CENTER);seal.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(seal,new LinearLayout.LayoutParams(-1,Ui.dp(a,92)));
        card.addView(LedgerUi.heading(a,PlanArtwork.label(plan),dark));card.addView(LedgerUi.caption(a,RpgPlanSeals.concept(plan),dark));return card;
    }
    private static LinearLayout achievement(GrowthActivity a,RpgCatalog.Entry e,JSONObject fact,boolean dark){boolean unlocked=fact!=null&&fact.optBoolean("unlocked");LinearLayout card=Ui.card(a,dark);
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);ImageView mark=new ImageView(a);int tier=e.metric.equals("tier")?(int)Math.min(9,e.target-1):e.rarity.equals("mythic")?9:e.rarity.equals("epic")?6:e.rarity.equals("rare")?3:0;mark.setImageResource(TierTheme.EMBLEMS[tier]);mark.setAlpha(unlocked?1:.28f);mark.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(mark,new LinearLayout.LayoutParams(Ui.dp(a,52),Ui.dp(a,52)));
        TextView name=LedgerUi.heading(a,korean(a)?e.ko:e.en,dark);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);
        int rarity=e.rarity.equals("mythic")?R.string.rpg3_rarity_mythic:e.rarity.equals("epic")?R.string.rpg3_rarity_epic:e.rarity.equals("rare")?R.string.rpg3_rarity_rare:e.rarity.equals("legendary")?R.string.rpg3_rarity_legendary:R.string.rpg3_rarity_common;
        card.addView(LedgerUi.caption(a,a.getString(rarity)+" · +"+e.bonus+" XP",dark));card.addView(LedgerUi.caption(a,korean(a)?e.descriptionKo():e.descriptionEn(),dark));
        long progress=fact==null?0:fact.optLong("progress");card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_progress,number(progress),number(e.target)),dark));
        if(e.days>0||e.weeks>0)card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_days_weeks,fact==null?0:fact.optInt("progress_days"),e.days,fact==null?0:fact.optInt("progress_weeks"),e.weeks),dark));
        ProgressBar bar=Ui.progress(a,dark);bar.setMax(1000);bar.setProgress(unlocked?1000:(int)(1000*Math.min(1,fact==null?0:fact.optDouble("progress_ratio",0))));bar.setContentDescription(a.getString(R.string.rpg3_progress,number(progress),number(e.target)));Ui.addSpacer(card,8);card.addView(bar);
        card.addView(LedgerUi.caption(a,unlocked?a.getString(R.string.rpg3_earned,V3Display.time(fact.optLong("first_earned_at"))):a.getString(R.string.rpg3_locked),dark));return card;
    }
    private static void tokens(GrowthActivity a,LinearLayout parent,JSONObject state,boolean dark){LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.heading(a,a.getString(R.string.rpg3_tokens),dark));JSONObject tokens=state==null?null:state.optJSONObject("tokens");boolean known=false;long total=0,at=0;
        try{known=tokens!=null&&RpgTokens.known(tokens);if(known){total=RpgTokens.milestoneTotal(tokens);at=tokens.getJSONObject("official").optLong("at");}}catch(Exception ignored){}
        card.addView(LedgerUi.amount(a,known?number(total):"—",dark,28));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_token_note),dark));
        if(at>0){card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_last_tokens,V3Display.time(at)),dark));if(System.currentTimeMillis()-at>86400000L)card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_stale),dark));}
        if(tokens!=null){try{JSONObject sessions=tokens.getJSONObject("sessions");long input=0,cached=0,output=0;for(String id:AccountProgress.keys(sessions)){JSONObject t=sessions.getJSONObject(id).getJSONObject("total");input=Math.addExact(input,t.getLong("inputTokens"));cached=Math.addExact(cached,t.getLong("cachedInputTokens"));output=Math.addExact(output,t.getLong("outputTokens"));}if(sessions.length()>0){Ui.addSpacer(card,10);card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_local_scope),dark));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_input,number(input)),dark));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_cached,number(cached)),dark));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_output,number(output)),dark));}}catch(Exception ignored){card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_unknown),dark));}}
        parent.addView(card);Ui.addSpacer(parent,12);
    }
}
