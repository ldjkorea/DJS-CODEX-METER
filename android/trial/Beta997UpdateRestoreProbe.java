package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.lang.reflect.Field;
import java.util.Map;

/** Checks real Activity saved state without downloading an APK or opening an installer. */
public final class Beta997UpdateRestoreProbe extends Instrumentation {
    Throwable failure;
    void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable t){failure=t;}});if(failure!=null){Throwable problem=failure;failure=null;throw new AssertionError(problem);}}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;UpdateActivity activity=null;Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("codex_meter_updates_v1",0);Map<String,?> old=prefs.getAll();
        try{
            if(!Build.MODEL.contains("sdk")||!c.getPackageName().equals("dev.bennett.codexmeter.trial")||!TrialDemo.active(c))throw new AssertionError("Owned synthetic beta only");
            String base=GitHubReleaseSource.REPOSITORY_URL,tag="beta-v9.9.8";
            JSONObject fixture=new JSONObject().put("tag_name",tag).put("prerelease",true).put("html_url",base+"/releases/tag/"+tag).put("assets",new JSONArray().put(new JSONObject().put("name","GPT-HUD-BETA-9.9.8.apk").put("size",24000000).put("browser_download_url",base+"/releases/download/"+tag+"/GPT-HUD-BETA-9.9.8.apk")).put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",base+"/releases/download/"+tag+"/SHA256SUMS.txt")));
            UpdatePreferences.saveSuccess(c,new JSONArray().put(fixture).toString(),"");
            activity=(UpdateActivity)startActivitySync(new Intent(c,UpdateActivity.class).putExtra(UpdateActivity.EXTRA_VERSION,"9.9.8").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            Field waiting=UpdateActivity.class.getDeclaredField("waitingForInstallPermission");waiting.setAccessible(true);waiting.setBoolean(activity,true);
            Bundle state=new Bundle();UpdateActivity current=activity;main(()->callActivityOnSaveInstanceState(current,state));
            if(!state.getBoolean("update_waiting_permission"))throw new AssertionError("UPDATE_PERMISSION_STATE_REQUIRED: permission wait must survive Activity recreation");
            result.putString("stream","Beta997 update restoration probe: 3 checks passed.\n");code=-1;
        }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}
        finally{if(activity!=null){UpdateActivity current=activity;main(current::finish);}SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> entry:old.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);}e.commit();}
        finish(code,result);
    }
}
