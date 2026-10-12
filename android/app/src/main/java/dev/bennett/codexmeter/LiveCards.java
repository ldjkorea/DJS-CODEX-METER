package dev.bennett.codexmeter;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/** Immediate utilization presentation; historical settlements stay in their existing journal. */
final class LiveCards {
    private LiveCards() {}
    static LinearLayout crest(AppCompatActivity a,boolean dark,LiveUtilization.Result live,boolean fresh){
        int rank=TierTheme.tier(a);
        LinearLayout card=Ui.card(a,dark);TierTheme.frame(card,rank,dark);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(a,16),Ui.dp(a,10),Ui.dp(a,16),Ui.dp(a,10));
        TierCompanionView badge=new TierCompanionView(a,rank);
        int size=Ui.dp(a,132);
        card.addView(TierTheme.companion(badge,rank),new LinearLayout.LayoutParams(size,size));
        TextView title=LedgerUi.amount(a,rank<0?a.getString(R.string.fun_placing):TierPresentation.ENGLISH[rank],dark,24);
        title.setGravity(Gravity.CENTER);title.setLetterSpacing(.06f);
        card.addView(title,new LinearLayout.LayoutParams(-1,-2));
        if(rank<0){TextView note=LedgerUi.caption(a,a.getString(fresh?R.string.live_need_coverage:R.string.ui_ledger_refresh),dark);note.setGravity(Gravity.CENTER);card.addView(note);}
        org.json.JSONObject growth=RpgProgressStore.progress(a);
        String equipped=growth==null?"":growth.optString("equipped_title");
        if(!equipped.isEmpty()){TextView alias=LedgerUi.badge(a,RpgGrowthUi.title(a,equipped),dark,false);RpgDisplayStyle.title(alias,dark,19);alias.setMinHeight(Ui.dp(a,48));alias.setOnClickListener(v->a.startActivity(new android.content.Intent(a,GrowthActivity.class).putExtra("growth_mode","titles")));card.addView(alias);}
        java.util.List<String> representatives=RpgShowcase.selected(a,growth);
        if(!representatives.isEmpty()){
            LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER);Ui.addSpacer(card,14);
            for(String id:representatives){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);LinearLayout medal=RpgDisplayStyle.medal(a,e,dark);medal.setOnClickListener(v->RpgMedalUi.show(a,growth,e,dark,false,null));RpgDisplayStyle.addMedal(row,medal);}card.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        return card;
    }
    static LinearLayout ai(AppCompatActivity a,boolean dark,LiveUtilization.Result live,UsageSnapshot snapshot,boolean fresh,double daily,long now){
        return ai(a,dark,live,snapshot,fresh,daily,now,null);
    }
    static LinearLayout ai(AppCompatActivity a,boolean dark,LiveUtilization.Result live,UsageSnapshot snapshot,boolean fresh,double daily,long now,String selected){
        UsageWindow window=snapshot.longWindow();
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(Ui.dp(a,10),Ui.dp(a,14),Ui.dp(a,10),Ui.dp(a,14));
        card.addView(LedgerUi.caption(a,a.getString(R.string.live_ai_title),dark));Ui.addSpacer(card,10);
        String key=SpicyAi.key(daily,window==null?Double.NaN:window.usedPercent,fresh,live.bonus,window==null?0:window.effectiveResetAtMillis(snapshot.fetchedAtMillis)-now,now);
        int id=resource(key);
        TextView quote=LedgerUi.heading(a,"“"+(selected==null?a.getString(id==0?R.string.live_ai_missing_0:id):selected)+"”",dark);quote.setTextSize(18);card.addView(quote);return card;
    }
    private static int resource(String key){
        switch(key){
            case "live_ai_missing_0":return R.string.live_ai_missing_0;
            case "live_ai_missing_1":return R.string.live_ai_missing_1;
            case "live_ai_missing_2":return R.string.live_ai_missing_2;
            case "live_ai_bonus_0":return R.string.live_ai_bonus_0;
            case "live_ai_bonus_1":return R.string.live_ai_bonus_1;
            case "live_ai_bonus_2":return R.string.live_ai_bonus_2;
            case "live_ai_empty_0":return R.string.live_ai_empty_0;
            case "live_ai_empty_1":return R.string.live_ai_empty_1;
            case "live_ai_empty_2":return R.string.live_ai_empty_2;
            case "live_ai_sprint_0":return R.string.live_ai_sprint_0;
            case "live_ai_sprint_1":return R.string.live_ai_sprint_1;
            case "live_ai_sprint_2":return R.string.live_ai_sprint_2;
            case "live_ai_active_0":return R.string.live_ai_active_0;
            case "live_ai_active_1":return R.string.live_ai_active_1;
            case "live_ai_active_2":return R.string.live_ai_active_2;
            case "live_ai_idle_0":return R.string.live_ai_idle_0;
            case "live_ai_idle_1":return R.string.live_ai_idle_1;
            case "live_ai_idle_2":return R.string.live_ai_idle_2;
            case "live_ai_zero_0":return R.string.live_ai_zero_0;
            case "live_ai_zero_1":return R.string.live_ai_zero_1;
            case "live_ai_zero_2":return R.string.live_ai_zero_2;
            case "live_ai_partial_0":return R.string.live_ai_partial_0;
            case "live_ai_partial_1":return R.string.live_ai_partial_1;
            case "live_ai_partial_2":return R.string.live_ai_partial_2;
            case "live_ai_steady_0":return R.string.live_ai_steady_0;
            case "live_ai_steady_1":return R.string.live_ai_steady_1;
            case "live_ai_steady_2":return R.string.live_ai_steady_2;
            default:return R.string.live_ai_missing_0;
        }
    }
    static double daily(UsageLedgerDatabase.Data data,String policy,long now){
        LedgerAggregation.Day day=LedgerPresentation.find(data.days,policy,LedgerAggregation.day(now));
        return day!=null&&(day.coveredMillis>0||day.points>0)?day.points:Double.NaN;
    }
}
