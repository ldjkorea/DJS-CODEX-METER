package dev.bennett.codexmeter;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
public final class TrialWelcomeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle saved){Ui.applySelectedTheme(this);super.onCreate(saved);
        if(!"dev.bennett.codexmeter.trial".equals(getPackageName())){startActivity(new Intent(this,MainActivity.class));finish();return;}
        if(!BetaIdentity.done(this)&&!BetaIdentity.prefs(this).getString("name","").isEmpty())BetaIdentity.prefs(this).edit().putBoolean("setup_done",true).apply();
        if(!BetaIdentity.done(this)){startActivity(new Intent(this,BetaSetupActivity.class));finish();return;}
        BetaUpdatePolicy.initialize(this);boolean dark=Ui.isDark(this);LinearLayout body=Ui.installPage(this,"GPT HUD · "+AppConstants.VERSION_NAME,false).content;
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=findViewById(R.id.dashboard_refresh);refresh.setEnabled(false);refresh.setRefreshing(false);
        body.addView(BetaUi.title(this,BetaIdentity.name(this),dark));Ui.addSpacer(body,8);body.addView(BetaUi.body(this,BetaCopy.t(this,"welcome_note"),dark));
        LinearLayout demo=BetaUi.group(body,BetaCopy.t(this,"demo_open"),dark);demo.addView(BetaUi.note(this,BetaCopy.t(this,"demo_note"),dark));demo.addView(BetaUi.action(this,BetaCopy.t(this,"demo_open"),true,dark,()->BetaGrowthStore.WORKER.execute(()->{
            try{TrialDemo.prepare(getApplicationContext());BetaDemo995.prepare(getApplicationContext());runOnUiThread(()->startActivity(new Intent(this,MainActivity.class)));}catch(Exception e){runOnUiThread(()->Toast.makeText(this,BetaCopy.t(this,"failed"),Toast.LENGTH_LONG).show());}
        })));
        LinearLayout real=BetaUi.group(body,BetaCopy.t(this,"own_account"),dark);real.addView(BetaUi.note(this,BetaCopy.t(this,"transition_note"),dark));real.addView(BetaUi.action(this,BetaCopy.t(this,"own_account"),false,dark,()->BetaGrowthStore.WORKER.execute(()->{try{TrialDemo.leaveDemo(getApplicationContext());runOnUiThread(()->startActivity(new Intent(this,MainActivity.class).putExtra("start_sign_in",true)));}catch(Exception e){runOnUiThread(()->Toast.makeText(this,BetaCopy.t(this,"failed"),Toast.LENGTH_LONG).show());}})));
        LinearLayout info=BetaUi.group(body,BetaCopy.t(this,"app_info"),dark);info.addView(BetaUi.row(this,BetaCopy.t(this,"updates"),AppConstants.VERSION_NAME,dark,()->startActivity(new Intent(this,UpdateActivity.class))));info.addView(BetaUi.row(this,BetaCopy.t(this,"diagnostics"),"",dark,()->BetaDiagnostics.show(this)));
    }
}
