package dev.bennett.codexmeter;

import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Read-only account settlements; each policy and currency stays separate. */
public final class BillingHistoryActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);boolean dark=Ui.isDark(this);
        LinearLayout content=Ui.installPage(this,getString(R.string.rpg3_billing_history),true).content;
        content.addView(LedgerUi.caption(this,getString(R.string.next_loading),dark));String account=SubscriptionStore.accountKey(this);
        WORKER.execute(()->{JSONObject recaps=new JSONObject();try{JSONObject p=RpgProgressStore.peek(getApplicationContext());if(p!=null)recaps=new JSONObject(p.getJSONObject("settlements").toString());}catch(Exception ignored){}
            final JSONObject rows=recaps;runOnUiThread(()->{if(isDestroyed()||isFinishing()||account==null||!account.equals(SubscriptionStore.accountKey(this)))return;content.removeAllViews();
                List<String> ids=AccountProgress.keys(rows);ids.sort((a,b)->rows.optJSONObject(b).optJSONObject("bill").optString("start").compareTo(rows.optJSONObject(a).optJSONObject("bill").optString("start")));
                if(ids.isEmpty())content.addView(LedgerUi.caption(this,getString(R.string.rpg3_no_progress),dark));
                for(String id:ids){try{JSONObject recap=rows.getJSONObject(id),bill=recap.getJSONObject("bill");LinearLayout card=Ui.card(this,dark);
                    card.addView(LedgerUi.heading(this,PlanArtwork.label(bill.getString("plan")),dark));card.addView(LedgerUi.caption(this,getString(R.string.rpg_billing_period,bill.getString("start"),bill.getString("end")),dark));
                    card.addView(LedgerUi.caption(this,getString(R.string.rpg3_payment,FunActivity.money(bill.getString("currency"),new BigDecimal(bill.getString("amount")))),dark));
                    JSONObject buckets=recap.getJSONObject("buckets");for(String policy:AccountProgress.keys(buckets)){JSONObject value=buckets.getJSONObject(policy);
                        card.addView(LedgerUi.caption(this,getString(policy.startsWith("weekly|")?R.string.ui_weekly_158f3d:R.string.ui_monthly_d31edb)+" · "+PlanArtwork.label(policy.split("\\|")[1]),dark));
                        card.addView(LedgerUi.amount(this,getString(R.string.rpg3_billing_usage,LedgerUi.number(new BigDecimal(value.getString("percent")).doubleValue())),dark,20));
                        card.addView(LedgerUi.caption(this,getString(R.string.rpg3_billing_value)+" · "+(value.isNull("reference_value")?"—":FunActivity.money(bill.getString("currency"),new BigDecimal(value.getString("reference_value")))),dark));
                        card.addView(LedgerUi.caption(this,getString(R.string.rpg_partial),dark));
                    }
                    card.addView(LedgerUi.caption(this,getString(recap.optString("status").equals("final")?R.string.rpg3_confirmed:R.string.rpg3_pending),dark));content.addView(card);Ui.addSpacer(content,10);
                }catch(Exception ignored){content.addView(LedgerUi.caption(this,getString(R.string.rpg3_failed),dark));}}
                content.addView(LedgerUi.caption(this,getString(R.string.rpg3_billing_note),dark));
            });
        });
    }
}
