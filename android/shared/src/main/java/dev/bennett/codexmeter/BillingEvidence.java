package dev.bennett.codexmeter;

import java.math.BigDecimal;
import org.json.JSONObject;

/** Read-only payment matching. Never relabel an old money figure with a new currency. */
public final class BillingEvidence {
    private BillingEvidence() { }
    public static boolean matches(JSONObject recap,SubscriptionCost cost,String plan) {
        if(recap==null||cost==null)return false;
        try {
            JSONObject bill=recap.getJSONObject("bill");
            return bill.getString("plan").equals(LedgerRecord.cleanPlan(plan))
                    &&bill.getString("start").equals(cost.start.toString())
                    &&bill.getString("end").equals(cost.end.toString())
                    &&bill.getString("currency").equals(cost.currency)
                    &&new BigDecimal(bill.getString("amount")).compareTo(cost.amount)==0;
        }catch(Exception invalid){return false;}
    }
}
