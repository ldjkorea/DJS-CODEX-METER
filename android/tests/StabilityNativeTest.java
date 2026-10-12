package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Real Activity race with a blocked worker, on an owned synthetic emulator account. */
public final class StabilityNativeTest extends Instrumentation {
    int checks; Throwable error; Activity active;
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable e){error=e;}});if(error!=null)throw new AssertionError(error);}
    static Object field(Object a,String key)throws Exception{Field f=GrowthActivity.class.getDeclaredField(key);f.setAccessible(true);return f.get(a);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();CountDownLatch release=new CountDownLatch(1);try{
        Context c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa")&&Build.MODEL.contains("sdk"),"Isolated emulator only");
        AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);UpdatePreferences.setAutomaticChecks(c,false);
        SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_STABILITY","SYNTHETIC_REFRESH","",System.currentTimeMillis()+86400000,"stability-"+java.util.UUID.randomUUID(),""));
        Field f=GrowthActivity.class.getDeclaredField("WORKER");f.setAccessible(true);ExecutorService worker=(ExecutorService)f.get(null);
        CountDownLatch entered=new CountDownLatch(1);worker.execute(()->{entered.countDown();try{release.await(15,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        check(entered.await(3,TimeUnit.SECONDS),"Deterministically block growth worker");
        active=startActivitySync(new Intent(c,GrowthActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));GrowthActivity a=(GrowthActivity)active;
        main(()->{try{check((Boolean)field(a,"busy"),"Old account request pending");LinearLayout content=(LinearLayout)field(a,"content");TextView privateText=new TextView(a);privateText.setText("OLD_ACCOUNT_PRIVATE_PROFILE");content.addView(privateText);
            SecureTokenStore.clear(c);a.load(false);check(content.indexOfChild(privateText)==-1,"Logout immediately removes old profile before worker finishes");check(((String)field(a,"loadedAccount")).isEmpty(),"Old action identity cleared");
            SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_B","SYNTHETIC_REFRESH_B","",System.currentTimeMillis()+86400000,"stability-b-"+java.util.UUID.randomUUID(),""));a.load(false);check((Boolean)field(a,"busy"),"New account queued independently");
        }catch(Exception e){throw new RuntimeException(e);}});
        release.countDown();Thread.sleep(500);waitForIdleSync();main(()->{try{check(SubscriptionStore.accountKey(c).equals(field(a,"loadedAccount")),"Stale callback cannot overwrite new account");}catch(Exception e){throw new RuntimeException(e);}});
        result.putString("stream","Stability Activity isolation: "+checks+" checks passed\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}finally{release.countDown();if(active!=null)main(active::finish);}}
}
