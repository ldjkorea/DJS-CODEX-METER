package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;
/** Owned process-death fixture; no real account, browser or token exchange. */
public final class Beta997AuthProcessNativeTest extends Instrumentation {
    String phase;
    @Override public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase");start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();try{
        if(!Build.MODEL.contains("sdk")||!c.getPackageName().equals("dev.bennett.codexmeter.trial"))throw new AssertionError("Owned beta emulator only");
        SharedPreferences qa=c.getSharedPreferences("beta997_auth_process_qa",0);
        if("seed".equals(phase)){
            if(!TrialDemo.active(c))throw new AssertionError("Synthetic demo required before auth seed");
            qa.edit().putBoolean("seeded",true).putBoolean("automatic",AppPreferences.getAutomaticRefresh(c)).putBoolean("launch",AppPreferences.getRefreshOnLaunch(c)).commit();
            TrialDemo.leaveDemo(c);AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);
            c.startForegroundService(new Intent(c,OAuthService.class).setAction(OAuthService.ACTION_START));
            for(int i=0;i<70&&AppPreferences.getOAuthUrl(c).isEmpty();i++)Thread.sleep(100);
            if(!AppPreferences.isOAuthPending(c)||AppPreferences.getOAuthUrl(c).isEmpty()||!AccountSession.signInActive())throw new AssertionError("Actual service must create pending state before kill");
            Thread.sleep(150);result.putString("stream","Beta997 interrupted auth seed: 3 checks passed.\n");
        }else{
            if(!qa.getBoolean("seeded",false)||SecureTokenStore.load(c)!=null)throw new AssertionError("Owned signed-out checkpoint required");
            if(AppPreferences.isOAuthPending(c)||!AppPreferences.getOAuthUrl(c).isEmpty()||AccountSession.signInActive())throw new AssertionError("Cold foreground must clear dead PKCE pending state");
            TrialDemo.prepare(c);AppPreferences.setAutomaticRefresh(c,qa.getBoolean("automatic",false));AppPreferences.setRefreshOnLaunch(c,qa.getBoolean("launch",false));qa.edit().remove("seeded").commit();
            result.putString("stream","Beta997 interrupted auth recovery: 4 checks passed; retry ready, no credentials invented.\n");
        }code=-1;
    }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}finish(code,result);}
}
