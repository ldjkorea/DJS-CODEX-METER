package dev.bennett.codexmeter;

import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Real platform/render/store code; only the existing isolated emulator QA application. */
public final class PhaseCNativeTest extends Instrumentation {
    int checks;Context c;Throwable error;StringBuilder report=new StringBuilder();
    void check(boolean value,String note){checks++;if(!value)throw new AssertionError(note);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable e){error=e;}});if(error!=null)throw new AssertionError(error);}
    void restore(SharedPreferences p,Map<String,?> previous){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> x:previous.entrySet()){Object v=x.getValue();String k=x.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);}check(e.commit(),"Restore isolated preferences");}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;SharedPreferences auth=null,settings=null;Map<String,?> originalAuth=null,originalSettings=null;try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa")&&Build.MODEL.contains("sdk"),"Owned QA emulator only");
        auth=c.getSharedPreferences("secure_auth_v1",0);settings=c.getSharedPreferences("codex_meter_settings_v1",0);originalAuth=new HashMap<>(auth.getAll());originalSettings=new HashMap<>(settings.getAll());long now=System.currentTimeMillis();
        SecureTokenStore.save(c,new AuthTokens("PHASE_C_SYNTHETIC","PHASE_C_REFRESH","",now+86400000,"phase-c-"+UUID.randomUUID(),""));String blob=auth.getString("blob","");
        auth.edit().putString("blob","{corrupted QA ciphertext}").commit();check(SecureTokenStore.load(c)==null,"Invalid ciphertext does not authenticate");check(auth.getString("blob","").equals("{corrupted QA ciphertext}"),"Read failure never deletes encrypted credentials");auth.edit().putString("blob",blob).commit();check(SecureTokenStore.load(c)!=null,"Original ciphertext remains decryptable");
        String account=SubscriptionStore.accountKey(c);check(account!=null,"Synthetic stable account hash");
        boolean blocked=false;try{SettingsTransferStore.collect(c,true,false,false,true);}catch(IllegalArgumentException e){blocked=true;}check(blocked,"General settings export rejects credentials");
        SettingsTransfer.Document ordinary=SettingsTransferStore.collect(c,true,true,true,false);check(!ordinary.hasAuthentication(),"Ordinary settings export excludes authentication");
        File preparedFile=new File(c.getCacheDir(),"phase-c-test-"+UUID.randomUUID()+".apk");try{try(FileOutputStream out=new FileOutputStream(preparedFile)){out.write(new byte[]{1,2,3});}UpdateInstaller.PreparedUpdate prepared=new UpdateInstaller.PreparedUpdate(preparedFile,"2.8.50",100);try(FileOutputStream out=new FileOutputStream(preparedFile)){out.write(new byte[]{1,2,4});}
            boolean rejected=false;try{UpdateInstaller.commit(c,prepared);}catch(SecurityException e){rejected=e.getMessage().contains("changed before");}check(rejected,"Actual commit rejects modified prepared APK before installer session");
        }finally{preparedFile.delete();}
        java.lang.reflect.Method verify=UpdateInstaller.class.getDeclaredMethod("verifyPackage",Context.class,String.class,File.class);verify.setAccessible(true);boolean downgrade=false;try{verify.invoke(null,c,c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName,new File(c.getApplicationInfo().sourceDir));}catch(java.lang.reflect.InvocationTargetException e){downgrade=e.getCause() instanceof UpdateInstaller.DowngradeNotSupportedException;}check(downgrade,"Actual package manager blocks equal installed versionCode");
        RpgProgressStore.setAchievementsEnabled(c,account,true);RpgProgressStore.setTitlesEnabled(c,account,true);
        JSONObject envelope=new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",now).put("summary",new JSONObject().put("lifetimeTokens",1000000L)).put("dailyUsageBuckets",new JSONArray().put(new JSONObject().put("startDate",LedgerAggregation.day(now).toString()).put("tokens",1000000L)));
        RpgProgressStore.tokens(c,account,envelope);RpgProgressStore.setTitlesEnabled(c,account,false);
        JSONObject archive=RpgProgressStore.archive(c,account);long xp=RpgProgressStore.progress(c).getLong("xp");char[] pass="SYNTHETIC-backup-pass".toCharArray();byte[] encrypted=RpgBackupCodec.encrypt(archive,pass);JSONObject decoded=RpgBackupCodec.decrypt(encrypted,pass);
        check(!new String(encrypted,java.nio.charset.StandardCharsets.UTF_8).contains("PHASE_C_REFRESH"),"Encrypted RPG excludes token");
        File portable=new File(c.getCacheDir(),"phase-c-backup-"+UUID.randomUUID()+".gpthud");try{try(FileOutputStream output=new FileOutputStream(portable)){output.write(encrypted);}check(RpgBackupCodec.decrypt(java.nio.file.Files.readAllBytes(portable.toPath()),pass).getString("account").equals(account),"Actual encrypted file round trip");}finally{portable.delete();}
        check(RpgProgressStore.restoreArchive(c,account,decoded),"Actual Android SQLite restore");check(RpgProgressStore.restoreArchive(c,account,decoded)&&RpgProgressStore.progress(c).getLong("xp")==xp,"Repeated actual restore never adds XP");
        JSONObject after=RpgProgressStore.archive(c,account);check(after.getJSONObject("document").getJSONObject("future").getJSONObject("rpg").getJSONObject(TitleJourney.KEY).getBoolean("enabled")==false,"Title OFF survives actual restore");
        String kept=after.getJSONObject("document").toString();JSONObject bad=new JSONObject(decoded.toString()).put("account","b".repeat(64));boolean wrong=false;try{RpgProgressStore.restoreArchive(c,account,bad);}catch(Exception e){wrong=true;}check(wrong&&RpgProgressStore.archive(c,account).getJSONObject("document").toString().equals(kept),"Foreign archive rejection preserves actual DB");
        boolean wrongPass=false;try{RpgBackupCodec.decrypt(encrypted,"WRONG-password".toCharArray());}catch(Exception e){wrongPass=true;}check(wrongPass,"Incorrect backup password rejected");encrypted[encrypted.length/2]^=1;boolean damaged=false;try{RpgBackupCodec.decrypt(encrypted,pass);}catch(Exception e){damaged=true;}check(damaged,"Tampered backup rejected");Arrays.fill(pass,'\0');
        for(double used:new double[]{0,100}){AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(used,604800,0,(now+86400000)/1000),now));main(()->platformRender(now));}
        AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(42.125,604800,0,(now+86400000)/1000),now));main(()->benchmark(now));
        check(WidgetRenderCache.freshUsage(c),"Fresh quota avoids extra provider network request");AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(42.125,604800,0,(now-1000)/1000),now));check(!WidgetRenderCache.freshUsage(c),"Passed reset requires new observation");
        AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(42.125,604800,0,(now+86400000)/1000),now-86400000));
        main(()->{View stale=SamsungLockWidgetSupport.buildViews(c,SamsungLockWidgetSupport.Shape.SQUARE,SamsungLockWidgetSupport.Style.RINGS).apply(c,new FrameLayout(c));TextView text=stale.findViewById(R.id.lock_square_value);check(text!=null&&text.getText().toString().contains(c.getString(R.string.phase_c_saved_at,"").trim()),"Samsung stale graphics visibly become saved-value fallback");layout(stale,140,120);capture(stale,"samsung-stale.png");});
        AppPreferences.setShowDashboardFiveHour(c,true);AppPreferences.setShowDashboardWeekly(c,true);AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,new UsageWindow(23,18000,0,(now+3600000)/1000),new UsageWindow(42,604800,0,(now+86400000)/1000),now-86400000));
        main(()->{for(SamsungLockWidgetSupport.Metric metric:new SamsungLockWidgetSupport.Metric[]{SamsungLockWidgetSupport.Metric.FIVE_HOUR,SamsungLockWidgetSupport.Metric.WEEKLY}){View stale=SamsungLockWidgetSupport.buildViews(c,null,0,SamsungLockWidgetSupport.Shape.SQUARE,SamsungLockWidgetSupport.Style.DIALS,metric).apply(c,new FrameLayout(c));TextView text=stale.findViewById(R.id.lock_square_value);String label=c.getString(metric==SamsungLockWidgetSupport.Metric.FIVE_HOUR?R.string.ui_5h_fa0873:R.string.ui_w_e2415c);check(text!=null&&text.getText().toString().contains(label)&&text.getText().toString().contains(metric==SamsungLockWidgetSupport.Metric.FIVE_HOUR?"77%":"58%"),"Dedicated stale widget retains its own metric and remaining value");}});
        result.putString("stream","Phase C native: "+checks+" checks passed; actual Android rendering/store/crypto, synthetic account, no Samsung host/Wear device proof.\n"+report);code=-1;
    }catch(Throwable t){result.putString("stream",android.util.Log.getStackTraceString(t));}finally{try{if(originalSettings!=null)restore(settings,originalSettings);if(originalAuth!=null)restore(auth,originalAuth);}catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));code=0;}}finish(code,result);}
    void platformRender(long now){
        for(SamsungLockWidgetSupport.Shape shape:SamsungLockWidgetSupport.Shape.values())for(SamsungLockWidgetSupport.Style style:SamsungLockWidgetSupport.Style.values())for(SamsungLockWidgetSupport.Metric metric:SamsungLockWidgetSupport.Metric.values()){
            View view=SamsungLockWidgetSupport.buildViews(c,null,0,shape,style,metric).apply(c,new FrameLayout(c));check(view!=null,"Samsung synthetic RemoteViews: "+shape+style+metric);layout(view,shape==SamsungLockWidgetSupport.Shape.WIDE?240:140,120);
        }
        for(int tier:new int[]{-1,0,9}){Bundle size=new Bundle();size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,160);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,70);View v=WidgetRenderer.buildPreview(c,919191,WidgetOptions.defaults(),size,false).apply(c,new FrameLayout(c));layout(v,160,70);check(v.getWidth()>0,"2x1 preview with zero/depleted weekly only");}
    }
    void layout(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(c,width),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Ui.dp(c,height),View.MeasureSpec.EXACTLY));view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());}
    void benchmark(long now){try{
        Bundle size=new Bundle();size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,160);size.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,70);WidgetOptions options=WidgetOptions.defaults();
        JSONObject tasks=new JSONObject().put("verified",true).put("checked",now).put("rows",new JSONArray());for(int i=0;i<6;i++)tasks.getJSONArray("rows").put(new JSONObject().put("thread",String.format("%032x",i)).put("name","Synthetic task "+i).put("state","running").put("at",now).put("seen",now).put("seq",6-i));
        for(boolean task:new boolean[]{false,true}){
            String id="native-cost:"+task;WidgetRenderCache.forget(id);String signature=task?TaskStatusData.widgetFingerprint(tasks,now,0,now,true):WidgetRenderCache.usage(c,919191,size,options);long[] elapsed=new long[2];int[] renders=new int[2];long bytes=0;android.widget.RemoteViews captured=null;
            for(int phase=0;phase<2;phase++){long start=SystemClock.elapsedRealtimeNanos();for(int i=0;i<12;i++){
                String current=task?TaskStatusData.widgetFingerprint(tasks,now,0,now,true):WidgetRenderCache.usage(c,919191,size,options);
                if(phase==1&&WidgetRenderCache.unchanged(id,current))continue;
                android.widget.RemoteViews rv=task?TaskStatusWidget.render(c,320,180,4,tasks,now,true):WidgetRenderer.buildPreview(c,919191,options,size,false);
                captured=rv;
                android.os.Parcel p=android.os.Parcel.obtain();try{rv.writeToParcel(p,0);bytes=p.dataSize();}finally{p.recycle();}renders[phase]++;if(phase==1)WidgetRenderCache.delivered(id,current);
            }elapsed[phase]=SystemClock.elapsedRealtimeNanos()-start;}
            View view=captured.apply(c,new FrameLayout(c));layout(view,task?320:160,task?180:70);capture(view,task?"tasks-four.png":"usage-two-one.png");
            check(renders[0]==12&&renders[1]==1,"Unchanged render construction 12 -> 1");check(WidgetRenderCache.unchanged(id,signature),"Successful delivery marker");check(!WidgetRenderCache.unchanged(id,signature+"changed"),"Changed evidence requires redraw");WidgetRenderCache.forget(id);check(!WidgetRenderCache.unchanged(id,signature),"Host lifecycle forces redraw");
            report.append(task?"Task":"Usage2x1").append(" render construction: 12 -> 1; ").append(elapsed[0]/1000000d).append(" -> ").append(elapsed[1]/1000000d).append(" ms; single Parcel ").append(bytes).append(" bytes. Not Samsung Binder cost.\n");
        }
        String base=WidgetRenderCache.usage(c,919191,size,options);Bundle resized=new Bundle(size);resized.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,220);check(!base.equals(WidgetRenderCache.usage(c,919191,resized,options)),"Resize invalidates usage marker");AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(42.25,604800,0,(now+86400000)/1000),now));check(!base.equals(WidgetRenderCache.usage(c,919191,size,options)),"Same integer different precise data invalidates marker");
        for(int i=0;i<65;i++)WidgetRenderCache.delivered("phase-c-cap:"+i,"test");check(!WidgetRenderCache.unchanged("phase-c-cap:0","test")&&WidgetRenderCache.unchanged("phase-c-cap:64","test"),"Delivery markers bounded to 64, recently used marker retained");for(int i=0;i<65;i++)WidgetRenderCache.forget("phase-c-cap:"+i);
    }catch(Exception e){throw new RuntimeException(e);}}
    void capture(View view,String name){try{File folder=new File(c.getExternalFilesDir(null),"phase-c-fixtures");folder.mkdirs();Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));try(FileOutputStream out=new FileOutputStream(new File(folder,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}catch(Exception e){throw new RuntimeException(e);}}
}
