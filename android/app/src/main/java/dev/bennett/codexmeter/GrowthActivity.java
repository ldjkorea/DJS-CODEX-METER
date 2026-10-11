package dev.bennett.codexmeter;

import android.content.*;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Retained growth root. Full journals are read on a worker only. */
public final class GrowthActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private LinearLayout content; private boolean busy,dark; private String signature="",loadedAccount="";
    androidx.appcompat.app.AlertDialog detailDialog;
    private int scroll; private long generation; private boolean receiverRegistered;
    private final BroadcastReceiver updates=new BroadcastReceiver(){public void onReceive(Context c,Intent i){load(false);}};
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);dark=Ui.isDark(this);
        content=Ui.installPage(this,getString(R.string.rpg3_growth),false).content;MatteNav.install(this,3);
        if(state!=null){scroll=state.getInt("growth_scroll");getIntent().putExtra("catalog",false).putExtra("growth_mode",state.getString("growth_mode","overview"));}
        String destination=mode();if(!destination.equals("overview")){getIntent().putExtra("catalog",false).putExtra("growth_mode","overview");content.post(()->RpgCollectionActivity.open(this,destination));}
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){if(!mode().equals("overview")){open("overview");}else MatteNav.open(GrowthActivity.this,1);}});
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);String destination=mode();getIntent().putExtra("catalog",false).putExtra("growth_mode","overview");if(!destination.equals("overview"))RpgCollectionActivity.open(this,destination);signature="";load(true);}
    @Override protected void onResume(){super.onResume();
        if(!receiverRegistered){androidx.core.content.ContextCompat.registerReceiver(this,updates,new IntentFilter(AppConstants.ACTION_USAGE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);receiverRegistered=true;}load(false);
    }
    @Override protected void onPause(){if(receiverRegistered){unregisterReceiver(updates);receiverRegistered=false;}super.onPause();}
    @Override protected void onDestroy(){generation++;if(detailDialog!=null)detailDialog.dismiss();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);android.view.View view=findViewById(R.id.dashboard_scroll);state.putInt("growth_scroll",view==null?scroll:view.getScrollY());state.putString("growth_mode",mode());}
    void load(boolean force){
        if(busy||content==null)return;String account=SubscriptionStore.accountKey(this);
        String next=account+":"+mode()+":"+getSharedPreferences("codex_rpg_showcase_v1",0).getAll().hashCode()+":"+getSharedPreferences("codex_rpg_display_v1",0).getAll().hashCode()+":"+LiveUsageStore.signature(this,AppPreferences.loadSnapshot(this));
        if(!force&&next.equals(signature)&&content.getChildCount()>0)return;
        if(account==null){if(detailDialog!=null)detailDialog.dismiss();loadedAccount="";generation++;signature=next;content.removeAllViews();content.addView(LedgerUi.caption(this,getString(R.string.rpg3_no_account),dark));return;}
        busy=true;long ticket=++generation;
        if(content.getChildCount()==0)content.addView(LedgerUi.caption(this,getString(R.string.next_loading),dark));
        WORKER.execute(()->{JSONObject journal=null;java.util.List<String> unlocks=java.util.Collections.emptyList();boolean failed=false;
            try{journal=RpgProgressStore.export(getApplicationContext(),account);unlocks=RpgShowcase.observe(getApplicationContext(),account,journal==null?null:journal.optJSONObject("rpg"));}catch(Exception e){failed=true;}
            final JSONObject result=journal;final boolean error=failed;final java.util.List<String> earned=unlocks;
            runOnUiThread(()->{busy=false;if(isDestroyed()||isFinishing()||ticket!=generation)return;
                if(!account.equals(SubscriptionStore.accountKey(this))){if(detailDialog!=null)detailDialog.dismiss();signature="";load(true);return;}
                android.view.View view=findViewById(R.id.dashboard_scroll);if(content.getChildCount()>1&&view!=null)scroll=view.getScrollY();
                loadedAccount=account;signature=next;content.removeAllViews();
                if(error)content.addView(LedgerUi.caption(this,getString(R.string.rpg3_failed),dark));
                else {
                    if(!earned.isEmpty()){
                        android.widget.TextView banner=LedgerUi.badge(this,getString(R.string.growth_new_unlock,earned.size()),dark,false);banner.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);banner.setOnClickListener(v->open("achievements"));content.addView(banner);Ui.addSpacer(content,12);
                        if(android.provider.Settings.Global.getFloat(getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1)>0){banner.setAlpha(.4f);banner.animate().alpha(1).setDuration(450).start();}
                    }
                    RpgGrowthUi.render(this,content,result,dark,mode());
                }
                if(view!=null)view.post(()->view.scrollTo(0,scroll));
            });
        });
    }
    String mode(){String value=getIntent().getStringExtra("growth_mode");return "titles".equals(value)?"titles":"achievements".equals(value)||getIntent().getBooleanExtra("catalog",false)?"achievements":"overview";}
    void open(String mode){if(busy)return;if(!mode.equals("overview")){RpgCollectionActivity.open(this,mode);return;}if(detailDialog!=null)detailDialog.dismiss();getIntent().putExtra("catalog",false).putExtra("growth_mode",mode);scroll=0;android.view.View view=findViewById(R.id.dashboard_scroll);if(view!=null)view.scrollTo(0,0);load(true);}
    void detail(String kind,JSONObject state){if(isFinishing()||isDestroyed()||!loadedAccount.equals(SubscriptionStore.accountKey(this)))return;if(detailDialog!=null)detailDialog.dismiss();detailDialog=RpgDetails.show(this,kind,state,dark);}
    void chooseShowcase(JSONObject state){if(!busy&&loadedAccount.equals(SubscriptionStore.accountKey(this)))RpgCollectionActivity.open(this,"showcase");}
    void equip(String title){String account=SubscriptionStore.accountKey(this);if(account==null||busy)return;busy=true;
        WORKER.execute(()->{boolean saved=false;try{saved=RpgProgressStore.equip(getApplicationContext(),account,title);}catch(Exception ignored){}final boolean ok=saved;
            runOnUiThread(()->{busy=false;if(!isDestroyed()&&account.equals(SubscriptionStore.accountKey(this))){android.widget.Toast.makeText(this,ok?R.string.rpg3_saved:R.string.rpg3_failed,android.widget.Toast.LENGTH_SHORT).show();load(true);}});
        });
    }
    void recommend(){String account=SubscriptionStore.accountKey(this);if(account==null||busy)return;busy=true;
        WORKER.execute(()->{try{RpgProgressStore.recommend(getApplicationContext(),account);}catch(Exception ignored){}runOnUiThread(()->{busy=false;if(!isDestroyed()&&account.equals(SubscriptionStore.accountKey(this)))load(true);});});
    }
}
