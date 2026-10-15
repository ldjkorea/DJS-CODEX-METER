package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import org.json.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicReference;
/** Real Activity recreation and denied-permission recovery; never downloads or installs an APK. */
public final class Beta997UpdateRecoveryNativeTest extends Instrumentation {
    int checks;Throwable failure;
    void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
    void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable t){failure=t;}});if(failure!=null){Throwable problem=failure;failure=null;throw new AssertionError(problem);}}
    Field field(String name)throws Exception{Field f=UpdateActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("codex_meter_updates_v1",0);Map<String,?> old=prefs.getAll();AtomicReference<UpdateActivity> latest=new AtomicReference<>();Application app=(Application)c.getApplicationContext();
        Application.ActivityLifecycleCallbacks callbacks=new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity a,Bundle b){if(a instanceof UpdateActivity)latest.set((UpdateActivity)a);}public void onActivityStarted(Activity a){}public void onActivityResumed(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}};
        try{
            check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic beta only");check(!c.getPackageManager().canRequestPackageInstalls(),"Test requires denied install permission; no download can start");
            String base=GitHubReleaseSource.REPOSITORY_URL,tag="beta-v9.9.8";JSONObject fixture=new JSONObject().put("tag_name",tag).put("prerelease",true).put("html_url",base+"/releases/tag/"+tag).put("assets",new JSONArray().put(new JSONObject().put("name","GPT-HUD-BETA-9.9.8.apk").put("size",24000000).put("browser_download_url",base+"/releases/download/"+tag+"/GPT-HUD-BETA-9.9.8.apk")).put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",base+"/releases/download/"+tag+"/SHA256SUMS.txt")));UpdatePreferences.saveSuccess(c,new JSONArray().put(fixture).toString(),"");
            app.registerActivityLifecycleCallbacks(callbacks);UpdateActivity first=(UpdateActivity)startActivitySync(new Intent(c,UpdateActivity.class).putExtra(UpdateActivity.EXTRA_VERSION,"9.9.8").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();Field waiting=field("waitingForInstallPermission"),running=field("operationRunning"),status=field("status");
            Method request=UpdateActivity.class.getDeclaredMethod("requestInstall");request.setAccessible(true);main(()->{try{request.invoke(first);}catch(Exception e){throw new RuntimeException(e);}});check(waiting.getBoolean(first),"Explicit install waits for permission");
            main(first::recreate);for(int i=0;i<60&&(latest.get()==first||latest.get()==null);i++)Thread.sleep(100);UpdateActivity second=latest.get();Thread.sleep(400);waitForIdleSync();check(second!=first&&!second.isDestroyed(),"Actual Activity recreated");check(waiting.getBoolean(second),"Permission wait survives recreation");check(((GitHubRelease)field("release").get(second)).version.equals("9.9.8"),"Selected version survives recreation");check(!running.getBoolean(second),"No duplicate automatic download after denied permission");
            check(((TextView)status.get(second)).getText().toString().equals(BetaCopy.t(second,"update_permission_pending")),"Permission retry guidance shown");
            main(()->{try{request.invoke(second);((androidx.appcompat.app.AlertDialog)field("installPermissionDialog").get(second)).cancel();}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();check(!waiting.getBoolean(second),"Back/cancel clears pending permission request");
            main(()->{try{running.setBoolean(second,true);second.getIntent().putExtra(UpdateActivity.EXTRA_START_INSTALL,true);second.recreate();}catch(Exception e){throw new RuntimeException(e);}});for(int i=0;i<60&&(latest.get()==second||latest.get()==null);i++)Thread.sleep(100);UpdateActivity third=latest.get();Thread.sleep(400);waitForIdleSync();
            check(third!=second&&!running.getBoolean(third),"Interrupted work restored as retryable idle state");check(!third.getIntent().hasExtra(UpdateActivity.EXTRA_START_INSTALL),"Consumed install intent cannot replay on recreation");check(((TextView)status.get(third)).getText().toString().equals(BetaCopy.t(third,"update_interrupted")),"Interrupted preparation explains retry");
            check(!waiting.getBoolean(third),"Cancelled permission state remains cancelled");
            try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("appops set dev.bennett.codexmeter.trial REQUEST_INSTALL_PACKAGES allow");java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
            check(c.getPackageManager().canRequestPackageInstalls(),"Granted permission recognized by actual PackageManager");
            Object installLock=field("INSTALL_LOCK").get(null);UpdateActivity resumed;
            synchronized(installLock){
                main(()->{try{waiting.setBoolean(third,true);third.recreate();}catch(Exception e){throw new RuntimeException(e);}});
                for(int i=0;i<60&&(latest.get()==third||latest.get()==null);i++)Thread.sleep(100);resumed=latest.get();Thread.sleep(400);waitForIdleSync();
                check(resumed!=third&&!waiting.getBoolean(resumed)&&running.getBoolean(resumed),"Permission grant resumes one pending preparation after recreation");
                UpdatePreferences.setInstallError(c,"SYNTHETIC_NEWER_OPERATION");UpdateActivity stopped=resumed;main(stopped::finish);
            }
            java.util.concurrent.ExecutorService worker=(java.util.concurrent.ExecutorService)field("executor").get(resumed);
            check(worker.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS),"Destroyed preparation exits before network/file work");
            check(UpdatePreferences.installError(c).equals("SYNTHETIC_NEWER_OPERATION"),"Cancelled old worker cannot overwrite newer error state");
            check(UpdatePreferences.installedVersion(c).equals("9.9.7"),"No installer or package change occurred");
            result.putString("stream","Beta997 updater recovery: "+checks+" checks passed; actual recreation/permission cancellation/interrupted state, no APK download or install.\n");code=-1;
        }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}finally{UpdateActivity a=latest.get();if(a!=null)main(a::finish);app.unregisterActivityLifecycleCallbacks(callbacks);SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> entry:old.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);}e.commit();}finish(code,result);
    }
}
