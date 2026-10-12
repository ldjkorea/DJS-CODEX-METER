package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Owned synthetic emulator only: actual main-loop read while quota networking is busy. */
public final class StartupLockNativeTest extends Instrumentation {
    int checks; Throwable error; Bundle args; Context c; String account;
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable t){error=t;}});if(error!=null)throw new AssertionError(error);}
    @Override public void onCreate(Bundle b){super.onCreate(b);args=b;start();}
    @Override public void onStart(){Bundle result=new Bundle();CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);Thread holder=null;try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa")&&Build.MODEL.contains("sdk"),"Owned synthetic emulator only");
        AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);UpdatePreferences.setAutomaticChecks(c,false);
        long now=System.currentTimeMillis();SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_STARTUP","SYNTHETIC_STARTUP_REFRESH","",now+86400000,"startup-"+UUID.randomUUID(),""));account=SubscriptionStore.accountKey(c);
        AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(41,604800,0,(now+86400000)/1000),now));
        JSONObject cached=new JSONObject().put("tier",new JSONObject().put("tier",4)).put("settlements",new JSONObject()).put("billing",new JSONObject()).put("rpg",new JSONObject().put("xp",12345).put("KR",new JSONObject().put("tier",4)).put("collection_revision",CollectionRestart.REVISION).put("achievement_enabled",false).put("title_enabled",false));
        SharedPreferences prefs=c.getSharedPreferences("codex_rpg_display_v1",0);prefs.edit().putString(account,cached.toString()).putString(account+":collection_revision",Integer.toString(CollectionRestart.REVISION)).commit();
        boolean probe="true".equals(args.getString("probe"));
        holder=new Thread(()->{synchronized(UsageApi.NETWORK_LOCK){held.countDown();try{release.await(probe?6:12,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}},"startup-network-fixture");holder.start();check(held.await(3,TimeUnit.SECONDS),"Network mutex deterministically held");
        StringBuilder trace=new StringBuilder();Thread sampler=new Thread(()->{try{Thread.sleep(350);for(StackTraceElement frame:Looper.getMainLooper().getThread().getStackTrace())trace.append(frame).append('\n');}catch(Exception ignored){}});sampler.start();
        long begin=SystemClock.elapsedRealtime();main(()->check(TierTheme.tier(c)==4,"Cached account tier retained"));long elapsed=SystemClock.elapsedRealtime()-begin;sampler.join(1000);
        if(probe){check(elapsed>=4500&&trace.toString().contains("RpgProgressStore.peek"),"Published path blocks actual Android main loop");result.putString("stream","Startup lock reproduction: "+checks+" checks passed; blocked "+elapsed+" ms\n"+trace);finish(-1,result);return;}
        check(elapsed<1000&&holder.isAlive(),"Tier read completes before busy networking releases");
        long launchBegin=SystemClock.elapsedRealtime();
        Activity home=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        long launchMillis=SystemClock.elapsedRealtime()-launchBegin;
        check(launchMillis<3000&&holder.isAlive(),"Actual home opens while networking still holds its mutex");
        CountDownLatch heartbeat=new CountDownLatch(1);new Handler(Looper.getMainLooper()).postDelayed(heartbeat::countDown,100);
        check(heartbeat.await(2,TimeUnit.SECONDS),"Actual home main loop remains responsive");
        java.io.File images=new java.io.File(c.getExternalFilesDir(null),"startup-fixtures");images.mkdirs();
        android.graphics.Bitmap screen=getUiAutomation().takeScreenshot();
        check(screen!=null,"Actual home screenshot captured");
        try(java.io.FileOutputStream image=new java.io.FileOutputStream(new java.io.File(images,"home-network-busy.png"))){screen.compress(android.graphics.Bitmap.CompressFormat.PNG,100,image);}screen.recycle();
        main(home::finish);
        main(()->{check(RpgProgressStore.peek(c)!=null,"Warm display cache usable under network contention");prefs.edit().remove(account).commit();check(RpgProgressStore.peek(c)==null,"Missing cache stays unknown without main-thread database/network wait");prefs.edit().putString(account,"not-json").commit();check(RpgProgressStore.peek(c)==null,"Invalid cache stays unknown without main-thread recovery IO");});
        cached.getJSONObject("rpg").put("collection_revision",0).put("unlocked_ids",new JSONArray().put("tokens_1m"));prefs.edit().putString(account,cached.toString()).commit();
        main(()->{JSONObject safe=RpgProgressStore.peek(c);check(safe!=null&&safe.optJSONObject("rpg").optJSONArray("unlocked_ids").length()==0,"Old cache hides legacy collection before worker migration");});
        check(prefs.getString(account,"").equals(cached.toString()),"Display read does not write or migrate stored data");
        // The renderer must not hold its cache monitor across a network/storage wait.
        CountDownLatch painted=new CountDownLatch(1);Thread paint=new Thread(()->{synchronized(UsageApi.NETWORK_LOCK){TierTheme.tier(c);painted.countDown();}});release.countDown();paint.start();check(painted.await(3,TimeUnit.SECONDS),"Background renderer completes without opposite lock order");paint.join(1000);
        SecureTokenStore.clear(c);main(()->check(RpgProgressStore.peek(c)==null&&TierTheme.tier(c)==-1,"Logout cannot expose previous account cache"));
        result.putString("stream","Startup lock hotfix: "+checks+" checks passed; cached main-loop read "+elapsed+" ms; actual home opened "+launchMillis+" ms with responsive main loop; synthetic emulator only\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}finally{release.countDown();if(holder!=null)try{holder.join(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}
}
