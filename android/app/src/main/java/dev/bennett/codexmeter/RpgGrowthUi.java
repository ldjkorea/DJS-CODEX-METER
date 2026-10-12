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
    static String title(android.content.Context c,String id){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);if(e==null){RpgTitleCatalog.Title t=RpgTitleCatalog.find(id);if(t!=null)e=t.display;}return e==null?"—":korean(c)?e.title:e.titleEn;}
    static String name(android.content.Context c,RpgCatalog.Entry e){return RpgDisplayCopy.name(e,korean(c));}
    static int rankAccent(int grade,boolean dark){int color=grade<0?0xffc2c9d0:TierTheme.COLORS[Math.min(9,grade)];if(!dark){float[] hsv=new float[3];android.graphics.Color.colorToHSV(color,hsv);hsv[1]=Math.max(.3f,hsv[1]);hsv[2]=.38f;color=android.graphics.Color.HSVToColor(hsv);}return color;}
    static void center(TextView text){text.setGravity(Gravity.CENTER);}
    static LinearLayout tile(GrowthActivity a,boolean dark){
        LinearLayout card=Ui.card(a,dark);card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(a,14),Ui.dp(a,14),Ui.dp(a,14),Ui.dp(a,14));
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
        ImageView crest=new ImageView(a);if(region.equals("WORLD"))crest.setImageDrawable(new WorldTierMark(grade,dark));else crest.setImageResource(TierTheme.EMBLEMS[Math.max(0,grade)]);crest.setScaleType(ImageView.ScaleType.FIT_CENTER);crest.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);if(grade<0)crest.setAlpha(.35f);
        card.addView(crest,new LinearLayout.LayoutParams(-1,Ui.dp(a,96)));
        TextView name=LedgerUi.amount(a,rank(a,grade),dark,18);name.setTextColor(rankAccent(grade,dark));center(name);card.addView(name,new LinearLayout.LayoutParams(-1,-2));
        return card;
    }
    // Equal measured heights within each row; narrow screens / enlarged text remain natural-height stacks.
    static boolean stacked(android.content.Context c){return c.getResources().getConfiguration().screenWidthDp<340||c.getResources().getConfiguration().fontScale>1.25f;}
    static LinearLayout pair(GrowthActivity a,View first,View second){
        LinearLayout row=new LinearLayout(a){@Override protected void onMeasure(int w,int h){super.onMeasure(w,h);if(getOrientation()==HORIZONTAL){int height=Math.max(first.getMeasuredHeight(),second.getMeasuredHeight());if(first.getLayoutParams().height!=height||second.getLayoutParams().height!=height){first.getLayoutParams().height=height;second.getLayoutParams().height=height;super.onMeasure(w,h);}}}};
        row.setOrientation(stacked(a)?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        row.addView(first,new LinearLayout.LayoutParams(stacked(a)?-1:0,-2,stacked(a)?0:1));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(stacked(a)?-1:0,-2,stacked(a)?0:1);
        if(stacked(a))params.topMargin=Ui.dp(a,10);else params.setMarginStart(Ui.dp(a,10));row.addView(second,params);return row;
    }
    static void render(GrowthActivity a,LinearLayout parent,JSONObject journal,boolean dark,boolean catalog){render(a,parent,journal,dark,catalog?"achievements":"overview");}
    static void render(GrowthActivity a,LinearLayout parent,JSONObject journal,boolean dark,String mode){
        JSONObject state=journal==null?null:journal.optJSONObject("rpg");JSONObject facts=state==null?null:state.optJSONObject("achievements");if(facts==null)facts=new JSONObject();
        if(!mode.equals("overview")){parent.addView(LedgerUi.action(a,a.getString(R.string.growth_overview),false,dark,()->a.open("overview")));}
        if(mode.equals("titles")){parent.addView(LedgerUi.action(a,a.getString(R.string.medal_titles),false,dark,()->a.open("titles")));return;}
        if(mode.equals("achievements")){parent.addView(LedgerUi.action(a,a.getString(R.string.medal_codex),false,dark,()->a.open("achievements")));return;}
        String equipped=state==null?"":state.optString("equipped_title");
        LinearLayout kr=tier(a,state==null?null:state.optJSONObject("KR"),"KR",dark),world=tier(a,state==null?null:state.optJSONObject("WORLD"),"WORLD",dark);
        tap(a,kr,"KR",a.getString(R.string.rpg3_korea),()->a.detail("KR",state),dark);tap(a,world,"WORLD",a.getString(R.string.rpg3_world),()->a.detail("WORLD",state),dark);
        parent.addView(pair(a,kr,world));Ui.addSpacer(parent,12);
        parent.addView(pair(a,level(a,state,dark),plan(a,state,dark)));Ui.addSpacer(parent,12);
        a.collections(journal);
        JSONObject aj=AchievementJourney.journey(state);boolean started=aj!=null&&aj.optLong("first_started_at")>0;
        if(started){
        LedgerUi.section(parent,a.getString(R.string.growth_showcase),dark);
        List<String> selected=RpgShowcase.selected(a,state);
        if(selected.isEmpty())parent.addView(LedgerUi.caption(a,a.getString(R.string.growth_showcase_empty),dark));
        LinearLayout medals=new LinearLayout(a);medals.setOrientation(LinearLayout.HORIZONTAL);
        for(String id:selected){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);LinearLayout badge=RpgDisplayStyle.medal(a,e,dark);badge.setTag("growth:representative");badge.setOnClickListener(v->a.medalDetail(state,e));RpgDisplayStyle.addMedal(medals,badge);}parent.addView(medals);Ui.addSpacer(parent,12);
        parent.addView(LedgerUi.action(a,a.getString(R.string.medal_codex),false,dark,()->a.open("achievements")));

        }
        JSONObject tj=TitleJourney.journey(state);if(tj!=null&&(tj.optLong("first_started_at")>0||!equipped.isEmpty())){
        LedgerUi.section(parent,a.getString(R.string.rpg3_titles),dark);
        Button titleButton=LedgerUi.action(a,equipped.isEmpty()?a.getString(R.string.growth_title_pick):title(a,equipped),false,dark,()->a.open("titles"));RpgDisplayStyle.title(titleButton,dark,20);parent.addView(titleButton);
        }
        parent.addView(LedgerUi.action(a,a.getString(R.string.rpg3_tokens),false,dark,()->a.detail("tokens",state)));
    }
    static LinearLayout compact(android.content.Context c,RpgCatalog.Entry e,boolean dark){
        LinearLayout row=Ui.card(c,dark);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(Ui.dp(c,56));
        ImageView mark=new ImageView(c);mark.setImageDrawable(new RpgAchievementMark(e,dark));mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(mark,new LinearLayout.LayoutParams(Ui.dp(c,42),Ui.dp(c,42)));
        TextView label=LedgerUi.caption(c,name(c,e),dark);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginStart(Ui.dp(c,10));row.addView(label,lp);return row;
    }
    private static LinearLayout level(GrowthActivity a,JSONObject state,boolean dark){
        LinearLayout card=tile(a,dark);TierTheme.frame(card,TierTheme.tier(a),dark);
        TextView label=LedgerUi.caption(a,a.getString(R.string.rpg3_level),dark);center(label);card.addView(label);Ui.addSpacer(card,10);
        long xp=state==null?0:state.optLong("xp");int level=RpgEngine.level(xp);TextView value=LedgerUi.amount(a,state==null?"Lv. —":"Lv. "+level,dark,38);center(value);card.addView(value);Ui.addSpacer(card,10);
        TextView total=LedgerUi.caption(a,a.getString(R.string.rpg3_xp,number(xp)),dark);center(total);card.addView(total);
        tap(a,card,"level",a.getString(R.string.rpg3_level),()->a.detail("level",state),dark);return card;
    }
    private static LinearLayout plan(GrowthActivity a,JSONObject state,boolean dark){
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(a);String plan=snapshot==null?null:snapshot.planType;
        LinearLayout card=tile(a,dark);android.graphics.drawable.GradientDrawable frame=LedgerUi.shape(a,LedgerUi.tint(dark),22);frame.setStroke(Ui.dp(a,1),dark?0xff687078:0xff59616b);card.setBackground(frame);TextView label=LedgerUi.caption(a,a.getString(R.string.rpg3_plan),dark);center(label);card.addView(label);Ui.addSpacer(card,8);
        ImageView seal=new ImageView(a);seal.setImageResource(RpgPlanSeals.image(plan));seal.setScaleType(ImageView.ScaleType.FIT_CENTER);seal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(seal,new LinearLayout.LayoutParams(-1,Ui.dp(a,96)));
        TextView name=LedgerUi.heading(a,PlanArtwork.label(plan),dark);center(name);card.addView(name,new LinearLayout.LayoutParams(-1,-2));
        tap(a,card,"plan",a.getString(R.string.rpg3_plan),()->a.detail("plan",state),dark);return card;
    }
    static void tokens(GrowthActivity a,LinearLayout parent,JSONObject state,boolean dark){LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.heading(a,a.getString(R.string.rpg3_tokens),dark));JSONObject tokens=state==null?null:state.optJSONObject("tokens");boolean known=false;long total=0,at=0;
        try{known=tokens!=null&&RpgTokens.known(tokens);if(known){total=RpgTokens.milestoneTotal(tokens);at=tokens.getJSONObject("official").optLong("at");}}catch(Exception ignored){}
        card.addView(LedgerUi.amount(a,known?number(total):"—",dark,28));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_token_note),dark));
        if(at>0){card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_last_tokens,V3Display.time(at)),dark));if(System.currentTimeMillis()-at>86400000L)card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_stale),dark));}
        if(tokens!=null){try{JSONObject sessions=tokens.getJSONObject("sessions");long input=0,cached=0,output=0;for(String id:AccountProgress.keys(sessions)){JSONObject t=sessions.getJSONObject(id).getJSONObject("total");input=Math.addExact(input,t.getLong("inputTokens"));cached=Math.addExact(cached,t.getLong("cachedInputTokens"));output=Math.addExact(output,t.getLong("outputTokens"));}if(sessions.length()>0){Ui.addSpacer(card,10);card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_local_scope),dark));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_input,number(input)),dark));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_cached,number(cached)),dark));card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_output,number(output)),dark));}}catch(Exception ignored){card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_unknown),dark));}}
        parent.addView(card);Ui.addSpacer(parent,12);
    }
}
