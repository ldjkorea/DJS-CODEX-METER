package dev.bennett.codexmeter;

import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigDecimal;
import org.json.JSONObject;

/** Current window allocation, with short UI and detailed explanation on demand. */
final class SubscriptionValueUi {
    private SubscriptionValueUi() { }
    static LinearLayout card(AppCompatActivity a,boolean dark,UsageSnapshot snapshot,BigDecimal percent,LedgerPeriods.Span span,boolean fresh,boolean safe) {
        SubscriptionCost bill=SubscriptionStore.load(a,snapshot);
        SubscriptionValue.Result result=SubscriptionValue.calculate(bill,span,percent,fresh,safe);
        LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.heading(a,a.getString(R.string.ux_my_value),dark));Ui.addSpacer(card,12);
        if(result.state==SubscriptionValue.State.READY){
            LinearLayout figures=new LinearLayout(a);figures.setOrientation(LinearLayout.VERTICAL);
            figures.addView(LedgerUi.amount(a,FunActivity.money(bill.currency,result.used),dark,32));
            figures.addView(LedgerUi.caption(a,"/ "+FunActivity.money(bill.currency,result.allocated),dark));card.addView(figures);
            card.addView(LedgerUi.caption(a,a.getString(R.string.ux_value_labels),dark));
            card.addView(LedgerUi.caption(a,a.getString(snapshot.weekly!=null?R.string.ux_week_basis:R.string.ux_month_basis),dark));
            android.widget.ProgressBar progress=Ui.progress(a,dark);progress.setProgress(percent.intValue());progress.setContentDescription(a.getString(R.string.ux_value_percent,percent.stripTrailingZeros().toPlainString()));Ui.addSpacer(card,12);card.addView(progress);
            card.addView(LedgerUi.caption(a,a.getString(R.string.ux_value_percent,percent.stripTrailingZeros().toPlainString()),dark));
            card.addView(LedgerUi.caption(a,a.getString(R.string.ux_reference),dark));
        }else if(result.state!=SubscriptionValue.State.NO_PAYMENT){
            int message=result.state==SubscriptionValue.State.NEED_REFRESH?R.string.ui_ledger_refresh:result.state==SubscriptionValue.State.PERIOD_MISMATCH?R.string.ux_period_mismatch:R.string.ux_value_pending;
            card.addView(LedgerUi.caption(a,a.getString(message),dark));
        }
        Ui.addSpacer(card,12);
        if(bill==null||result.state==SubscriptionValue.State.PERIOD_MISMATCH){
            android.widget.Button edit=LedgerUi.action(a,a.getString(bill==null?R.string.ux_register:R.string.v3_payment_edit),true,dark,()->SubscriptionUi.edit(a,AppPreferences.loadSnapshot(a),dark,a::recreate));
            edit.setEnabled(SubscriptionStore.key(a,snapshot)!=null);card.addView(edit);
        }
        return card;
    }
    static LinearLayout card(AppCompatActivity a,boolean dark,UsageSnapshot snapshot,LiveUtilization.Result live,boolean fresh){
        SubscriptionCost bill=SubscriptionStore.load(a,snapshot);
        LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.heading(a,a.getString(R.string.ux_my_value),dark));Ui.addSpacer(card,12);
        boolean active=bill!=null&&bill.active(LedgerAggregation.day(System.currentTimeMillis()));
        JSONObject monthly=RpgProgressStore.monthly(a,snapshot);JSONObject value=null;
        try{UsageWindow w=snapshot.longWindow();String policy=(snapshot.weekly!=null?"weekly":"monthly")+"|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+w.windowSeconds;
            value=monthly==null?null:monthly.getJSONObject("buckets").optJSONObject(policy);
        }catch(Exception ignored){}
        if(active&&value!=null&&!value.isNull("reference_value")){
            BigDecimal percentValue=new BigDecimal(value.optString("percent","0"));
            LinearLayout figures=new LinearLayout(a);figures.setOrientation(LinearLayout.VERTICAL);
            figures.addView(LedgerUi.amount(a,FunActivity.money(bill.currency,new BigDecimal(value.optString("reference_value","0"))),dark,32));
            figures.addView(LedgerUi.caption(a,"/ "+FunActivity.money(bill.currency,bill.amount),dark));card.addView(figures);
            card.addView(LedgerUi.caption(a,a.getString(R.string.rpg_billing_period,bill.start.toString(),bill.end.toString()),dark));
            card.addView(LedgerUi.caption(a,a.getString(R.string.live_value_ratio,LedgerUi.number(percentValue.doubleValue())),dark));
            android.widget.ProgressBar progress=Ui.progress(a,dark);progress.setProgress((int)Math.max(0,Math.min(100,percentValue.doubleValue())));Ui.addSpacer(card,12);card.addView(progress);
            card.addView(LedgerUi.caption(a,a.getString(R.string.rpg_reference),dark));
            card.addView(LedgerUi.caption(a,a.getString(fresh?R.string.rpg_partial:R.string.rpg_last_settlement),dark));
        }else card.addView(LedgerUi.caption(a,a.getString(bill==null?R.string.v3_payment_missing:!active?R.string.ux_period_mismatch:monthly!=null&&monthly.optBoolean("mixed_policies")?R.string.rpg_mixed_policy:R.string.live_need_coverage),dark));
        if(bill==null||!active){Ui.addSpacer(card,12);android.widget.Button edit=LedgerUi.action(a,a.getString(bill==null?R.string.ux_register:R.string.v3_payment_edit),true,dark,()->SubscriptionUi.edit(a,AppPreferences.loadSnapshot(a),dark,a::recreate));edit.setEnabled(SubscriptionStore.key(a,snapshot)!=null);card.addView(edit);}
        return card;
    }
}
