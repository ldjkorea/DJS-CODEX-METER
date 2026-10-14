package dev.bennett.codexmeter;

import android.content.Context;
import android.widget.*;
import org.json.*;

/** Small display snapshot only. No migration, evidence scan or network lock during rendering. */
final class MxpUi {
    private static final java.util.concurrent.ExecutorService WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
    private MxpUi(){}
    static int level(JSONObject s){JSONObject m=MxpEconomy.state(s);return m==null?RpgEngine.level(s==null?0:s.optLong("xp")):m.optInt("level",1);}
    static String progress(JSONObject m){return LedgerUi.number(MxpEconomy.progress(m));}
    static void periods(Context c,LinearLayout parent,JSONObject state,boolean dark){
        JSONObject m=MxpEconomy.state(state);if(m==null)return;
        // Full journal callers arrive via a worker-built summary in GrowthActivity; only home has period counters.
        if(!m.has("day"))return;
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.VERTICAL);
        row.addView(LedgerUi.caption(c,c.getString(R.string.mxp_periods),dark));
        row.addView(LedgerUi.caption(c,c.getString(R.string.mxp_day,m.optInt("day"))+" · "+c.getString(R.string.mxp_week,m.optInt("week"))+"\n"+c.getString(R.string.mxp_month,m.optInt("month")),dark));parent.addView(row);
        row.addView(LedgerUi.caption(c,c.getString(R.string.mxp_observed_date,m.optString("as_of_day"))+" · "+c.getString(m.optBoolean("day_verified")?R.string.mxp_verified:R.string.mxp_pending),dark));
    }
    static void details(GrowthActivity a,LinearLayout parent,JSONObject state,boolean dark){
        JSONObject m=MxpEconomy.state(state);if(m==null)return;
        parent.addView(LedgerUi.amount(a,"Lv. "+m.optInt("level",1),dark,38));
        parent.addView(LedgerUi.caption(a,a.getString(R.string.mxp_progress,progress(m),Long.toString(m.optLong("next_cost"))),dark));
        parent.addView(LedgerUi.caption(a,a.getString(R.string.mxp_total,Long.toString(m.optLong("total")),Long.toString(state.optLong("xp"))),dark));
        ProgressBar bar=Ui.progress(a,dark);bar.setMax(1000);bar.setProgress((int)(1000*MxpEconomy.progress(m)/Math.max(1,m.optLong("next_cost"))));parent.addView(bar);
        bar.setContentDescription(a.getString(R.string.mxp_progress,progress(m),Long.toString(m.optLong("next_cost"))));
        parent.addView(LedgerUi.caption(a,m.has("season_start")?a.getString(R.string.mxp_season,m.optString("season_start")):a.getString(R.string.mxp_waiting),dark));
        try{JSONObject view=MxpEconomy.summary(m,System.currentTimeMillis());if(view.has("current_season_start"))parent.addView(LedgerUi.caption(a,a.getString(R.string.mxp_year_progress,view.getString("current_season_start"),view.getString("current_season_end"),view.optInt("year_bronze"),view.optInt("year_silver"),view.optInt("year_gold")),dark));}catch(JSONException ignored){}
        parent.addView(LedgerUi.action(a,a.getString(R.string.mxp_history),false,dark,()->history(a,m,dark,0)));
        parent.addView(LedgerUi.caption(a,a.getString(R.string.mxp_reward_note),dark));parent.addView(LedgerUi.caption(a,a.getString(R.string.mxp_help),dark));
        honors(a,parent,state,dark);
    }
    static void honors(GrowthActivity a,LinearLayout parent,JSONObject state,boolean dark){
        String account=SubscriptionStore.accountKey(a);
        parent.addView(LedgerUi.action(a,"명예 도감",false,dark,()->BetaGrowthUi.catalog(a,state,dark,false,0,account)));
        parent.addView(LedgerUi.action(a,"칭호 옷장",false,dark,()->BetaGrowthUi.catalog(a,state,dark,true,0,account)));
    }
    private static void history(GrowthActivity a,JSONObject m,boolean dark,int page){
        String account=SubscriptionStore.accountKey(a);WORKER.execute(()->{JSONObject medals=m.optJSONObject("period_honors");java.util.List<String> keys=medals==null?new java.util.ArrayList<>():AccountProgress.keys(medals);java.util.Collections.sort(keys,java.util.Collections.reverseOrder());
            java.util.List<String> rows=new java.util.ArrayList<>();String[] kinds={"day","week","month","year","codex-week","codex-month","four-weeks"};String[] labels=a.getResources().getStringArray(R.array.mxp_period_labels);
            for(int i=page*40;i<Math.min(keys.size(),(page+1)*40);i++){String id=keys.get(i);int split=id.indexOf(':');String kind=id.substring(0,split),name=kind;for(int j=0;j<kinds.length;j++)if(kinds[j].equals(kind))name=labels[j];JSONObject medal=medals.optJSONObject(id);rows.add(name+" · "+id.substring(split+1)+"\n"+a.getString(R.string.mxp_history_stage,medal.optInt("stage"))+" · "+V3Display.time(medal.optLong("first_earned_at")));}
            a.runOnUiThread(()->{if(a.isDestroyed()||a.isFinishing()||!java.util.Objects.equals(account,SubscriptionStore.accountKey(a)))return;
                LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(a,20),Ui.dp(a,12),Ui.dp(a,20),Ui.dp(a,12));for(String row:rows){body.addView(LedgerUi.caption(a,row,dark));Ui.addSpacer(body,12);}if(rows.isEmpty())body.addView(LedgerUi.caption(a,a.getString(R.string.mxp_pending),dark));
                if(page>0)body.addView(LedgerUi.action(a,a.getString(R.string.mxp_previous),false,dark,()->history(a,m,dark,page-1)));if((page+1)*40<keys.size())body.addView(LedgerUi.action(a,a.getString(R.string.mxp_next),false,dark,()->history(a,m,dark,page+1)));
                ScrollView scroll=new ScrollView(a);scroll.addView(body);if(a.detailDialog!=null)a.detailDialog.dismiss();a.detailDialog=new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(R.string.mxp_history).setView(scroll).setPositiveButton(R.string.coach_close,null).create();a.detailDialog.show();
            });
        });
    }
}
