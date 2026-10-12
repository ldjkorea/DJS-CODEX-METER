package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Synthetic, isolated emulator account. Exercises production SQLite and real Activities. */
public final class CollectionRestartNativeTest extends Instrumentation {
    Context c;Activity active;int checks;Throwable error;String account;
    void check(boolean value,String note){checks++;if(!value)throw new AssertionError(note);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable e){error=e;}});if(error!=null)throw new AssertionError(error);waitForIdleSync();}
    JSONObject state()throws Exception{return RpgProgressStore.export(c,account).getJSONObject("rpg");}
    void save(JSONObject d)throws Exception{try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath(RpgProgressStore.NAME),null)){db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{account,d.toString()});}c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(account).remove(account+":collection_revision").commit();}
    void token(long value)throws Exception{Thread.sleep(3);long now=System.currentTimeMillis();RpgProgressStore.tokens(c,account,new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",now).put("summary",new JSONObject().put("lifetimeTokens",value)));}
    void capture(View root,String name,int width,int height)throws Exception{main(()->{int w=Ui.dp(c,width),h=Ui.dp(c,height);root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));try{File folder=new File(c.getExternalFilesDir(null),"collection-restart-fixtures");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(Exception e){throw new RuntimeException(e);}b.recycle();});}
    void ready(RpgCollectionActivity a)throws Exception{for(int i=0;i<100;i++){Thread.sleep(40);boolean[] done={false};main(()->done[0]=!a.busy&&a.state!=null);if(done[0])return;}throw new AssertionError("Collection timeout");}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa")&&Build.MODEL.contains("sdk"),"Owned emulator package only");
        AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);UpdatePreferences.setAutomaticChecks(c,false);long now=System.currentTimeMillis();
        SecureTokenStore.save(c,new AuthTokens("RESTART_QA_FAKE_ACCESS","RESTART_QA_FAKE_REFRESH","",now+86400000,"restart-synthetic-"+UUID.randomUUID(),""));account=SubscriptionStore.accountKey(c);
        AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(41.125,604800,0,(now+86400000)/1000),now));
        JSONObject old=AccountProgress.empty();old.getJSONObject("tier").put("tier",7).put("highest",7);
        RpgEngine.ingestOfficial(old,account,new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",now).put("summary",new JSONObject().put("lifetimeTokens",100_000_000)),now);
        AchievementJourney.prepare(old,now);TitleJourney.prepare(old,now);AchievementJourney.setEnabled(old,true,now);TitleJourney.setEnabled(old,true,now);long xp=RpgEngine.state(old).getLong("xp");save(old);
        JSONObject s=state();check(!AchievementJourney.enabled(s)&&!TitleJourney.enabled(s),"Existing ON account resets OFF on update");check(RpgGrowthFacts.earned(s).isEmpty()&&TitleJourney.owned(s).isEmpty(),"Old titles do not refill new collection");check(s.getLong("xp")==xp&&s.getJSONObject("KR").getInt("highest")==7,"Lifetime values preserved");
        check(RpgProgressStore.peek(c).getJSONObject("rpg").getInt("unlocked_achievements")==0,"Offline display cache zero");
        RpgProgressStore.setAchievementsEnabled(c,account,true);RpgProgressStore.setTitlesEnabled(c,account,true);
        JSONObject d=RpgProgressStore.archive(c,account).getJSONObject("document");
        JSONObject st=RpgEngine.state(d);long at=System.currentTimeMillis();String day=LedgerAggregation.day(at).toString();
        // Explicit synthetic post-start consumption evidence; never placed in production data.
        for(String key:new String[]{AchievementJourney.KEY,TitleJourney.KEY})st.getJSONObject(key).getJSONObject("activity").put(day,at+1);
        save(d);Thread.sleep(5);token(102_000_000);s=state();
        List<String> ids=Arrays.asList("days_1","heritage_1","tokens_1000000");check(RpgGrowthFacts.earned(s).containsAll(ids),"Actual engine awards new post-start medals");check(TitleJourney.owned(s).containsAll(ids),"Actual engine collects new independent titles");
        check(RpgProgressStore.equip(c,account,"days_1"),"Explicit title selection");check(RpgShowcase.save(c,account,ids,s),"Three medal selection");JSONObject archive=RpgProgressStore.archive(c,account);long earned=state().getLong("xp");
        for(String mode:new String[]{"achievements","showcase","titles"}){
            RpgCollectionActivity a=(RpgCollectionActivity)startActivitySync(new Intent(c,RpgCollectionActivity.class).putExtra("collection_mode",mode).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));active=a;ready(a);
            main(()->{check(a.findViewById(android.R.id.content).findViewWithTag("collection:recommend")==null,"No random recommendation control");});
            capture(a.findViewById(android.R.id.content),"collection-"+mode,360,900);main(a::finish);active=null;
        }
        for(boolean titles:new boolean[]{false,true}){check(RpgProgressStore.resetCollection(c,account,titles),"Confirmed scoped reset transaction");}
        check(RpgProgressStore.restoreArchive(c,account,archive),"Same-account backup merge succeeds");s=state();check(RpgGrowthFacts.earned(s).isEmpty()&&TitleJourney.owned(s).isEmpty(),"Old same-revision backup cannot revive manual reset");check(!AchievementJourney.enabled(s)&&!TitleJourney.enabled(s)&&s.getLong("xp")==earned,"Restore keeps OFF and lifetime XP");check(s.optString("equipped_title").isEmpty(),"Reset removes equipped title");
        String lan=LanSync.account(c);c.getSharedPreferences("codex_task_trial",0).edit().putBoolean(lan+"_on",true).commit();
        JSONArray rows=new JSONArray();for(int i=0;i<6;i++)rows.put(new JSONObject().put("Thread",String.format(Locale.ROOT,"%032x",i+1)).put("Turn",String.format(Locale.ROOT,"%032x",20+i)).put("Name","작업 "+(i+1)+" · GPT HUD 개발").put("State",i==0?"running":i==1?"completed":"interrupted").put("At",System.currentTimeMillis()).put("Seen",System.currentTimeMillis()).put("Sequence",6-i));
        JSONObject tasks=new JSONObject().put("epoch","1".repeat(32)).put("device","2".repeat(64)).put("source","local_log_trial").put("verified",true).put("checked",System.currentTimeMillis()).put("rows",rows);TaskStatusStore.save(c,lan,tasks);
        for(int width:new int[]{160,320,411}){FrameLayout parent=new FrameLayout(c);View view=CombinedStatusWidget.render(c,width,300).apply(c,parent);capture(view,"combined-"+width,width,300);main(()->{check(view.findViewById(R.id.combined_usage).getVisibility()==View.VISIBLE,"Combined quota visible");check(((TextView)view.findViewById(R.id.combined_usage)).getText().toString().contains("59%"),"Actual whole percent, no .000");check(((LinearLayout)view.findViewById(R.id.task_status_tasks)).getChildCount()>=2,"Combined tasks rendered");});}
        View compact=CombinedStatusWidget.render(c,320,260).apply(c,new FrameLayout(c));capture(compact,"combined-default",320,260);
        main(()->{LinearLayout tasksView=compact.findViewById(R.id.task_status_tasks);check(tasksView.getChildCount()==2,"Default 5x4 shows two rows");for(int i=0;i<2;i++)check(((LinearLayout)tasksView.getChildAt(i)).getChildCount()==2,"Default row has two tasks");});
        // Use explicit stale render input: an old running task must become gray/disconnected.
        View stale=TaskStatusWidget.render(c,320,240,TierTheme.tier(c),TaskStatusStore.snapshot(c),System.currentTimeMillis()-180000,true,0).apply(c,new FrameLayout(c));
        main(()->check(((TextView)stale.findViewById(R.id.task_status_dot)).getCurrentTextColor()==TaskStatusData.widgetColor(false,"running"),"Old running status is gray"));
        result.putString("stream","Collection restart native: "+checks+" checks passed; real SQLite/Activities and six synthetic tasks. Physical Galaxy and real account remain unverified.\n");code=-1;
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));}finally{if(active!=null)main(active::finish);}finish(code,result);}
}
