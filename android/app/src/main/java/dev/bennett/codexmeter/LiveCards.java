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
        return BetaHomeUi.card(a,dark);
    }
    static LinearLayout ai(AppCompatActivity a,boolean dark,LiveUtilization.Result live,UsageSnapshot snapshot,boolean fresh,double daily,long now){
        return ai(a,dark,live,snapshot,fresh,daily,now,null);
    }
    static LinearLayout ai(AppCompatActivity a,boolean dark,LiveUtilization.Result live,UsageSnapshot snapshot,boolean fresh,double daily,long now,String selected){
        return CoachUi.card(a,dark,snapshot,fresh,live.bonus,daily);
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
