package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.lang.reflect.*;

/** Real welcome button routes on an owned fresh fixture, without seed shortcuts. */
public final class BetaEntryNativeTest extends Instrumentation {
    volatile Activity resumed;String phase;int checks;
    void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    static View find(View v,String text){if(v instanceof Button&&((Button)v).getText().toString().equals(text))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=find(((ViewGroup)v).getChildAt(i),text);if(r!=null)return r;}return null;}
    static Object field(Object o,String name)throws Exception {Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    @Override public void onCreate(Bundle a){super.onCreate(a);phase=a.getString("phase");start();}
    void idle(long ms)throws Exception {Thread.sleep(ms);waitForIdleSync();}
    @Override public void onStart(){Context c=getTargetContext();Bundle result=new Bundle();int code=0;ActivityMonitor browser=null;
        Application.ActivityLifecycleCallbacks life=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){resumed=a;}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};
        Application app=(Application)c.getApplicationContext();app.registerActivityLifecycleCallbacks(life);
        try{
            check(c.getPackageName().equals("dev.bennett.codexmeter.trial")&&Build.MODEL.contains("sdk"),"Owned beta emulator");check(SecureTokenStore.load(c)==null,"Fresh fixture has no account");check(!AppPreferences.isOnboardingComplete(c),"No demo bypass or completed onboarding");
            startActivitySync(new Intent(c,TrialWelcomeActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle(1000);check(resumed instanceof TrialWelcomeActivity,"Actual welcome after locale recreation");
            runOnMainSync(()->find(resumed.findViewById(android.R.id.content),"BETA 업데이트 확인").performClick());idle(400);check(resumed instanceof UpdateActivity,"Actual update button opens update page");
            UpdateActivity update=(UpdateActivity)resumed;for(int i=0;i<100&&Boolean.TRUE.equals(field(update,"operationRunning"));i++)Thread.sleep(400);waitForIdleSync();
            check(field(update,"release")!=null,"Actual update UI loaded a release");check(((GitHubRelease)field(update,"release")).tag.startsWith("beta-v9.9."),"Actual page picked a beta release");
            runOnMainSync(update::finish);idle(400);
            IntentFilter f=new IntentFilter(Intent.ACTION_VIEW);f.addDataScheme("https");f.addDataScheme("http");browser=addMonitor(f,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            runOnMainSync(()->find(resumed.findViewById(android.R.id.content),"내 계정으로 체험판 사용").performClick());idle(1800);check(resumed instanceof OnboardingActivity,"Account button reached existing onboarding");
            if(phase.equals("fresh-before")){check(!AppPreferences.isOAuthPending(c),"Reproduced lost start_sign_in on fresh first-run route");check((int)field(resumed,"step")==OnboardingFlow.STEP_WELCOME,"Fresh account button stopped at welcome instead of login");}
            else {for(int i=0;i<60&&!AppPreferences.isOAuthPending(c);i++)Thread.sleep(100);check(AppPreferences.isOAuthPending(c),"Fresh account click starts real OAuth service");check(browser.getHits()>0,"Fresh account click reaches browser launch");check((int)field(resumed,"step")==OnboardingFlow.STEP_ACCOUNT,"Account step displayed");}
            Method callback=MainActivity.class.getDeclaredMethod("isOAuthReturnIntent",Intent.class);callback.setAccessible(true);boolean accepts=(boolean)callback.invoke(null,new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(AppConstants.APP_LINK)));
            check(accepts==!phase.equals("fresh-before"),phase.equals("fresh-before")?"Reproduced beta callback rejected by hard-coded normal scheme":"Actual MainActivity accepts beta return URI");
            check(SecureTokenStore.load(c)==null,"No real credentials used or created");
            if(phase.equals("fresh-after")){Class<?> diagnostics=Class.forName("dev.bennett.codexmeter.BetaDiagnostics");Method summary=diagnostics.getDeclaredMethod("summary",Context.class);summary.setAccessible(true);String text=(String)summary.invoke(null,c);check(text.contains("9.9.3")&&text.contains("990003"),"Diagnostics show actually installed version and code");check(!text.contains(AppPreferences.getOAuthUrl(c))&&!text.contains("code_challenge="),"Diagnostics omit OAuth query and PKCE data");}
            result.putString("stream","Beta entry "+phase+": "+checks+" checks passed; actual welcome/update/account buttons and unseeded first-run routing.\n");code=-1;
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));}
        finally{try{c.startService(new Intent(c,OAuthService.class).setAction(OAuthService.ACTION_CANCEL_SILENT));}catch(Exception ignored){}if(browser!=null)removeMonitor(browser);if(resumed!=null)runOnMainSync(()->resumed.finish());app.unregisterActivityLifecycleCallbacks(life);}
        finish(code,result);
    }
}
