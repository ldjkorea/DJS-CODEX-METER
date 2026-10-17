package dev.bennett.codexmeter;

import android.app.*;import android.app.job.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import java.io.*;import org.json.*;
/** Actual Android job delivery versus a fixed synthetic quota; no real account or network. */
public final class Beta999WeeklyRefreshNativeTest extends Instrumentation {
 String phase;int checks;Throwable uiFailure;StringBuilder facts=new StringBuilder();
 void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable e){uiFailure=e;}});if(uiFailure!=null){Throwable e=uiFailure;uiFailure=null;throw new AssertionError(e);}}
 String shell(String command)throws Exception{try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();}}
 String force(int id)throws Exception{return shell("cmd jobscheduler run -f dev.bennett.codexmeter.trial "+id);}
 UsageSnapshot sample(long at,double used){return new UsageSnapshot("plus",true,false,new UsageWindow(25.25,18000,0,(at+7200000)/1000),new UsageWindow(used,604800,0,(at+3*86400000L)/1000),at);}
 int workerSkips(Context c)throws Exception{java.io.File file=new java.io.File(c.getFilesDir(),"diagnostic-logs/events.jsonl");if(!file.isFile())return 0;String text=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);return text.split("refresh_job_skipped_demo_worker",-1).length-1;}
 int skips(Context c)throws Exception{java.io.File file=new java.io.File(c.getFilesDir(),"diagnostic-logs/events.jsonl");if(!file.isFile())return 0;String text=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);return text.split("refresh_job_skipped_demo",-1).length-1;}
 void displayed(MainActivity a,UsageSnapshot expected){main(()->{
  View root=a.findViewById(android.R.id.content);TextView used=root.findViewWithTag("beta999:weekly-used"),remaining=root.findViewWithTag("beta998:quota:weekly:value");
  check(used!=null&&remaining!=null,"Both quota views exist");
  String actual=used.getText().toString(),want=UsagePrecision.used(expected.weekly,0);facts.append("display=").append(actual).append(" expected=").append(want).append(" stored=").append(AppPreferences.loadSnapshot(a).weekly.preciseUsedPercent).append('\n');
  check(actual.contains(want),"Weekly used matches current snapshot: actual="+actual+" expected="+want);
  check(remaining.getText().toString().equals(UsagePrecision.remaining(expected.weekly,0)),"Remaining matches same snapshot");
 });}
 void pushed(MainActivity a,Context c,UsageSnapshot snapshot)throws Exception{
  check(AppPreferences.saveSnapshot(c,snapshot),"Background snapshot commit succeeds");
  String want=UsagePrecision.used(snapshot.weekly,0);boolean[] ready={false};
  for(int i=0;i<60;i++){waitForIdleSync();main(()->{TextView value=a.findViewById(android.R.id.content).findViewWithTag("beta999:weekly-used");ready[0]=value!=null&&value.getText().toString().contains(want);});if(ready[0])break;Thread.sleep(50);}
  main(()->{TextView value=a.findViewById(android.R.id.content).findViewWithTag("beta999:weekly-used");facts.append("background push: actual=").append(value==null?"missing":value.getText()).append(" expected=").append(want).append('\n');});
  check(ready[0],"Foreground quota must update after background save without manual rebuild/broadcast");displayed(a,snapshot);
 }
 void resumeAndClear(MainActivity home,Context c,UsageSnapshot fixed)throws Exception{
  Activity other=startActivitySync(new Intent(c,BetaHubActivity.class).putExtra("page","health").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  java.lang.reflect.Field field=MainActivity.class.getDeclaredField("snapshotListenerRegistered");field.setAccessible(true);
  main(()->{try{check(!field.getBoolean(home),"Stopped home unregisters snapshot listener");}catch(IllegalAccessException e){throw new AssertionError(e);}});
  UsageSnapshot hidden=sample(fixed.fetchedAtMillis+2,52.25);AppPreferences.saveSnapshot(c,hidden);main(other::finish);waitForIdleSync();Thread.sleep(150);displayed(home,hidden);
  main(()->{try{check(field.getBoolean(home),"Resumed home registers listener");}catch(IllegalAccessException e){throw new AssertionError(e);}});
  c.getSharedPreferences("codex_meter_settings_v1",0).edit().remove("last_snapshot").commit();
  boolean[] cleared={false};for(int i=0;i<60;i++){waitForIdleSync();main(()->cleared[0]=home.findViewById(android.R.id.content).findViewWithTag("beta999:weekly-used")==null);if(cleared[0])break;Thread.sleep(50);}
  check(cleared[0],"Background snapshot removal clears obsolete used value");main(()->{TextView remaining=home.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:value");check(remaining!=null&&remaining.getText().toString().equals("—"),"Missing source does not retain an old remaining value");});pushed(home,c,fixed);
 }
 void queuedTransition(Context c,UsageSnapshot fixed)throws Exception{
  JobScheduler jobs=c.getSystemService(JobScheduler.class);android.content.SharedPreferences secure=c.getSharedPreferences("secure_auth_v1",0);String blob=secure.getString("blob","");int beforeWorker=workerSkips(c);
  try{
   synchronized(UsageApi.NETWORK_LOCK){
    SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_NO_NETWORK","SYNTHETIC_NO_NETWORK_REFRESH","",System.currentTimeMillis()+3600000,"beta999-job-transition",""));
    check(!TrialDemo.active(c),"Synthetic non-demo branch");check(RefreshScheduler.schedulePeriodic(c)&&jobs.getPendingJob(73100)!=null,"Non-demo periodic scheduling retained");
    check(RefreshScheduler.scheduleAtNextReset(c,fixed)&&jobs.getPendingJob(73102)!=null,"Non-demo reset scheduling retained");
    check(RefreshScheduler.scheduleImmediate(c),"Queue real-account-shaped immediate job");force(73101);
    boolean active=false;for(int i=0;i<60;i++){if(shell("cmd jobscheduler get-job-state dev.bennett.codexmeter.trial 73101").contains("active")){active=true;break;}Thread.sleep(25);}
    check(active,"Actual worker queued while request lock held; no network can start");
    check(secure.edit().putString("blob",blob).commit(),"Return to original encrypted demo identity before worker proceeds");
   }
   for(int i=0;i<100&&workerSkips(c)==beforeWorker;i++)Thread.sleep(50);
   check(workerSkips(c)>beforeWorker,"Actual queued worker logged its account-to-demo rejection");
   check(jobs.getPendingJob(73101)==null,"Queued worker rechecks demo under the request lock");
   check(AppPreferences.loadSnapshot(c).fetchedAtMillis==fixed.fetchedAtMillis&&AppPreferences.loadSnapshot(c).weekly.preciseUsedPercent==66.75,"Queued non-demo job cannot reset restored demo snapshot");
  }finally{secure.edit().putString("blob",blob).commit();RefreshScheduler.cancelAll(c);}
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);phase=b.getString("phase","verify");start();}
 @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();UsageSnapshot original=null;MainActivity home=null;boolean automatic=false,launch=false,diagnostics=false;try{
  check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic beta only");
  diagnostics=DiagnosticLog.isEnabled(c);DiagnosticLog.setEnabled(c,true);original=AppPreferences.loadSnapshot(c);automatic=AppPreferences.getAutomaticRefresh(c);launch=AppPreferences.getRefreshOnLaunch(c);
  String account=SubscriptionStore.accountKey(c),tokens=SecureTokenStore.load(c).toJson().toString(),growth=BetaGrowthStore.load(c,account).toString();
  boolean updateChoice=UpdatePreferences.automaticChecks(c);RefreshScheduler.cancelAll(c);AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);
  UsageSnapshot fixed=sample(System.currentTimeMillis(),66.75);check(AppPreferences.saveSnapshot(c,fixed),"Store exact66.75 fixture");
  home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();MainActivity screen=home;displayed(screen,fixed);
  if(phase.equals("before")){pushed(screen,c,sample(fixed.fetchedAtMillis+1,43.25));}
  else{
  JobScheduler jobs=c.getSystemService(JobScheduler.class);android.os.PersistableBundle extras=new android.os.PersistableBundle();extras.putString("reason","periodic");
  JobInfo oldJob=new JobInfo.Builder(73100,new ComponentName(c,UsageRefreshJobService.class)).setPeriodic(900000).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setExtras(extras).build();
  int skippedBefore=skips(c);check(jobs.schedule(oldJob)==JobScheduler.RESULT_SUCCESS,"Inject actual leftover periodic job");String delivered=force(73100);facts.append("forced=").append(delivered).append('\n');
  for(int i=0;i<100;i++){UsageSnapshot current=AppPreferences.loadSnapshot(c);if(current.fetchedAtMillis!=fixed.fetchedAtMillis||skips(c)>skippedBefore)break;Thread.sleep(100);}
  Thread.sleep(250);UsageSnapshot after=AppPreferences.loadSnapshot(c);facts.append("after raw=").append(after.weekly.preciseUsedPercent).append(" initial=").append(fixed.weekly.preciseUsedPercent).append(" timestampChanged=").append(after.fetchedAtMillis!=fixed.fetchedAtMillis).append('\n');
  main(screen::rebuild);waitForIdleSync();
  check(after.weekly.preciseUsedPercent==66.75&&after.fetchedAtMillis==fixed.fetchedAtMillis,"Background demo job must not overwrite fixture: actual="+after.weekly.preciseUsedPercent+" expected=66.75");check(skips(c)>skippedBefore,"Actual service logged demo-job rejection");check(jobs.getPendingJob(73100)==null,"Old usage job cancelled");displayed(screen,fixed);
  pushed(screen,c,sample(fixed.fetchedAtMillis+1,43.25));
  check(RefreshScheduler.schedulePeriodic(c),"Demo periodic request safely handled");check(jobs.getPendingJob(73100)==null&&jobs.getPendingJob(73103)==null&&jobs.getPendingJob(73104)==null,"No future periodic demo jobs");
  check(RefreshScheduler.scheduleAtNextReset(c,fixed),"Demo reset request safely handled");check(jobs.getPendingJob(73102)==null,"No future reset demo job");
  UsageSnapshot manual=UsageApi.refreshAndCache(c);check(manual.weekly.preciseUsedPercent==15&&TrialDemo.active(c),"Explicit manual demo refresh still works locally");main(screen::rebuild);waitForIdleSync();displayed(screen,manual);
  UsageSnapshot sameTime=sample(manual.fetchedAtMillis,66.75);AppPreferences.saveSnapshot(c,sameTime);main(screen::rebuild);waitForIdleSync();displayed(screen,sameTime);
  resumeAndClear(screen,c,sameTime);queuedTransition(c,sameTime);
  check(SecureTokenStore.load(c).toJson().toString().equals(tokens),"Tokens preserved");check(BetaGrowthStore.load(c,account).toString().equals(growth),"No XP/reward/evidence mutation");check(UpdatePreferences.automaticChecks(c)==updateChoice,"Release updater choice unaffected");
  }
  result.putString("stream","Beta999 weekly refresh: "+checks+" checks passed; actual JobScheduler delivery, demo isolation, manual refresh, same-time redraw, data preserved.\n"+facts);code=-1;
 }catch(Throwable e){result.putString("stream",facts+android.util.Log.getStackTraceString(e));}
 finally{try{RefreshScheduler.cancelAll(c);DiagnosticLog.setEnabled(c,diagnostics);if(home!=null){MainActivity a=home;main(a::finish);}if(original!=null)AppPreferences.saveSnapshot(c,original);AppPreferences.setAutomaticRefresh(c,automatic);AppPreferences.setRefreshOnLaunch(c,launch);}catch(Throwable e){code=0;result.putString("stream",result.getString("stream","")+e);}}
 finish(code,result);
 }
}
