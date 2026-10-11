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
    private LinearLayout content; private boolean busy,dark; private String signature="";
    private int scroll; private long generation; private boolean receiverRegistered;
    private final BroadcastReceiver updates=new BroadcastReceiver(){public void onReceive(Context c,Intent i){load(false);}};
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);dark=Ui.isDark(this);
        content=Ui.installPage(this,getString(R.string.rpg3_growth),false).content;MatteNav.install(this,3);
        if(state!=null)scroll=state.getInt("growth_scroll");
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){public void handleOnBackPressed(){if(getIntent().getBooleanExtra("catalog",false)){getIntent().putExtra("catalog",false);scroll=0;load(true);}else MatteNav.open(GrowthActivity.this,1);}});
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);signature="";load(true);}
    @Override protected void onResume(){super.onResume();
        if(!receiverRegistered){androidx.core.content.ContextCompat.registerReceiver(this,updates,new IntentFilter(AppConstants.ACTION_USAGE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);receiverRegistered=true;}load(false);
    }
    @Override protected void onPause(){if(receiverRegistered){unregisterReceiver(updates);receiverRegistered=false;}super.onPause();}
    @Override protected void onDestroy(){generation++;super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);android.view.View view=findViewById(R.id.dashboard_scroll);state.putInt("growth_scroll",view==null?scroll:view.getScrollY());}
    void load(boolean force){
        if(busy||content==null)return;String account=SubscriptionStore.accountKey(this);
        String next=account+":"+getIntent().getBooleanExtra("catalog",false)+":"+getSharedPreferences("codex_rpg_display_v1",0).getAll().hashCode()+":"+LiveUsageStore.signature(this,AppPreferences.loadSnapshot(this));
        if(!force&&next.equals(signature)&&content.getChildCount()>0)return;
        if(account==null){generation++;signature=next;content.removeAllViews();content.addView(LedgerUi.caption(this,getString(R.string.rpg3_no_account),dark));return;}
        busy=true;long ticket=++generation;
        if(content.getChildCount()==0)content.addView(LedgerUi.caption(this,getString(R.string.next_loading),dark));
        WORKER.execute(()->{JSONObject journal=null;boolean failed=false;
            try{journal=RpgProgressStore.export(getApplicationContext(),account);}catch(Exception e){failed=true;}
            final JSONObject result=journal;final boolean error=failed;
            runOnUiThread(()->{busy=false;if(isDestroyed()||isFinishing()||ticket!=generation)return;
                if(!account.equals(SubscriptionStore.accountKey(this))){signature="";load(true);return;}
                android.view.View view=findViewById(R.id.dashboard_scroll);if(content.getChildCount()>1&&view!=null)scroll=view.getScrollY();
                signature=next;content.removeAllViews();
                if(error)content.addView(LedgerUi.caption(this,getString(R.string.rpg3_failed),dark));
                else RpgGrowthUi.render(this,content,result,dark,getIntent().getBooleanExtra("catalog",false));
                if(view!=null)view.post(()->view.scrollTo(0,scroll));
            });
        });
    }
    void equip(String title){String account=SubscriptionStore.accountKey(this);if(account==null||busy)return;busy=true;
        WORKER.execute(()->{boolean saved=false;try{saved=RpgProgressStore.equip(getApplicationContext(),account,title);}catch(Exception ignored){}final boolean ok=saved;
            runOnUiThread(()->{busy=false;if(!isDestroyed()&&account.equals(SubscriptionStore.accountKey(this))){android.widget.Toast.makeText(this,ok?R.string.rpg3_saved:R.string.rpg3_failed,android.widget.Toast.LENGTH_SHORT).show();load(true);}});
        });
    }
    void recommend(){String account=SubscriptionStore.accountKey(this);if(account==null||busy)return;busy=true;
        WORKER.execute(()->{try{RpgProgressStore.recommend(getApplicationContext(),account);}catch(Exception ignored){}runOnUiThread(()->{busy=false;if(!isDestroyed())load(true);});});
    }
}
