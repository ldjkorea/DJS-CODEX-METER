package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.concurrent.*;
import org.json.*;

/** Real candidate MainActivity, with owned synthetic data only; no fixture card extraction. */
public final class Beta9913HomeNativeTest extends Instrumentation {
    String phase;int checks;Throwable failure;
    void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    void main(Runnable work){runOnMainSync(()->{try{work.run();}catch(Throwable t){failure=t;}});if(failure!=null){Throwable t=failure;failure=null;throw new AssertionError(t);}}
    static String hash(String value)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format("%02x",b&255));return result.toString();}
    @Override public void onCreate(Bundle b){super.onCreate(b);phase=b.getString("phase","verify");start();}
    UsageSnapshot sample(double used,long at){return new UsageSnapshot("plus",true,false,new UsageWindow(25.25,18000,0,(at+7200000)/1000),new UsageWindow(used,604800,0,(at+3*86400000L)/1000),at);}
    int count(View node,Object tag){int n=tag.equals(node.getTag())?1:0;if(node instanceof ViewGroup){ViewGroup group=(ViewGroup)node;for(int i=0;i<group.getChildCount();i++)n+=count(group.getChildAt(i),tag);}return n;}
    View direct(View child,ViewGroup parent){while(child.getParent()!=parent){check(child.getParent() instanceof View,"Attached home hierarchy");child=(View)child.getParent();}return child;}
    void shown(MainActivity a,UsageSnapshot expected,boolean coachVisible)throws Exception{
        for(int attempt=0;attempt<160;attempt++){
            boolean[] ready={false};main(()->{View root=a.findViewById(android.R.id.content);TextView used=root.findViewWithTag("beta999:weekly-used"),remaining=root.findViewWithTag("beta998:quota:weekly:value"),quote=root.findViewWithTag("home:coach-text");
                boolean values=used!=null&&remaining!=null&&used.getText().toString().contains(UsagePrecision.used(expected.weekly,AppPreferences.usageDecimalPlaces(a)))&&remaining.getText().toString().equals(UsagePrecision.remaining(expected.weekly,AppPreferences.usageDecimalPlaces(a)));
                boolean coach=!coachVisible?quote==null:quote!=null&&quote.getLayout()!=null&&quote.getText().length()>0&&SpicyQuotes.peek(a,SubscriptionStore.accountKey(a))!=null&&quote.getText().toString().equals("“"+SpicyQuotes.peek(a,SubscriptionStore.accountKey(a)).text+"”");
                ready[0]=values&&coach;
            });
            if(ready[0]){check(true,"Latest snapshot and asynchronous coach visible together");layout(a,coachVisible);return;}Thread.sleep(50);
        }throw new AssertionError("Async home did not settle: used="+expected.weekly.usedPercent+", coach="+coachVisible);
    }
    void layout(MainActivity a,boolean coachVisible){main(()->{
        View root=a.findViewById(android.R.id.content);View summary=root.findViewWithTag("beta999:summary"),weekly=root.findViewWithTag("beta999:weekly");check(summary!=null&&weekly!=null,"Summary and weekly exist");ViewGroup parent=(ViewGroup)summary.getParent();check(parent.indexOfChild(summary)==0,"Identity/quota stays first");
        check(count(root,"beta999:summary")==1&&count(root,"beta999:weekly")==1,"No duplicate primary sections");
        check(root.findViewWithTag("beta996:tier-badge")!=null&&root.findViewWithTag("beta996:level")!=null,"Tier and level remain");
        check(root.findViewWithTag("beta998:focus")==null,"No new now-task panel");
        if(coachVisible){TextView quote=root.findViewWithTag("home:coach-text");check(quote!=null&&count(root,"home:coach-text")==1&&count(root,"beta9911:coach")==1,"One coach after callback");
            View slot=direct(quote,parent);check(parent.indexOfChild(summary)<parent.indexOfChild(slot)&&parent.indexOfChild(slot)<parent.indexOfChild(weekly),"Actual summary -> coach -> weekly order");
            check(quote.getTextAlignment()==View.TEXT_ALIGNMENT_CENTER&&(quote.getGravity()&Gravity.HORIZONTAL_GRAVITY_MASK)==Gravity.CENTER_HORIZONTAL,"Actual candidate centered quote");
            check(quote.getEllipsize()==null&&quote.getLayout().getLineEnd(quote.getLineCount()-1)==quote.length(),"Actual candidate full text");
        }else check(root.findViewWithTag("home:coach-text")==null,"OFF hides quote in actual home");
    });}
    MainActivity rotate(MainActivity old,int rotation,UsageSnapshot snapshot,String selected)throws Exception{
        ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);check(getUiAutomation().setRotation(rotation),"System rotation requested");Activity next=monitor.waitForActivityWithTimeout(4000);removeMonitor(monitor);
        MainActivity active=next instanceof MainActivity?(MainActivity)next:old;check(!active.isDestroyed(),"Active home after system rotation");shown(active,snapshot,true);
        check(SpicyQuotes.peek(active,SubscriptionStore.accountKey(active)).id.equals(selected),"Rotation retains the selected stable quote ID");return active;
    }
    MainActivity captureHome;
    void image(Context c,String name)throws Exception{
        File dir=new File(c.getExternalFilesDir(null),"home-coach-full");dir.mkdirs();
        for(int attempt=0;attempt<20;attempt++){
            try{getUiAutomation().waitForIdle(180,2000);}catch(TimeoutException busy){/* A ticker may be pending; check the actual card below. */}
            boolean[] ready={false};main(()->{TextView quote=captureHome.findViewById(android.R.id.content).findViewWithTag("home:coach-text");ready[0]=quote!=null&&quote.getLayout()!=null;});
            if(!ready[0]){Thread.sleep(100);continue;}
            CountDownLatch drawn=new CountDownLatch(1);main(()->{View decor=captureHome.getWindow().getDecorView();decor.getViewTreeObserver().registerFrameCommitCallback(drawn::countDown);decor.invalidate();});check(drawn.await(3,TimeUnit.SECONDS),"Real home frame committed before capture");
            File shot=new File(dir,name+".png");try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("screencap -p"));FileOutputStream stream=new FileOutputStream(shot)){input.transferTo(stream);}
            main(()->ready[0]=captureHome.findViewById(android.R.id.content).findViewWithTag("home:coach-text")!=null);
            if(!ready[0])continue;
            Bitmap bitmap=android.graphics.BitmapFactory.decodeFile(shot.getAbsolutePath());check(bitmap!=null,"Actual system full screen captured");bitmap.recycle();return;
        }throw new AssertionError("Coach never stayed visible for full-screen capture");
    }
    void voiceCache(Context c,String account)throws Exception{
        // Instrumentation's target Context can keep its pre-switch locale after Activity recreation.
        Configuration korean=new Configuration(c.getResources().getConfiguration());korean.setLocales(LocaleList.forLanguageTags("ko"));c=c.createConfigurationContext(korean);
        long at=System.currentTimeMillis();UsageSnapshot zero=new UsageSnapshot("plus",true,false,new UsageWindow(0,18000,0,(at+7200000)/1000),new UsageWindow(0,604800,0,(at+3*86400000L)/1000),at);
        AppPreferences.saveSnapshot(c,zero);AppPreferences.setLastError(c,"");String identity=SpicyQuotes.identity(c,account);
        SpicyQuotes.select(c,account,zero,true,false,0,at);SpicyQuotes.Selection first=SpicyQuotes.peek(c,account);check(first!=null,"Actual catalog zero/medal selection available");
        UsageSnapshot next=new UsageSnapshot("plus",true,false,new UsageWindow(0,18000,0,(at+7200000)/1000),new UsageWindow(0,604800,0,(at+3*86400000L)/1000),at+1);AppPreferences.saveSnapshot(c,next);Thread.sleep(5);
        check(SpicyQuotes.peek(c,account)==null,"New evidence invalidates rendered selection");SpicyQuotes.select(c,account,next,true,false,0,System.currentTimeMillis());
        check(first.id.equals(SpicyQuotes.peek(c,account).id),"Same situation refresh retains quote ID/cooldown");
        UsageSnapshot other=new UsageSnapshot("plus",true,false,new UsageWindow(0.1,18000,0,(at+7200000)/1000),zero.weekly,at);AppPreferences.saveSnapshot(c,other);
        check(!identity.equals(SpicyQuotes.identity(c,account))&&SpicyQuotes.peek(c,account)==null,"Same capture time with changed short precision invalidates cache");
        SpicyQuotes.select(c,account,other,true,false,0,System.currentTimeMillis());check(!SpicyQuotes.peek(c,account).group.equals("zero"),"Actual selector never claims zero with positive short activity");
        AppPreferences.setLastError(c,"SYNTHETIC_OFFLINE");check(SpicyQuotes.peek(c,account)==null,"Error invalidates previously visible active quote");SpicyQuotes.select(c,account,other,true,false,0,System.currentTimeMillis());
        check(SpicyQuotes.peek(c,account).group.equals("network"),"Actual selector uses unknown/error voice, no medal override");AppPreferences.setLastError(c,"");
        UsageSnapshot stale=new UsageSnapshot("plus",true,false,zero.fiveHour,zero.weekly,at-900001);AppPreferences.saveSnapshot(c,stale);SpicyQuotes.select(c,account,stale,false,false,0,System.currentTimeMillis());check(SpicyQuotes.peek(c,account).group.equals("stale"),"Actual selector stale voice");
        UsageSnapshot free=new UsageSnapshot("free",true,false,null,null,new UsageWindow(0,2592000,0,(at+15*86400000L)/1000),java.util.Collections.emptyList(),null,-1,at);AppPreferences.saveSnapshot(c,free);check(SpicyQuotes.peek(c,account)==null,"Plan/window switch invalidates paid text");
        SpicyQuotes.select(c,account,free,true,false,0,System.currentTimeMillis());SpicyQuotes.Selection selection=SpicyQuotes.peek(c,account);check(selection!=null&&!selection.text.contains("구독료")&&!selection.text.contains("구독은")&&!selection.text.contains("월급 주고"),"Actual Free catalog selection never invents payment: "+(selection==null?"no selection":selection.group+" / "+selection.text));
        String selectedId=selection.id;check(SpicyQuotes.select(c,account,zero,true,false,0,System.currentTimeMillis())==null,"Obsolete Plus worker rejected after Free snapshot committed");
        check(SpicyQuotes.peek(c,account)!=null&&selectedId.equals(SpicyQuotes.peek(c,account).id),"Obsolete worker cannot replace the current Free selection");
    }
    @Override public void onStart(){Context c=getTargetContext();Bundle result=new Bundle();int code=0;MainActivity home=null;UsageSnapshot original=null;String oldError=null,oldPreset=null,oldTone=null,oldCoach=null;LocaleList oldLocales=null;boolean oldUpdates=false,oldNotify=false;String oldNotified=null;try{
        check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&"gpt-hud-trial-demo".equals(SecureTokenStore.load(c).accountId),"Owned synthetic account only");
        PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);check(info.versionName.equals(phase.equals("before")?"9.9.12":"9.9.13")&&info.getLongVersionCode()==(phase.equals("before")?990012:990013),"Expected baseline/candidate identity");
        SharedPreferences checkpoint=c.getSharedPreferences("home_coach_full_qa",0);String account=SubscriptionStore.accountKey(c);JSONObject state=BetaGrowthStore.load(c,account);
        if(phase.equals("before")){
            check(!checkpoint.contains("uid"),"Fresh checkpoint; do not overwrite previous baseline");
            check(checkpoint.edit().putInt("uid",c.getApplicationInfo().uid).putLong("first_install",info.firstInstallTime).putLong("before_update",info.lastUpdateTime).putString("auth_hash",hash(SecureTokenStore.load(c).toJson().toString())).putString("state",state.toString()).putString("archive",BetaGrowthStore.archive(c,account).toString()).putString("name",BetaIdentity.name(c)).commit(),"Baseline checkpoint saved");
            result.putString("stream","Full home baseline: "+checks+" checks passed; synthetic state captured before local APK replacement.\n");code=-1;
        }else{
            check(phase.equals("verify")&&checkpoint.contains("uid"),"Candidate requires existing synthetic baseline");
            check(c.getApplicationInfo().uid==checkpoint.getInt("uid",-1)&&info.firstInstallTime==checkpoint.getLong("first_install",-1)&&info.lastUpdateTime>checkpoint.getLong("before_update",Long.MAX_VALUE),"Same app/data with actual APK replacement");
            check(hash(SecureTokenStore.load(c).toJson().toString()).equals(checkpoint.getString("auth_hash","")),"Encrypted synthetic auth preserved by replacement");
            JSONObject before=new JSONObject(checkpoint.getString("state",""));for(String key:new String[]{"total","level","progress","honors","titles","equipped","enabled","rewards"})check(before.get(key).toString().equals(state.get(key).toString()),"Replacement retains "+key);
            check(BetaGrowthStore.archive(c,account).toString().equals(checkpoint.getString("archive",""))&&BetaIdentity.name(c).equals(checkpoint.getString("name","")),"Archive/name preserved");
            original=AppPreferences.loadSnapshot(c);oldError=AppPreferences.getLastError(c);oldPreset=BetaPreferences.preset(c);oldTone=FunStore.tone(c);oldUpdates=UpdatePreferences.automaticChecks(c);oldNotify=UpdatePreferences.notifyUpdatesEnabled(c);oldNotified=UpdatePreferences.notifiedVersion(c);oldLocales=c.getSystemService(android.app.LocaleManager.class).getApplicationLocales();
            oldCoach=c.getSharedPreferences("coach_v2",0).getString(account,"");JSONObject fixture=new JSONObject(oldCoach);fixture.put("hidden",new JSONArray().put("ko.quote.3323ab4c6e324eda"));c.getSharedPreferences("coach_v2",0).edit().putString(account,fixture.toString()).commit();
            check(!AppPreferences.getAutomaticRefresh(c)&&!AppPreferences.getRefreshOnLaunch(c),"Synthetic fixture never requests automatic account refresh");
            c.getSystemService(android.app.LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags("ko"));FunStore.tone(c,"spicy");UpdatePreferences.setAutomaticChecks(c,false);AppPreferences.setLastError(c,"");long xp=state.getLong("total"),now=System.currentTimeMillis();UsageSnapshot first=sample(66.75,now);AppPreferences.saveSnapshot(c,first);
            home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));shown(home,first,true);captureHome=home;check(home.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko"),"Actual Korean candidate UI");image(c,"portrait-initial");
            for(String preset:new String[]{"simple","growth","detail"}){BetaPreferences.prefs(c).edit().putString("preset",preset).commit();MainActivity active=home;main(active::rebuild);shown(active,first,true);}
            ExecutorService worker=Executors.newSingleThreadExecutor();UsageSnapshot latest=first;try{
                int index=1;for(double used:new double[]{47.25,0,100,66.75}){UsageSnapshot next=sample(used,now+index++);check(worker.submit(()->AppPreferences.saveSnapshot(c,next)).get(),"Background commit without broadcast");shown(home,next,true);latest=next;}
                for(double used:new double[]{89.25,12.5,47.25}){UsageSnapshot next=sample(used,now+index++);worker.submit(()->AppPreferences.saveSnapshot(c,next)).get();latest=next;}shown(home,latest,true);
            }finally{worker.shutdownNow();}
            image(c,"portrait-after-background");String selected=SpicyQuotes.peek(home,account).id;
            home=rotate(home,UiAutomation.ROTATION_FREEZE_90,latest,selected);captureHome=home;check(home.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE,"Actual landscape configuration");image(c,"landscape");
            home=rotate(home,UiAutomation.ROTATION_FREEZE_0,latest,selected);captureHome=home;check(home.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT,"Actual portrait configuration");image(c,"portrait-after-rotation");
            AppPreferences.setLastError(c,"SYNTHETIC_OFFLINE");MainActivity active=home;main(active::rebuild);shown(active,latest,true);main(()->{TextView status=active.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:status");check(status.getText().toString().equals(active.getString(R.string.phase_a_cached_failed)),"Offline status remains explicit");});
            check(SpicyQuotes.peek(c,account).group.equals("network"),"Displayed error quote matches current evidence");
            FunStore.tone(c,"off");main(active::rebuild);shown(active,latest,false);FunStore.tone(c,"spicy");AppPreferences.setLastError(c,"");main(active::rebuild);shown(active,latest,true);
            AppPreferences.saveSnapshot(c,new UsageSnapshot("free",true,false,null,null,now+99));main(active::rebuild);main(()->{TextView remaining=active.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:value");check(remaining.getText().toString().equals("—"),"Missing limit remains unknown");});
            main(active::finish);waitForIdleSync();Thread.sleep(300);home=null;voiceCache(c,account);
            check(BetaGrowthStore.load(c,account).getLong("total")==xp,"Home interactions grant no XP");check(hash(SecureTokenStore.load(c).toJson().toString()).equals(checkpoint.getString("auth_hash","")),"Home interactions preserve auth");
            result.putString("stream","Full candidate home: "+checks+" checks passed; real MainActivity async commits/rapid latest snapshot/all presets/system rotations/quote identity/OFF/offline/missing/data continuity; synthetic only.\n");code=-1;
        }
    }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}finally{try{
        if(home!=null){MainActivity active=home;main(active::finish);}
        if(original!=null)AppPreferences.saveSnapshot(c,original);if(oldError!=null)AppPreferences.setLastError(c,oldError);if(oldPreset!=null)BetaPreferences.prefs(c).edit().putString("preset",oldPreset).commit();if(oldTone!=null)FunStore.tone(c,oldTone);
        if(oldCoach!=null)c.getSharedPreferences("coach_v2",0).edit().putString(SubscriptionStore.accountKey(c),oldCoach).commit();
        if(oldLocales!=null){c.getSystemService(android.app.LocaleManager.class).setApplicationLocales(oldLocales);UpdatePreferences.setAutomaticChecks(c,oldUpdates);UpdatePreferences.setNotifyUpdatesEnabled(c,oldNotify);UpdatePreferences.setNotifiedVersion(c,oldNotified);}getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);
    }catch(Throwable cleanup){code=0;result.putString("stream",result.getString("stream","")+android.util.Log.getStackTraceString(cleanup));}}finish(code,result);}
}
