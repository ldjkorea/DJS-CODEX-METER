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
        JSONObject monthly=RpgProgressStore.monthly(a,snapshot);
        if(!BillingEvidence.matches(monthly,bill,snapshot.planType))monthly=null;
        JSONObject value=null;
        LinearLayout details=new LinearLayout(a);details.setOrientation(LinearLayout.VERTICAL);details.setVisibility(android.view.View.GONE);
        try{UsageWindow w=snapshot.longWindow();String policy=(snapshot.weekly!=null?"weekly":"monthly")+"|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+w.windowSeconds;
            value=monthly==null?null:monthly.getJSONObject("buckets").optJSONObject(policy);
        }catch(Exception ignored){}
        if(active&&monthly!=null&&monthly.optBoolean("mixed_policies")){
            card.addView(LedgerUi.caption(a,a.getString(R.string.ref_mixed_billing),dark));
            JSONObject document=RpgProgressStore.peek(a);JSONObject recaps=document==null?null:document.optJSONObject("settlements");if(recaps!=null)for(String id:AccountProgress.keys(recaps)){JSONObject recap=recaps.optJSONObject(id),b=recap.optJSONObject("bill");if(b!=null&&b.optString("start").equals(bill.start.toString())&&b.optString("end").equals(bill.end.toString())){details.addView(LedgerUi.caption(a,b.optString("plan")+" · "+b.optString("currency")+" · "+b.optString("amount"),dark));JSONObject buckets=recap.optJSONObject("buckets");for(String p:AccountProgress.keys(buckets)){JSONObject v=buckets.optJSONObject(p);details.addView(LedgerUi.caption(a,p+" · "+v.optString("percent")+"%",dark));}}}
        }else if(active&&value!=null&&!value.isNull("reference_value")){
            BigDecimal percentValue=new BigDecimal(value.optString("percent","0"));
            LinearLayout figures=new LinearLayout(a);figures.setOrientation(LinearLayout.VERTICAL);
            figures.addView(LedgerUi.amount(a,FunActivity.money(bill.currency,new BigDecimal(value.optString("reference_value","0"))),dark,32));
            figures.addView(LedgerUi.caption(a,a.getString(R.string.phase_a_registered_payment,FunActivity.money(bill.currency,bill.amount)),dark));card.addView(figures);
            card.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_billing_usage,LedgerUi.number(percentValue.doubleValue())),dark));
            android.widget.ProgressBar progress=Ui.progress(a,dark);progress.setProgress((int)Math.max(0,Math.min(100,percentValue.doubleValue())));Ui.addSpacer(card,12);card.addView(progress);
            card.addView(LedgerUi.caption(a,a.getString(fresh?R.string.rpg_partial:R.string.rpg_last_settlement),dark));
        }else card.addView(LedgerUi.caption(a,a.getString(bill==null?R.string.v3_payment_missing:!active?R.string.ux_period_mismatch:monthly!=null&&monthly.optBoolean("mixed_policies")?R.string.rpg_mixed_policy:monthly==null?R.string.phase_a_bill_pending:R.string.live_need_coverage),dark));
        if(bill==null||!active){Ui.addSpacer(card,12);android.widget.Button edit=LedgerUi.action(a,a.getString(bill==null?R.string.ux_register:R.string.v3_payment_edit),true,dark,()->SubscriptionUi.edit(a,AppPreferences.loadSnapshot(a),dark,a::recreate));edit.setEnabled(SubscriptionStore.key(a,snapshot)!=null);card.addView(edit);}
        details.addView(LedgerUi.caption(a,a.getString(R.string.phase_a_value_note),dark));
        details.addView(LedgerUi.caption(a,a.getString(R.string.phase_a_reference_price),dark));
        if(bill!=null){
            JSONObject all=RpgProgressStore.peek(a),recaps=all==null?null:all.optJSONObject("settlements");int matching=0;if(recaps!=null)for(String id:AccountProgress.keys(recaps)){JSONObject b=recaps.optJSONObject(id).optJSONObject("bill");if(b!=null&&b.optString("start").equals(bill.start.toString())&&b.optString("end").equals(bill.end.toString()))matching++;}if(matching>1)details.addView(LedgerUi.caption(a,a.getString(R.string.ref_mixed_billing),dark));
            details.addView(LedgerUi.caption(a,a.getString(R.string.rpg_billing_period,bill.start.toString(),bill.end.toString()),dark));
            details.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_payment,FunActivity.money(bill.currency,bill.amount)),dark));
            long start=bill.start.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),end=bill.end.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();double elapsed=100d*(System.currentTimeMillis()-start)/(end-start);
            details.addView(LedgerUi.caption(a,a.getString(R.string.rpg3_elapsed,LedgerUi.number(Math.max(0,Math.min(100,elapsed)))),dark));
        }
        JSONObject display=RpgProgressStore.peek(a),recaps=display==null?null:display.optJSONObject("settlements");if(recaps!=null)for(String id:AccountProgress.keys(recaps)){try{JSONObject recap=recaps.getJSONObject(id),oldBill=recap.getJSONObject("bill");java.time.LocalDate today=LedgerAggregation.day(System.currentTimeMillis());if(snapshot!=null&&!oldBill.getString("plan").equals(LedgerRecord.cleanPlan(snapshot.planType))&&!today.isBefore(java.time.LocalDate.parse(oldBill.getString("start")))&&today.isBefore(java.time.LocalDate.parse(oldBill.getString("end")))){Ui.addSpacer(details,8);details.addView(LedgerUi.caption(a,oldBill.getString("plan")+" · "+oldBill.getString("start")+" ~ "+oldBill.getString("end"),dark));JSONObject buckets=recap.getJSONObject("buckets");for(String policy:AccountProgress.keys(buckets)){JSONObject v=buckets.getJSONObject(policy);String detail=policy+" · "+LedgerUi.number(v.getDouble("percent"))+"%";if(!v.isNull("reference_value"))detail+=" · "+FunActivity.money(oldBill.getString("currency"),new BigDecimal(v.getString("reference_value")));details.addView(LedgerUi.caption(a,detail,dark));}details.addView(LedgerUi.caption(a,a.getString(R.string.ref_mixed_billing),dark));}}catch(Exception ignored){details.addView(LedgerUi.caption(a,a.getString(R.string.rpg_mixed_policy),dark));}}
        android.widget.Button more=LedgerUi.action(a,a.getString(R.string.phase_a_billing_details),false,dark,()->{});
        more.setOnClickListener(v->{boolean open=details.getVisibility()!=android.view.View.VISIBLE;details.setVisibility(open?android.view.View.VISIBLE:android.view.View.GONE);more.setText(a.getString(open?R.string.phase_a_less:R.string.phase_a_billing_details));});
        card.addView(more);card.addView(details);
        Ui.addSpacer(card,10);card.addView(LedgerUi.action(a,a.getString(R.string.rpg3_billing_history),false,dark,()->a.startActivity(new android.content.Intent(a,BillingHistoryActivity.class))));
        return card;
    }
}
