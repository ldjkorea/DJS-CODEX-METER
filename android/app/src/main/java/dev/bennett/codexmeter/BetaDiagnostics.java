package dev.bennett.codexmeter;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
/** User-opened local status. OAuth URLs, tokens and conversation content are never collected. */
final class BetaDiagnostics {
    static String summary(Context c){
        PackageInfo info;try{info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);}catch(Exception e){return BetaExperienceUi.t(c,"앱 버전 조회 실패","App version unavailable");}
        long versionCode=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
        return "GPT HUD BETA\n"+BetaExperienceUi.t(c,"버전: %s / %d","Version: %s / %d",info.versionName,versionCode)
            +"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\n"+BetaExperienceUi.t(c,"패키지: %s","Package: %s",c.getPackageName())
            +"\n"+BetaExperienceUi.t(c,"업데이트 기능: %s · 자동 확인: %s","Updates enabled: %s · automatic: %s",BuildConfig.KOREAN_UPDATES_ENABLED,UpdatePreferences.automaticChecks(c))
            +"\n"+BetaExperienceUi.t(c,"APK 설치 허용: %s","APK install source allowed: %s",c.getPackageManager().canRequestPackageInstalls())
            +"\n"+BetaExperienceUi.t(c,"초기 설정 완료: %s · 가상 데모: %s","Setup completed: %s · demo: %s",AppPreferences.isOnboardingComplete(c),TrialDemo.active(c))
            +"\n"+BetaExperienceUi.t(c,"로그인 준비 중: %s","Sign-in pending: %s",AppPreferences.isOAuthPending(c))
            +"\n"+BetaExperienceUi.t(c,"업데이트 확인 오류: %s","Update check error: %s",DiagnosticSanitizer.redact(UpdatePreferences.lastError(c)))
            +"\n"+BetaExperienceUi.t(c,"설치 오류: %s","Install error: %s",DiagnosticSanitizer.redact(UpdatePreferences.installError(c)))
            +"\n"+BetaExperienceUi.t(c,"앱 실행 오류: %s","Scheduler error: %s",DiagnosticSanitizer.redact(AppPreferences.getSchedulerError(c)));
    }
    static void show(AppCompatActivity a){String text=summary(a);new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(BetaExperienceUi.t(a,"로그인·업데이트 진단","Sign-in and update diagnostics")).setMessage(text).setNegativeButton(BetaCopy.t(a,"close"),null).setPositiveButton(BetaExperienceUi.t(a,"진단 정보 복사","Copy diagnostics"),(d,w)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("BETA diagnostics",text));}).show();}
}
