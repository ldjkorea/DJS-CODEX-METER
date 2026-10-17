package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Regression for the primary home quota surface; owned synthetic AVD only. */
public final class Beta999HomeNativeTest extends Instrumentation {
    int checks; Throwable failure; String locale, theme; Dialog staleDialog;
    void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    void main(Runnable work) { runOnMainSync(() -> { try { work.run(); } catch (Throwable e) { failure=e; } });
        if (failure!=null) { Throwable e=failure; failure=null; throw new AssertionError(e); } }
    TextView label(View root, String tag) { return root.findViewWithTag("beta998:quota:"+tag); }
    void bounds(View v) {
        if(v.getVisibility()!=View.VISIBLE)return;
        if(v instanceof TextView) { TextView t=(TextView)v; android.text.Layout l=t.getLayout();
            if(l!=null&&t.length()>0) { check(l.getEllipsisCount(l.getLineCount()-1)==0,"No ellipsis");
                check(t.getHeight()+1>=t.getCompoundPaddingTop()+t.getCompoundPaddingBottom()+l.getLineBottom(l.getLineCount()-1),"No clipped quota text: "+t.getText()); } }
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)bounds(((ViewGroup)v).getChildAt(i));
    }
    void capture(Context c) throws Exception {
        java.io.File dir=new java.io.File(c.getExternalFilesDir(null),"beta999-home");dir.mkdirs();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,locale+"-"+theme+".png"))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
        }
    }
    void verifyReader(Context c,UsageSnapshot snapshot)throws Exception{
        java.io.File file=new java.io.File(c.getCacheDir(),"beta999-reader-fixture.db");check(!file.exists(),"Unique synthetic reader fixture");
        Context wrapper=new ContextWrapper(c){@Override public java.io.File getDatabasePath(String name){return file;}};
        try{
            try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null)){
                db.execSQL(UsageLedgerDatabase.CREATE_OBSERVATIONS);
                long end=snapshot.weekly.resetAtMillis();long at=LedgerPeriods.span(end,604800,snapshot.fetchedAtMillis).start+3600000;
                for(int i=0;i<2;i++){android.content.ContentValues values=new android.content.ContentValues();values.put("meter","weekly");values.put("plan","plus");values.put("policy","weekly|plus|604800");values.put("observed_at",at+i*60000);values.put("used",10+i*2.5);values.put("used_decimal",Double.toString(10+i*2.5));values.put("reset_at",end);values.put("window_seconds",604800);values.put("source","api_precise");values.put("manual",0);db.insertOrThrow("observations",null,values);}
            }
            long size=file.length();BetaWeeklyUsage.Model model=BetaWeeklyReader.read(wrapper,snapshot,System.currentTimeMillis());
            check(model.recordedDays==1&&model.measuredDays==1,"Actual SQLite read-only daily projection");check(model.days.stream().anyMatch(d->d.measured&&d.points==2.5),"SQLite decimal preserved");check(file.length()==size,"Reader did not migrate/write database");
            final boolean[] rejected={false};main(()->{try{BetaWeeklyReader.read(wrapper,snapshot,System.currentTimeMillis());}catch(IllegalStateException expected){rejected[0]=true;}catch(Exception e){throw new AssertionError(e);}});check(rejected[0],"Database reader rejects home main thread");
            String stamp=BetaWeeklyUi.stamp(c,snapshot);UsageSnapshot changed=new UsageSnapshot("pro",true,false,snapshot.fiveHour,snapshot.weekly,snapshot.fetchedAtMillis+1);check(!stamp.equals(BetaWeeklyUi.stamp(c,changed)),"Changed policy/snapshot fences late result");
        }finally{check(android.database.sqlite.SQLiteDatabase.deleteDatabase(file),"Only own synthetic fixture cleaned");}
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args);locale=args.getString("locale","ko");theme=args.getString("theme","dark");start(); }
    @Override public void onStart() {
        Bundle result=new Bundle(); int code=0; Context c=getTargetContext(); MainActivity home=null;
        SharedPreferences settings=c.getSharedPreferences("codex_meter_settings_v1",0);
        String oldSnapshot=settings.getString("last_snapshot",null), oldError=AppPreferences.getLastError(c);
        String oldPreset=BetaPreferences.preset(c), oldTheme=AppPreferences.getAppTheme(c);
        android.app.LocaleManager locales=c.getSystemService(android.app.LocaleManager.class);
        String oldLocale=locales.getApplicationLocales().toLanguageTags();
        try {
            check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic sandbox");
            String account=SubscriptionStore.accountKey(c), growth=BetaGrowthStore.load(c,account).toString(), tokens=SecureTokenStore.load(c).toJson().toString();
            AppPreferences.setAppTheme(c,theme);locales.setApplicationLocales(LocaleList.forLanguageTags(locale));Thread.sleep(300);
            long now=System.currentTimeMillis();
            UsageWindow five=new UsageWindow(25.25,18000,0,(now+7200000)/1000), week=new UsageWindow(66.75,604800,0,(now+3*86400000L)/1000);
            UsageSnapshot sample=new UsageSnapshot("plus",true,false,five,week,now);
            check(AppPreferences.saveSnapshot(c,sample),"Synthetic cached quota persisted");
            home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Thread.sleep(1200);BetaGrowthStore.WORKER.submit(()->{}).get();waitForIdleSync();
            MainActivity a=home;
            for(String preset:new String[]{"simple","growth","detail"}) {
                BetaPreferences.prefs(c).edit().putString("preset",preset).commit();
                main(a::rebuild);waitForIdleSync();Thread.sleep(120);
                main(()->{
                    View root=a.findViewById(android.R.id.content), quota=root.findViewWithTag("beta998:quota"), crest=root.findViewWithTag("beta996:identity");
                    check(quota!=null&&crest!=null,"Quota and tier identity retained in "+preset);
                    ViewGroup summary=root.findViewWithTag("beta999:summary");View weekly=root.findViewWithTag("beta999:weekly");
                    check(summary!=null&&weekly!=null,"Combined box and weekly box in "+preset);
                    check(quota.getParent()==summary&&crest.getParent().getParent()==summary,"Quota and identity share one surface");
                    check(crest.getBackground()==null,"No nested profile card");
                    ViewGroup parent=(ViewGroup)summary.getParent();check(parent.indexOfChild(summary)==0,"Combined box first");
                    check(parent.indexOfChild(weekly)==parent.indexOfChild(summary)+2,"Only spacing between summary and weekly");
                    check(root.findViewWithTag("beta998:focus")==null,"No home focus/now task");
                    check(root.findViewWithTag("beta998:goal-link")==null,"No redundant home goal link");
                    check(root.findViewWithTag("beta999:weekly-used")!=null,"Server weekly used value");
                    check(((TextView)root.findViewWithTag("beta999:weekly-used")).getText().toString().contains(UsagePrecision.used(week,AppPreferences.usageDecimalPlaces(c))),"Used and remaining are distinct; actual="+((TextView)root.findViewWithTag("beta999:weekly-used")).getText()+" expected="+UsagePrecision.used(week,AppPreferences.usageDecimalPlaces(c))+" stored="+AppPreferences.loadSnapshot(c).weekly.preciseUsedPercent);
                    check(crest.findViewWithTag("beta996:tier-badge")!=null&&crest.findViewWithTag("beta996:level")!=null&&crest.findViewWithTag("beta996:title")!=null,"Badge level title preserved");
                    bounds(summary);bounds(weekly);
                    check(label(root,"five-hour:value").getText().toString().equals(UsagePrecision.remaining(five,AppPreferences.usageDecimalPlaces(c))),"5-hour remaining, not used");
                    check(label(root,"weekly:value").getText().toString().equals(UsagePrecision.remaining(week,AppPreferences.usageDecimalPlaces(c))),"Weekly remaining, not used");
                    check(label(root,"five-hour:reset").length()>0&&label(root,"weekly:reset").length()>0,"Both reset countdowns visible");
                    if(a.getResources().getConfiguration().fontScale<=1.3f&&a.getResources().getConfiguration().orientation==1) {
                        int[] position=new int[2];label(root,"weekly:value").getLocationOnScreen(position);
                        View scroll=a.findViewById(R.id.dashboard_scroll);android.graphics.Rect viewport=new android.graphics.Rect();scroll.getGlobalVisibleRect(viewport);
                        check(position[1]+label(root,"weekly:value").getHeight()<=viewport.bottom,"Both values in initial viewport");
                    }
                    bounds(quota);
                });
            }
            BetaWeeklyUi.WORKER.submit(()->{}).get();waitForIdleSync();Thread.sleep(250);
            main(()->{View root=a.findViewById(android.R.id.content);check(root.findViewWithTag("beta999:weekly-missing")!=null,"Insufficient daily records explicitly marked");});
            verifyReader(c,sample);
            java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
            BetaWeeklyUi.WORKER.execute(()->{entered.countDown();try{release.await(10,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});check(entered.await(5,java.util.concurrent.TimeUnit.SECONDS),"Controlled slow worker");
            View[] oldGraph={null};
            try{
                UsageSnapshot older=new UsageSnapshot("plus",true,false,five,week,now-1000);
                AppPreferences.saveSnapshot(c,older);main(a::rebuild);
                main(()->{
                    LinearLayout host=new LinearLayout(a);host.setOrientation(LinearLayout.VERTICAL);
                    staleDialog=new Dialog(a);staleDialog.setContentView(host);
                    BetaWeeklyUi.add(a,host,older,Ui.isDark(a), new java.util.ArrayList<>());
                    staleDialog.show();oldGraph[0]=host.findViewWithTag("beta999:weekly-graph");
                });
                waitForIdleSync();Thread.sleep(100);main(()->check(oldGraph[0].isAttachedToWindow(),"Controlled older graph attached independently of home rebuild"));
                AppPreferences.saveSnapshot(c,sample);
            }finally{release.countDown();}
            BetaWeeklyUi.WORKER.submit(()->{}).get();waitForIdleSync();
            main(()->{try{check(oldGraph[0].isAttachedToWindow(),"Older graph still attached after snapshot observer redraw");check(oldGraph[0].findViewWithTag("beta999:weekly-missing")==null,"Late result for replaced snapshot is discarded even while old graph attached");}finally{staleDialog.dismiss();staleDialog=null;}});
            main(a::rebuild);BetaWeeklyUi.WORKER.submit(()->{}).get();waitForIdleSync();Thread.sleep(150);
            java.util.List<LedgerRecord> graphRows=java.util.List.of(new LedgerRecord("weekly","plus",now-900000,65,"65",week.resetAtMillis(),604800,"api_precise",false),new LedgerRecord("weekly","plus",now,66.75,"66.75",week.resetAtMillis(),604800,"api_precise",false));
            main(()->{LinearLayout graph=a.findViewById(android.R.id.content).findViewWithTag("beta999:weekly-graph");BetaWeeklyUi.render(a,graph,BetaWeeklyUsage.project(sample,graphRows,now),Ui.isDark(a));});waitForIdleSync();Thread.sleep(200);
            main(()->bounds(a.findViewById(android.R.id.content).findViewWithTag("beta999:weekly")));
            capture(c);
            AppPreferences.setLastError(c,"SYNTHETIC_OFFLINE");main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"five-hour:status").getText().toString().equals(a.getString(R.string.phase_a_cached_failed)),"Cached failure marked");check(label(root,"weekly:value").getText().toString().equals(UsagePrecision.remaining(week,0)),"Failure retains last observed value");});
            UsageSnapshot missing=new UsageSnapshot("free",true,false,null,null,now);AppPreferences.saveSnapshot(c,missing);main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"five-hour:value").getText().toString().equals("—")&&label(root,"weekly:value").getText().toString().equals("—"),"Unprovided windows never invented as 0 or 100");});
            settings.edit().remove("last_snapshot").commit();main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"five-hour:status").length()>0&&label(root,"weekly:value").getText().toString().equals("—"),"No snapshot still has both labelled placeholders");});
            AppPreferences.saveSnapshot(c,new UsageSnapshot("plus",true,false,five,week,now-86400000));main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"weekly:status").getText().toString().equals(a.getString(R.string.phase_a_stale)),"Stale observation clearly marked");});
            UsageSnapshot expired=new UsageSnapshot("plus",true,false,new UsageWindow(25,18000,0,(now-60000)/1000),new UsageWindow(67,604800,0,(now-60000)/1000),now);
            AppPreferences.saveSnapshot(c,expired);main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"weekly:reset").getText().toString().equals(a.getString(R.string.phase_a_reset_passed)),"Expired weekly is not reset to full");});
            check(BetaGrowthStore.load(c,account).toString().equals(growth),"Rendering never changes growth evidence or rewards");
            check(SecureTokenStore.load(c).toJson().toString().equals(tokens),"Rendering preserves authentication");
            result.putString("stream","Beta999 combined home: "+checks+" checks passed; "+locale+" / "+theme+" / font "+c.getResources().getConfiguration().fontScale+".\n");code=-1;
        } catch(Throwable e) {result.putString("stream",android.util.Log.getStackTraceString(e));}
        finally {
            if(staleDialog!=null)main(()->{staleDialog.dismiss();staleDialog=null;});
            if(home!=null){MainActivity a=home;main(a::finish);}
            SharedPreferences.Editor edit=settings.edit();if(oldSnapshot==null)edit.remove("last_snapshot");else edit.putString("last_snapshot",oldSnapshot);edit.commit();
            AppPreferences.setLastError(c,oldError);BetaPreferences.prefs(c).edit().putString("preset",oldPreset).commit();
            AppPreferences.setAppTheme(c,oldTheme);locales.setApplicationLocales(LocaleList.forLanguageTags(oldLocale));
        }
        finish(code,result);
    }
}
