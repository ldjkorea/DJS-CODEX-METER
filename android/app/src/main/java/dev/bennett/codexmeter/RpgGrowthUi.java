package dev.bennett.codexmeter;

import android.content.*;
import android.view.*;
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
    static String title(android.content.Context c,String id){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);return e==null?"—":korean(c)?e.title:e.titleEn;}
    static String name(android.content.Context c,RpgCatalog.Entry e){return korean(c)?e.ko:e.en;}
    static int rankAccent(int grade,boolean dark){int color=grade<0?0xffc2c9d0:TierTheme.COLORS[Math.min(9,grade)];if(!dark){float[] hsv=new float[3];android.graphics.Color.colorToHSV(color,hsv);hsv[1]=Math.max(.3f,hsv[1]);hsv[2]=.38f;color=android.graphics.Color.HSVToColor(hsv);}return color;}
    static void center(TextView text){text.setGravity(Gravity.CENTER);}
    static LinearLayout tile(GrowthActivity a,boolean dark){
        LinearLayout card=Ui.card(a,dark);card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(a,14),Ui.dp(a,22),Ui.dp(a,14),Ui.dp(a,18));
        if(!LedgerUi.stacked(a))card.setMinimumHeight(Ui.dp(a,300));
        return card;
    }
    static void tap(GrowthActivity a,LinearLayout card,String tag,String label,Runnable action,boolean dark){
        card.setTag("growth:"+tag);card.setFocusable(true);card.setClickable(true);
        android.util.TypedValue ripple=new android.util.TypedValue();a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true);
        if(ripple.resourceId!=0)card.setForeground(androidx.core.content.ContextCompat.getDrawable(a,ripple.resourceId));
        card.setContentDescription(label+" · "+a.getString(R.string.growth_tap));card.setOnClickListener(v->action.run());
        Ui.addSpacer(card,10);TextView hint=LedgerUi.caption(a,a.getString(R.string.growth_tap),dark);center(hint);card.addView(hint);
    }
    static LinearLayout tier(GrowthActivity a,JSONObject state,String region,boolean dark){
        int grade=state==null?-1:state.optInt("tier",-1),highest=state==null?-1:state.optInt("highest",-1);
        LinearLayout card=tile(a,dark);TierTheme.frame(card,grade,dark);
        TextView label=LedgerUi.caption(a,a.getString(region.equals("KR")?R.string.rpg3_korea:R.string.rpg3_world),dark);center(label);card.addView(label);Ui.addSpacer(card,8);
        ImageView crest=new ImageView(a);crest.setImageResource(TierTheme.EMBLEMS[Math.max(0,grade)]);crest.setScaleType(ImageView.ScaleType.FIT_CENTER);crest.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);if(grade<0)crest.setAlpha(.35f);
        card.addView(crest,new LinearLayout.LayoutParams(-1,Ui.dp(a,124)));
        TextView name=LedgerUi.amount(a,rank(a,grade),dark,18);name.setTextColor(rankAccent(grade,dark));center(name);card.addView(name,new LinearLayout.LayoutParams(-1,-2));
        TextView best=LedgerUi.caption(a,a.getString(R.string.rpg3_highest,rank(a,highest)),dark);center(best);Ui.addSpacer(card,8);card.addView(best);
        TextView note=LedgerUi.caption(a,a.getString(state!=null&&state.optBoolean("provisional")?R.string.fun_provisional:R.string.rpg3_estimate),dark);center(note);card.addView(note);
        return card;
    }
    // Equal measured heights within each row; narrow screens / enlarged text remain natural-height stacks.
    static LinearLayout pair(GrowthActivity a,View first,View second){
        LinearLayout row=new LinearLayout(a){@Override protected void onMeasure(int w,int h){super.onMeasure(w,h);if(getOrientation()==HORIZONTAL){int height=Math.max(first.getMeasuredHeight(),second.getMeasuredHeight());if(first.getLayoutParams().height!=height||second.getLayoutParams().height!=height){first.getLayoutParams().height=height;second.getLayoutParams().height=height;super.onMeasure(w,h);}}}};
        row.setOrientation(LedgerUi.stacked(a)?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        row.addView(first,new LinearLayout.LayoutParams(LedgerUi.stacked(a)?-1:0,-2,LedgerUi.stacked(a)?0:1));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(LedgerUi.stacked(a)?-1:0,-2,LedgerUi.stacked(a)?0:1);
        if(LedgerUi.stacked(a))params.topMargin=Ui.dp(a,10);else params.setMarginStart(Ui.dp(a,10));row.addView(second,params);return row;
    }
    static void render(GrowthActivity a,LinearLayout parent,JSONObject journal,boolean dark,boolean catalog){render(a,parent,journal,dark,catalog?"achievements":"overview");}
    static void render(GrowthActivity a,LinearLayout parent,JSONObject journal,boolean dark,String mode){
        JSONObject state=journal==null?null:journal.optJSONObject("rpg");JSONObject facts=state==null?null:state.optJSONObject("achievements");if(facts==null)facts=new JSONObject();
        if(!mode.equals("overview")){parent.addView(LedgerUi.action(a,a.getString(R.string.growth_overview),false,dark,()->a.open("overview")));}
        if(mode.equals("titles")){titles(a,parent,state,facts,dark);return;}
        if(mode.equals("achievements")){
            LedgerUi.section(parent,a.getString(R.string.rpg3_achievements),dark);
            parent.addView(LedgerUi.caption(a,a.getString(R.string.growth_earned_count,RpgGrowthFacts.earned(state).size(),RpgCatalog.ALL.size()),dark));
            parent.addView(LedgerUi.action(a,a.getString(R.string.growth_showcase_edit),false,dark,()->a.chooseShowcase(state)));
            for(RpgCatalog.Entry e:RpgCatalog.ALL){parent.addView(achievement(a,e,facts.optJSONObject(e.id),dark));Ui.addSpacer(parent,8);}return;
        }
        String equipped=state==null?"":state.optString("equipped_title");
        TextView header=LedgerUi.badge(a,equipped.isEmpty()?a.getString(R.string.rpg3_not_equipped):title(a,equipped),dark,false);
        header.setGravity(Gravity.END);header.setMinHeight(Ui.dp(a,48));header.setOnClickListener(v->a.open("titles"));header.setContentDescription(a.getString(R.string.growth_title_pick)+" · "+header.getText());
        LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.END);top.addView(header);parent.addView(top);Ui.addSpacer(parent,12);
        LinearLayout kr=tier(a,state==null?null:state.optJSONObject("KR"),"KR",dark),world=tier(a,state==null?null:state.optJSONObject("WORLD"),"WORLD",dark);
        tap(a,kr,"KR",a.getString(R.string.rpg3_korea),()->a.detail("KR",state),dark);tap(a,world,"WORLD",a.getString(R.string.rpg3_world),()->a.detail("WORLD",state),dark);
        parent.addView(pair(a,kr,world));Ui.addSpacer(parent,12);
        parent.addView(pair(a,level(a,state,dark),plan(a,state,dark)));Ui.addSpacer(parent,12);
        parent.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_model),dark));
        LedgerUi.section(parent,a.getString(R.string.growth_showcase),dark);
        List<String> selected=RpgShowcase.selected(a,state);
        if(selected.isEmpty())parent.addView(LedgerUi.caption(a,a.getString(R.string.growth_showcase_empty),dark));
        for(String id:selected){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);LinearLayout badge=compact(a,e,dark);badge.setTag("growth:representative");badge.setContentDescription(a.getString(R.string.growth_representative)+" · "+name(a,e));badge.setOnClickListener(v->a.open("achievements"));badge.setFocusable(true);parent.addView(badge);Ui.addSpacer(parent,8);}
        parent.addView(LedgerUi.action(a,a.getString(R.string.rpg3_all),false,dark,()->a.open("achievements")));
        parent.addView(LedgerUi.action(a,a.getString(R.string.growth_showcase_edit),false,dark,()->a.chooseShowcase(state)));
        LedgerUi.section(parent,a.getString(R.string.rpg3_titles),dark);
        parent.addView(LedgerUi.action(a,equipped.isEmpty()?a.getString(R.string.growth_title_pick):title(a,equipped),false,dark,()->a.open("titles")));
        tokens(a,parent,state,dark);
    }
    static LinearLayout compact(android.content.Context c,RpgCatalog.Entry e,boolean dark){
        LinearLayout row=Ui.card(c,dark);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(Ui.dp(c,56));
        ImageView mark=new ImageView(c);mark.setImageDrawable(new RpgAchievementMark(e,dark));mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(mark,new LinearLayout.LayoutParams(Ui.dp(c,42),Ui.dp(c,42)));
        TextView label=LedgerUi.caption(c,name(c,e),dark);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginStart(Ui.dp(c,10));row.addView(label,lp);return row;
    }
    private static void titles(GrowthActivity a,LinearLayout parent,JSONObject state,JSONObject facts,boolean dark){
        LedgerUi.section(parent,a.getString(R.string.growth_title_pick),dark);parent.addView(LedgerUi.caption(a,a.getString(R.string.growth_title_note),dark));
        String equipped=state==null?"":state.optString("equipped_title"),recommend=state==null?"":state.optString("recommended_title");
        parent.addView(LedgerUi.heading(a,equipped.isEmpty()?a.getString(R.string.rpg3_not_equipped):a.getString(R.string.rpg3_equipped,title(a,equipped)),dark));
        if(!equipped.isEmpty())parent.addView(LedgerUi.action(a,a.getString(R.string.rpg3_unequip),false,dark,()->a.equip("")));
        if(!recommend.isEmpty()&&RpgGrowthFacts.earned(state).contains(recommend)){parent.addView(LedgerUi.action(a,a.getString(R.string.rpg3_recommend)+" · "+title(a,recommend),false,dark,()->a.equip(recommend)));}
        Button random=LedgerUi.action(a,a.getString(R.string.rpg3_recommend),false,dark,a::recommend);random.setEnabled(!RpgGrowthFacts.earned(state).isEmpty());parent.addView(random);
        if(RpgGrowthFacts.earned(state).isEmpty())parent.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_no_achievements),dark));
        for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject fact=facts.optJSONObject(e.id);boolean earned=fact!=null&&fact.optBoolean("unlocked");
            LinearLayout row=Ui.card(a,dark);row.addView(LedgerUi.heading(a,title(a,e.id),dark));row.addView(LedgerUi.caption(a,name(a,e)+" · "+(korean(a)?e.descriptionKo():e.descriptionEn()),dark));
            Button button=LedgerUi.action(a,earned?(e.id.equals(equipped)?a.getString(R.string.rpg3_equipped,title(a,e.id)):a.getString(R.string.rpg3_choose)):a.getString(R.string.rpg3_locked),e.id.equals(equipped),dark,()->a.equip(e.id));button.setEnabled(earned&&!e.id.equals(equipped));button.setTag("growth:title:"+e.id);row.addView(button);parent.addView(row);Ui.addSpacer(parent,8);
        }
    }
    private static LinearLayout level(GrowthActivity a,JSONObject state,boolean dark){
        LinearLayout card=tile(a,dark);TierTheme.frame(card,TierTheme.tier(a),dark);
        TextView label=LedgerUi.caption(a,a.getString(R.string.rpg3_level),dark);center(label);card.addView(label);Ui.addSpacer(card,18);
        long xp=state==null?0:state.optLong("xp");int level=RpgEngine.level(xp);TextView value=LedgerUi.amount(a,state==null?"Lv. —":"Lv. "+level,dark,38);center(value);card.addView(value);Ui.addSpacer(card,18);
        TextView total=LedgerUi.caption(a,a.getString(R.string.rpg3_xp,number(xp)),dark);center(total);card.addView(total);
        long base=RpgEngine.threshold(level),next=RpgEngine.threshold(level+1);ProgressBar bar=Ui.progress(a,dark);bar.setMax(1000);bar.setProgress((int)Math.min(1000,1000d*(xp-base)/(next-base)));bar.setContentDescription(a.getString(R.string.rpg3_next_xp,number(next-xp),level+1));Ui.addSpacer(card,12);card.addView(bar);
        TextView remaining=LedgerUi.caption(a,state==null?a.getString(R.string.rpg3_unknown):a.getString(R.string.rpg3_next_xp,number(next-xp),level+1),dark);center(remaining);card.addView(remaining);
        tap(a,card,"level",a.getString(R.string.rpg3_level),()->a.detail("level",state),dark);return card;
    }
    private static LinearLayout plan(GrowthActivity a,JSONObject state,boolean dark){
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(a);String plan=snapshot==null?null:snapshot.planType;
        LinearLayout card=tile(a,dark);android.graphics.drawable.GradientDrawable frame=LedgerUi.shape(a,LedgerUi.tint(dark),22);frame.setStroke(Ui.dp(a,1),dark?0xff687078:0xff59616b);card.setBackground(frame);TextView label=LedgerUi.caption(a,a.getString(R.string.rpg3_plan),dark);center(label);card.addView(label);Ui.addSpacer(card,8);
        ImageView seal=new ImageView(a);seal.setImageResource(RpgPlanSeals.image(plan));seal.setScaleType(ImageView.ScaleType.FIT_CENTER);seal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(seal,new LinearLayout.LayoutParams(-1,Ui.dp(a,124)));
        TextView name=LedgerUi.heading(a,PlanArtwork.label(plan),dark);center(name);card.addView(name,new LinearLayout.LayoutParams(-1,-2));TextView concept=LedgerUi.caption(a,RpgPlanSeals.concept(plan),dark);center(concept);Ui.addSpacer(card,8);card.addView(concept);
        tap(a,card,"plan",a.getString(R.string.rpg3_plan),()->a.detail("plan",state),dark);return card;
    }
    private static LinearLayout achievement(GrowthActivity a,RpgCatalog.Entry e,JSONObject fact,boolean dark){
        boolean unlocked=fact!=null&&fact.optBoolean("unlocked");LinearLayout card=Ui.card(a,dark);
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);ImageView mark=new ImageView(a);mark.setImageDrawable(new RpgAchievementMark(e,dark));mark.setAlpha(unlocked?1:.35f);mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(mark,new LinearLayout.LayoutParams(Ui.dp(a,52),Ui.dp(a,52)));
        TextView name=LedgerUi.heading(a,name(a,e),dark);LinearLayout.LayoutParams title=new LinearLayout.LayoutParams(0,-2,1);title.setMarginStart(Ui.dp(a,10));row.addView(name,title);card.addView(row);
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
