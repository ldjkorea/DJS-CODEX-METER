package dev.bennett.codexmeter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.concurrent.*;

/** Compiled only into the separate trial APK. Never included in the normal app. */
public final class TrialWelcomeActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    @Override protected void onCreate(Bundle saved){
        Ui.applySelectedTheme(this);super.onCreate(saved);
        if(!getSharedPreferences("trial_welcome",0).getBoolean("language_chosen",false)){
            getSharedPreferences("trial_welcome",0).edit().putBoolean("language_chosen",true).apply();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ko"));
        }
        BetaUpdatePolicy.initialize(this);
        boolean dark=Ui.isDark(this);LinearLayout body=Ui.installPage(this,"GPT HUD BETA · "+AppConstants.VERSION_NAME,false).content;
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=findViewById(R.id.dashboard_refresh);refresh.setEnabled(false);
        body.addView(LedgerUi.heading(this,"RPG 3.0 · AI의 한마디 2.0",dark));
        body.addView(LedgerUi.caption(this,"기존 GPT HUD와 별도로 설치됩니다. BETA 업데이트를 자동 확인하며, 설치는 Android 확인 화면에서 직접 선택합니다.",dark));
        body.addView(LedgerUi.action(this,"BETA 업데이트 확인",false,dark,()->startActivity(new Intent(this,UpdateActivity.class))));
        Ui.addSpacer(body,20);
        body.addView(LedgerUi.action(this,"로그인 없이 데모 둘러보기",true,dark,()->demo()));
        body.addView(LedgerUi.caption(this,"첫 달 활동을 가정한 가상 계정으로 Lv.30대 성장·명예·매운맛 코치를 보여줍니다. 표시된 사용량과 성취는 실제 기록이 아닙니다.",dark));
        Ui.addSpacer(body,20);
        body.addView(LedgerUi.action(this,"내 계정으로 체험판 사용",false,dark,()->{
            WORKER.execute(()->{try{TrialDemo.leaveDemo(getApplicationContext());runOnUiThread(()->startActivity(new Intent(this,MainActivity.class)));}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"로그인 준비에 실패했습니다.",Toast.LENGTH_LONG).show());}});
        }));
        body.addView(LedgerUi.caption(this,"이 체험판에 별도로 로그인합니다. 기존 3.0 앱의 로그인이나 데이터를 가져오지 않습니다. 새 기록은 체험판에서 처음부터 쌓입니다.",dark));
    }
    private void demo(){
        Toast.makeText(this,"가상 체험 화면을 준비합니다.",Toast.LENGTH_SHORT).show();
        WORKER.execute(()->{try{TrialDemo.prepare(getApplicationContext());runOnUiThread(()->startActivity(new Intent(this,MainActivity.class)));}catch(Exception e){runOnUiThread(()->new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("데모 준비 실패").setMessage(e.getMessage()).setPositiveButton(android.R.string.ok,null).show());}});
    }
}
