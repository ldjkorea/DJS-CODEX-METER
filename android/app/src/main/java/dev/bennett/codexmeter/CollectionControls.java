package dev.bennett.codexmeter;

import android.content.Intent;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executors;

/** Destructive collection controls require explicit, account-checked confirmation. */
final class CollectionControls {
    private static final java.util.concurrent.ExecutorService WORKER=Executors.newSingleThreadExecutor();
    static void add(AppCompatActivity a,LinearLayout parent,boolean dark){
        LinearLayout card=Ui.card(a,dark);card.addView(LedgerUi.heading(a,a.getString(R.string.reborn_collections),dark));
        card.addView(LedgerUi.caption(a,a.getString(R.string.collection_restart_note),dark));
        for(boolean titles:new boolean[]{false,true})card.addView(LedgerUi.action(a,a.getString(titles?R.string.collection_reset_titles:R.string.collection_reset_medals),false,dark,()->confirm(a,titles)));
        parent.addView(card);Ui.addSpacer(parent,16);
    }
    private static void confirm(AppCompatActivity a,boolean titles){
        String account=SubscriptionStore.accountKey(a);if(account==null){Toast.makeText(a,R.string.rpg3_no_account,Toast.LENGTH_SHORT).show();return;}
        new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(titles?R.string.collection_reset_titles:R.string.collection_reset_medals)
                .setMessage(R.string.collection_reset_confirmation).setNegativeButton(android.R.string.cancel,null)
                .setPositiveButton(R.string.journey_confirm,(d,w)->WORKER.execute(()->{
                    boolean ok=false;try{ok=RpgProgressStore.resetCollection(a.getApplicationContext(),account,titles);}catch(Exception ignored){}
                    final boolean saved=ok;a.runOnUiThread(()->{if(a.isDestroyed()||!account.equals(SubscriptionStore.accountKey(a)))return;
                        Toast.makeText(a,saved?R.string.collection_reset_done:R.string.rpg3_failed,Toast.LENGTH_LONG).show();
                        if(saved){a.sendBroadcast(new Intent(AppConstants.ACTION_USAGE_UPDATED).setPackage(a.getPackageName()));WidgetRenderer.updateAll(a);a.recreate();}
                    });
                })).show();
    }
}
