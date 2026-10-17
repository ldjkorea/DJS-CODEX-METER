package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;

/** Actual SQLite, failed preference commit, signed update and Activity resume regression. */
public final class Stable311RecoveryNativeTest extends Instrumentation {
    String expectedVersion="9.9.7",checkpoint="beta997_qa";
    int checks;
    Throwable uiFailure;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    void main(Runnable work){runOnMainSync(()->{try{work.run();}catch(Throwable t){uiFailure=t;}});if(uiFailure!=null)throw new AssertionError(uiFailure);}
    static String text(View view){StringBuilder out=new StringBuilder();if(view instanceof TextView)out.append(((TextView)view).getText());if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)out.append(text(((ViewGroup)view).getChildAt(i)));return out.toString();}
    void awaitWorker()throws Exception{BetaGrowthStore.WORKER.submit(()->{}).get(15,TimeUnit.SECONDS);waitForIdleSync();}
    void waitText(Activity activity,String expected)throws Exception{for(int i=0;i<60;i++){final boolean[] found={false};main(()->found[0]=text(activity.findViewById(android.R.id.content)).contains(expected));if(found[0])return;Thread.sleep(100);}throw new AssertionError("UI did not update: "+expected);}
    Context failedCommit(Context base){return new ContextWrapper(base){@Override public SharedPreferences getSharedPreferences(String name,int mode){SharedPreferences prefs=super.getSharedPreferences(name,mode);if(!name.equals("beta_growth_display_v4"))return prefs;
        return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)->{
            if(!method.getName().equals("edit"))return method.invoke(prefs,args);
            SharedPreferences.Editor editor=prefs.edit();
            return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(self,operation,values)->{
                if(operation.getName().equals("commit"))return false;
                Object result=operation.invoke(editor,values);return result instanceof SharedPreferences.Editor?self:result;
            });
        });
    }};}
    @Override public void onCreate(Bundle args){super.onCreate(args);expectedVersion=args.getString("expected_version","9.9.7");checkpoint=args.getString("checkpoint","beta997_qa");start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();String account=null;JSONObject original=null;String originalName=null;Activity growth=null,other=null,home=null;
        try{
            check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter")&&"stable311-qa".equals(SecureTokenStore.load(c).accountId),"Owned synthetic stable only");
            check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals(expectedVersion),"Installed signed "+expectedVersion);
            account=SubscriptionStore.accountKey(c);original=BetaGrowthStore.load(c,account);originalName=BetaIdentity.name(c);
            SharedPreferences qa=c.getSharedPreferences(checkpoint,0);JSONObject before=new JSONObject(qa.getString("state",""));
            for(String key:new String[]{"rules","namespace","account","cutover","total","level","progress","next_cost","last_response","days","weeks","months","rewards","honors","titles","equipped","enabled","catalog_revision"})check(before.get(key).toString().equals(original.get(key).toString()),"9.9.6 update preserves "+key);
            check(original.optLong("seen_total")>=before.optLong("seen_total")&&original.optLong("seen_total")<=original.getLong("total"),"Viewing may acknowledge rewards without changing their balance");
            check(BetaGrowthStore.archive(c,account).toString().equals(qa.getString("archive","")),"Read-only previous archive retained");
            check(originalName.equals(qa.getString("name","")),"Name retained across signed update");
            SharedPreferences display=c.getSharedPreferences("beta_growth_display_v4",0);long xp=original.getLong("total");
            for(String bad:new String[]{"", "{", "{}", "{\"account\":\"another-account\"}",new JSONObject(BetaGrowthStore.peek(c).toString()).put("total",0).put("as_of_day","2000-01-01").toString()}){
                display.edit().putString(account,bad).commit();JSONObject recovered=BetaGrowthStore.load(c,account);
                check(recovered.toString().equals(original.toString()),"Recovery never changes DB");
                check(BetaGrowthStore.peek(c).getLong("total")==xp,"Missing/corrupt/stale/wrong-account cache repaired");
                check(BetaGrowthStore.peek(c).getString("as_of_day").equals(BetaV4.day(System.currentTimeMillis()).toString()),"Period summary current without rewards");
            }
            display.edit().remove(account).commit();boolean failed=false;try{BetaGrowthStore.load(failedCommit(c),account);}catch(java.io.IOException expected){failed=true;}
            check(failed,"Failed display commit is reported");check(BetaGrowthStore.displayError(c),"Failed commit marks recoverable state");
            check(BetaGrowthStore.activeWorker(failedCommit(c),account),"ACTIVE_STATE_INDEPENDENT: display save failure must not reactivate legacy growth");
            BetaGrowthStore.refreshDisplay(c);awaitWorker();check(!BetaGrowthStore.displayError(c),"Successful worker repair clears error");check(BetaGrowthStore.peek(c).getLong("total")==xp,"Failed display commit retains original XP");
            failed=false;try{BetaGrowthStore.enabled(failedCommit(c),account,!original.getBoolean("enabled"));}catch(java.io.IOException expected){failed=true;}
            check(failed&&BetaGrowthStore.displayError(c),"DB commit followed by display save failure is reported");
            check(BetaGrowthStore.activeWorker(failedCommit(c),account),"Active DB profile remains active after committed change/display failure");
            try(BetaGrowthStore helper=new BetaGrowthStore(c);android.database.Cursor row=helper.getReadableDatabase().rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{account})){
                check(row.moveToFirst()&&new JSONObject(row.getString(0)).getBoolean("enabled")!=original.getBoolean("enabled"),"Original DB commit survives failed display save");
            }
            BetaGrowthStore.refreshDisplay(c);awaitWorker();check(BetaGrowthStore.peek(c).getBoolean("enabled")!=original.getBoolean("enabled"),"Worker repairs from committed DB after display save failure");
            check(BetaGrowthStore.enabled(c,account,original.getBoolean("enabled")),"Restore original synthetic setting after fault injection");
            check(BetaGrowthStore.load(c,"f".repeat(64))==null,"Another account cannot read profile");check(BetaGrowthStore.peek(c).getString("account").equals(account),"Other account request cannot replace active cache");
            check(!BetaGrowthStore.activeWorker(c,"f".repeat(64)),"Other account cannot use activation check");
            Context unreadable=new ContextWrapper(c){@Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name,int mode,android.database.sqlite.SQLiteDatabase.CursorFactory factory,android.database.DatabaseErrorHandler handler){throw new android.database.sqlite.SQLiteException("Synthetic read failure");}};
            check(BetaGrowthStore.activeWorker(unreadable,account),"Unreadable existing growth DB conservatively freezes legacy rewards");
            java.util.concurrent.atomic.AtomicInteger broadcasts=new java.util.concurrent.atomic.AtomicInteger();BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context context,Intent intent){broadcasts.incrementAndGet();}};
            androidx.core.content.ContextCompat.registerReceiver(c,receiver,new IntentFilter(AppConstants.ACTION_USAGE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
            try{for(int i=0;i<5;i++)BetaGrowthStore.load(c,account);Thread.sleep(300);check(broadcasts.get()==0,"Unchanged display does not cause reload broadcast loop");}finally{c.unregisterReceiver(receiver);}
            final String key=account;ExecutorService concurrent=Executors.newFixedThreadPool(4);try{java.util.List<Future<Boolean>> jobs=new java.util.ArrayList<>();long at=original.getLong("last_response");for(int i=0;i<24;i++)jobs.add(concurrent.submit(()->BetaGrowthStore.confirm(c,key,at,at,false)));for(Future<Boolean> job:jobs)check(!job.get(),"Repeated existing evidence never pays twice");}finally{concurrent.shutdownNow();}
            check(BetaGrowthStore.load(c,account).getLong("total")==xp,"Recovery/concurrency create no XP");
            JSONObject backup=BetaGrowthStore.export(c,account);check(BetaGrowthStore.restore(c,account,backup),"Existing backup roundtrip succeeds");check(BetaGrowthStore.load(c,account).getLong("total")==xp,"Restore does not multiply rewards");
            growth=startActivitySync(new Intent(c,GrowthActivity.class).putExtra("beta_details",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitText(growth,"Lv. "+original.getInt("level"));
            other=startActivitySync(new Intent(c,BetaCatalogActivity.class).putExtra("titles",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            String title=original.getJSONObject("titles").keys().next();BetaIdentity.save(c,"캐시복구 검수 Alex");check(BetaGrowthStore.equip(c,account,title),"Equip saved while growth paused");check(BetaGrowthStore.enabled(c,account,false),"Collection toggle saved while paused");
            Activity paused=other;main(paused::finish);other=null;waitText(growth,BetaIdentity.title(c,title));waitText(growth,BetaCopy.t(c,"paused"));check(true,"Name/title/toggle refresh on growth resume");
            Activity oldGrowth=growth;main(oldGrowth::finish);growth=null;
            display.edit().remove(account).commit();home=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));awaitWorker();check(BetaGrowthStore.peek(c)!=null,"Actual home foreground repairs missing cache");waitText(home,"캐시복구 검수 Alex");
            check(BetaGrowthStore.load(c,account).getLong("total")==xp,"Actual UI creates no growth reward");check(BetaGrowthStore.archive(c,account).toString().equals(qa.getString("archive","")),"Archive remains identical after UI and restore");
            java.io.File dir=new java.io.File(c.getExternalFilesDir(null),"beta997-captures");dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,"home.png"))){getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            result.putString("stream","Beta"+expectedVersion+" recovery: "+checks+" checks passed; signed upgrade/SQLite/cache failure/concurrency/restore/actual Activity resume, synthetic only.\n");code=-1;
        }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}
        finally{try{if(other!=null){Activity a=other;main(a::finish);}if(growth!=null){Activity a=growth;main(a::finish);}if(home!=null){Activity a=home;main(a::finish);}if(original!=null){BetaIdentity.save(c,originalName);BetaGrowthStore.equip(c,account,original.getString("equipped"));BetaGrowthStore.enabled(c,account,original.getBoolean("enabled"));}}catch(Throwable cleanup){result.putString("stream",result.getString("stream","")+"Cleanup failed: "+cleanup);code=0;}}
        finish(code,result);
    }
}
