package dev.bennett.codexmeter;

import android.app.*;import android.app.job.*;import android.appwidget.*;import android.content.*;import android.content.res.Configuration;
import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.util.*;import org.json.*;

/** Owned emulator only. A seed phase runs on v29, then v30 updates the same QA package. */
public final class WidgetSimplificationNativeTest extends Instrumentation {
 private Context c;private int checks;private Bundle args;private Throwable error;
 private void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 private void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable e){error=e;}});if(error!=null)throw new AssertionError(error);waitForIdleSync();}
 @Override public void onCreate(Bundle b){args=b;super.onCreate(b);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{
  c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa"),"Isolated QA package only");
  if("seed".equals(args.getString("phase")))seed();else verify();
  out.putString("stream","Widget simplification native "+args.getString("phase","verify")+": "+checks+" checks passed; emulator fixtures, no Samsung/live PC proof.\n");finish(-1,out);
 }catch(Throwable e){out.putString("stream",android.util.Log.getStackTraceString(e));finish(0,out);}}
 private SharedPreferences prefs(){return c.getSharedPreferences("widget30_upgrade_fixture",0);}
 private void seed()throws Exception{
  if(prefs().contains("usage")){check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).getLongVersionCode()>=59,"Owned seed already exists");return;}
  check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).getLongVersionCode()==59,"Seed actually runs on previous v29");
  AuthTokens old=SecureTokenStore.load(c);SecureTokenStore.save(c,new AuthTokens("","","",0,"",""));
  ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
  AppWidgetHost host=new AppWidgetHost(c,2830);AppWidgetManager m=AppWidgetManager.getInstance(c);
  int usage=host.allocateAppWidgetId(),evo=host.allocateAppWidgetId(),task=host.allocateAppWidgetId();
  try{
   check(m.bindAppWidgetIdIfAllowed(usage,new ComponentName(c,CodexUsageWidget.class)),"Bind usage before update");
   check(m.bindAppWidgetIdIfAllowed(evo,new ComponentName(c,EvolutionWidget.class)),"Bind advanced before update");
   check(m.bindAppWidgetIdIfAllowed(task,new ComponentName(c,TaskStatusWidget.class)),"Bind tasks before update");
   WidgetOptions options=new WidgetOptions("rings","comfortable","material","large","dark","rose",56,"both","used","weekly",true,true,true,true,true,false).withPercentSymbol(false).withVisibleMeters("weekly,five_hour,next_reset");
   AppPreferences.saveWidgetOptions(c,usage,options);AppPreferences.saveWidgetTapAction(c,usage,"refresh");WidgetCrest.save(c,usage,true);
   EvolutionWidget.prefs(c).edit().putString(evo+".elements","week_usage,tier").putInt(evo+".opacity",56).putBoolean(evo+".theme",false).commit();
   prefs().edit().putInt("usage",usage).putInt("evo",evo).putInt("task",task).putInt("seed_version",59).putString("snapshot",AppPreferences.loadSnapshot(c).toJson().toString()).commit();
   Thread.sleep(1500);((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
  }finally{if(old!=null)SecureTokenStore.save(c,old);}
 }
 private void verify()throws Exception{
  check(prefs().getInt("seed_version",0)==59,"Prior version checkpoint retained after replacement");
  check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).getLongVersionCode()==60,"New v30 actually installed");
  AppWidgetManager manager=AppWidgetManager.getInstance(c);int id=prefs().getInt("usage",-1),evo=prefs().getInt("evo",-1),task=prefs().getInt("task",-1);
  for(int widget:new int[]{id,evo,task})check(manager.getAppWidgetInfo(widget)!=null,"Existing appWidgetId/provider survived update");
  manager.updateAppWidgetOptions(id,size(180,70));check(manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)==180,"Bound widget resize reaches its original provider");
  WidgetOptions before=AppPreferences.loadWidgetOptions(c,id);check(before.opacity==56&&before.accent.equals("rose")&&before.visibleMeters.equals("weekly,five_hour,next_reset")&&!before.showPercentSymbol,"Per-id options survived replacement");
  check(AppPreferences.getWidgetTapAction(c,id).equals("refresh")&&WidgetCrest.enabled(c,id),"Tap and crest retained");
  check(EvolutionWidget.prefs(c).getInt(evo+".opacity",0)==56&&!EvolutionWidget.prefs(c).getBoolean(evo+".theme",true),"Evolution settings retained");
  AuthTokens auth=SecureTokenStore.load(c);UsageSnapshot original=AppPreferences.loadSnapshot(c);SharedPreferences settings=c.getSharedPreferences("codex_meter_settings_v1",0);Map<String,?> saved=new HashMap<>(settings.getAll());
  AppWidgetHost host=new AppWidgetHost(c,2830);host.startListening();
  try{
   ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
   long now=System.currentTimeMillis();SecureTokenStore.save(c,new AuthTokens("WIDGET_QA_FAKE","WIDGET_QA_FAKE","",now+86400000,"widget30-fixture-"+UUID.randomUUID(),""));
   AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,new UsageWindow(23.75,18000,0,(now+3600000)/1000),new UsageWindow(41.875,604800,0,(now+86400000)/1000),now));
   AppPreferences.setShowDashboardFiveHour(c,true);AppPreferences.setShowDashboardWeekly(c,true);AppPreferences.clearLastError(c);AppPreferences.setUsageDecimalPlaces(c,3);
   String account=LanSync.account(c);SharedPreferences tasks=c.getSharedPreferences("codex_task_trial",0);tasks.edit().putBoolean(account+"_on",true).commit();
   JSONObject data=raw(now);TaskStatusStore.save(c,account,data);check(TaskStatusStore.snapshot(c).getJSONArray("rows").length()==6,"Six rows through production store");
   String prior=TaskStatusData.widgetFingerprint(TaskStatusStore.snapshot(c),TaskStatusStore.received(c),0,now,true);
   for(int i=3;i<6;i++){JSONArray sourceRows=data.getJSONArray("rows");JSONObject changed=null;for(int j=0;j<sourceRows.length();j++)if(sourceRows.getJSONObject(j).getString("Thread").equals(String.format("%032x",i)))changed=sourceRows.getJSONObject(j);
   changed.put("State","completed").put("Sequence",7+i);JSONArray reordered=new JSONArray().put(changed);for(int j=0;j<sourceRows.length();j++)if(sourceRows.getJSONObject(j)!=changed)reordered.put(sourceRows.getJSONObject(j));data.put("rows",reordered);TaskStatusStore.save(c,account,data);String next=TaskStatusData.widgetFingerprint(TaskStatusStore.snapshot(c),TaskStatusStore.received(c),0,now,true);check(!prior.equals(next),"Actual store notices fourth-sixth status change");prior=next;}
   check(TaskStatusStore.state(c,"unknown").equals(c.getString(R.string.task_trial_unverified)),"Unknown is not interrupted");
   main(()->{
    for(float font:new float[]{1f,1.5f}){Configuration conf=new Configuration(c.getResources().getConfiguration());conf.fontScale=font;c.getResources().updateConfiguration(conf,c.getResources().getDisplayMetrics());
     for(int[] size:new int[][]{{180,70},{250,85},{320,180}}){Bundle b=size(size[0],size[1]);
      View normal=draw(WidgetRenderer.buildPreview(c,id,WidgetOptions.defaults(),b,false),size[0],size[1],"usage-"+size[0]+"-"+font+".png");
      check(normal.findViewById(android.R.id.background).hasOnClickListeners(),"Usage tap is connected to PendingIntent");
      AppPreferences.setLastError(c,"Synthetic failure");View stale=draw(WidgetRenderer.buildPreview(c,id,WidgetOptions.defaults(),b,false),size[0],size[1],"cached-"+size[0]+"-"+font+".png");
      TextView status=stale.findViewById(R.id.updated_label);check(status.getVisibility()==View.VISIBLE&&status.getWidth()>0&&status.getHeight()>0,"Failure label occupies real space");AppPreferences.clearLastError(c);
     }
     View four=draw(TaskStatusWidget.render(c,320,180,4,TaskStatusStore.snapshot(c),now,true),320,180,"tasks-"+font+".png");
     if(font==1f){LinearLayout grid=four.findViewById(R.id.task_status_tasks);int visible=0;for(int line=0;line<grid.getChildCount();line++)visible+=((LinearLayout)grid.getChildAt(line)).getChildCount();check(visible==4,"5x3 production RemoteViews contains four complete cards");}
     draw(WidgetRenderer.buildPreview(c,id,WidgetOptions.defaults(),size(180,70),true),180,70,"usage-crest-"+font+".png");
    }
    Configuration conf=new Configuration(c.getResources().getConfiguration());conf.fontScale=1;c.getResources().updateConfiguration(conf,c.getResources().getDisplayMetrics());
    View v=draw(TaskStatusWidget.render(c,320,180,4,TaskStatusStore.snapshot(c),now,true,now+1),320,180,"tasks-failed.png");
    TextView state=v.findViewById(R.id.task_status_state);check(state.getCurrentTextColor()==0xffa7adb5&&!state.getText().toString().equals(c.getString(R.string.task_trial_running)),"Failed connection is gray, not running");
    draw(TaskStatusWidget.render(c,320,240,4,TaskStatusStore.snapshot(c),now,true),320,240,"tasks-six.png");
    View expired=draw(TaskStatusWidget.render(c,320,180,4,TaskStatusStore.snapshot(c),now-120001,true),320,180,"tasks-stale.png");
    check(((TextView)expired.findViewById(R.id.task_status_state)).getCurrentTextColor()==0xffa7adb5,"Expired running record becomes gray");
    draw(TaskStatusWidget.render(c,320,180,4,new JSONObject(),0,true),320,180,"tasks-no-connection.png");
    try{draw(TaskStatusWidget.render(c,320,180,4,new JSONObject().put("checked",now).put("verified",true).put("rows",new JSONArray()),now,true),320,180,"tasks-empty.png");}catch(Exception e){throw new RuntimeException(e);}
    AppPreferences.setShowDashboardFiveHour(c,false);View weekly=draw(WidgetRenderer.buildPreview(c,id,WidgetOptions.defaults(),size(180,70),true),180,70,"usage-week-only.png");check(!allText(weekly).contains("5시간"),"Hidden five-hour labels omitted");AppPreferences.setShowDashboardFiveHour(c,true);
    UsageSnapshot observed=AppPreferences.loadSnapshot(c);AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(0,604800,0,(now+86400000)/1000),now));
    View zero=draw(WidgetRenderer.buildPreview(c,id,WidgetOptions.defaults(),size(180,70),false),180,70,"usage-zero.png");check(allText(zero).contains("100.000%"),"Confirmed zero use is full remaining quota");
    AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,null,now));View missing=draw(WidgetRenderer.buildPreview(c,id,WidgetOptions.defaults(),size(180,70),false),180,70,"usage-unknown.png");check(!allText(missing).contains("100.000%"),"Absent limit does not manufacture zero consumption");AppPreferences.saveSnapshot(c,observed);
    EvolutionWidget.prefs(c).edit().putString("992.elements","week_usage,tier").putInt("992.opacity",56).putBoolean("992.theme",false).commit();new EvolutionWidget().onRestored(c,new int[]{992},new int[]{993});
    check(EvolutionWidget.prefs(c).getString("993.elements","").equals("week_usage,tier")&&EvolutionWidget.prefs(c).getInt("993.opacity",0)==56&&!EvolutionWidget.prefs(c).getBoolean("993.theme",true),"Evolution restore keeps all settings");
   });
   WidgetConfigActivity edit=(WidgetConfigActivity)startActivitySync(new Intent(c,WidgetConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
   main(()->{try{java.lang.reflect.Method method=WidgetConfigActivity.class.getDeclaredMethod("currentOptions");method.setAccessible(true);WidgetOptions next=(WidgetOptions)method.invoke(edit);check(next.density.equals(before.density)&&next.surfaceStyle.equals(before.surfaceStyle)&&next.resetMode.equals(before.resetMode)&&next.showUpdated==before.showUpdated,"Editor preserves controls not exposed on screen");edit.finish();}catch(Exception e){throw new RuntimeException(e);}});
   WidgetGuideActivity guide=(WidgetGuideActivity)startActivitySync(new Intent(c,WidgetGuideActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();main(()->{check(allText(guide.findViewById(R.id.dashboard_scroll)).contains(guide.getString(R.string.ws_sample)),"Guide clearly labels synthetic preview");save(guide.findViewById(android.R.id.content),"widget-guide.png");guide.finish();});
   TaskStatusStore.failed(c,account);check(!TaskStatusStore.connected(c,TaskStatusStore.snapshot(c),TaskStatusStore.received(c),System.currentTimeMillis()),"Production failure invalidates connection");
   TaskStatusStore.save(c,account,data.put("checked",System.currentTimeMillis()));check(TaskStatusStore.failed(c)==0,"Successful retry clears failure");
   tasks.edit().remove(account).remove(account+"_on").remove(account+"_received").remove(account+"_failed").commit();
  }finally{
   host.stopListening();if(auth!=null)SecureTokenStore.save(c,auth);if(original!=null)AppPreferences.saveSnapshot(c,original);
   SharedPreferences.Editor restore=settings.edit();for(String key:settings.getAll().keySet())if(!saved.containsKey(key))restore.remove(key);
   for(Map.Entry<String,?> e:saved.entrySet()){Object v=e.getValue();if(v instanceof String)restore.putString(e.getKey(),(String)v);else if(v instanceof Boolean)restore.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof Integer)restore.putInt(e.getKey(),(Integer)v);else if(v instanceof Long)restore.putLong(e.getKey(),(Long)v);else if(v instanceof Float)restore.putFloat(e.getKey(),(Float)v);}restore.commit();
   ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
  }
 }
 private JSONObject raw(long now)throws Exception{JSONArray rows=new JSONArray();for(int i=0;i<6;i++)rows.put(new JSONObject().put("Thread",String.format("%032x",i)).put("Turn",String.format("%032x",i+20)).put("State","running").put("Name","GPT HUD · 테스트 작업 "+(i+1)).put("At",now).put("Seen",now).put("Sequence",6-i));return new JSONObject().put("epoch","a".repeat(32)).put("device","b".repeat(64)).put("source","local_log_trial").put("checked",now).put("verified",true).put("rows",rows);}
 private Bundle size(int w,int h){Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);return b;}
 private View draw(RemoteViews rv,int width,int height,String name){View v=rv.apply(c,new FrameLayout(c));int w=Ui.dp(c,width),h=Ui.dp(c,height);v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);
  if(v.findViewById(R.id.task_status_tasks)!=null){LinearLayout rows=v.findViewById(R.id.task_status_tasks);for(int i=0;i<rows.getChildCount();i++)check(rows.getChildAt(i).getBottom()<=rows.getHeight(),"Task row fits the widget");}
  save(v,name);return v;
 }
 private String allText(View view){StringBuilder s=new StringBuilder();if(view instanceof TextView&&view.getVisibility()==View.VISIBLE)s.append(((TextView)view).getText());if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)s.append(allText(((ViewGroup)view).getChildAt(i)));return s.toString();}
 private void save(View view,String name){try{File folder=new File(c.getExternalFilesDir(null),"widget30-fixtures");folder.mkdirs();Bitmap b=Bitmap.createBitmap(Math.max(1,view.getWidth()),Math.max(1,view.getHeight()),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xfff3f0f7);view.draw(canvas);try(FileOutputStream f=new FileOutputStream(new File(folder,name))){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();}catch(Exception e){throw new RuntimeException(e);}}
}
