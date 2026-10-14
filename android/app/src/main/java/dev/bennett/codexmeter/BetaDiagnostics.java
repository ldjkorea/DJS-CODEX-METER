package dev.bennett.codexmeter;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;

/** User-opened local status only. Never exports tokens, account IDs, URLs or conversations. */
final class BetaDiagnostics {
    static String summary(Context c){
        PackageInfo info;try{info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);}catch(Exception e){return "앱 버전 조회 실패";}
        long versionCode=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
        return "GPT HUD BETA 진단\n버전: "+info.versionName+" / "+versionCode+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\n패키지: "+c.getPackageName()
            +"\n업데이트 기능: "+BuildConfig.KOREAN_UPDATES_ENABLED+"\n자동 확인: "+UpdatePreferences.automaticChecks(c)
            +"\nAPK 설치 허용: "+c.getPackageManager().canRequestPackageInstalls()
            +"\n초기 설정 완료: "+AppPreferences.isOnboardingComplete(c)+"\n가상 데모: "+TrialDemo.active(c)
            +"\n로그인 준비 중: "+AppPreferences.isOAuthPending(c)
            +"\n업데이트 확인 오류: "+DiagnosticSanitizer.redact(UpdatePreferences.lastError(c))
            +"\n설치 오류: "+DiagnosticSanitizer.redact(UpdatePreferences.installError(c))
            +"\n앱 실행 오류: "+DiagnosticSanitizer.redact(AppPreferences.getSchedulerError(c));
    }
    static void show(AppCompatActivity a){String text=summary(a);new androidx.appcompat.app.AlertDialog.Builder(a).setTitle("로그인 · 업데이트 진단").setMessage(text).setNegativeButton("닫기",null).setPositiveButton("진단 정보 복사",(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("BETA 진단",text));}).show();}
}
