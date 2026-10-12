package dev.bennett.codexmeter;

import android.app.*;import android.content.*;import android.content.res.Configuration;import android.graphics.*;import android.os.Bundle;import android.view.*;import android.widget.*;import java.io.*;import java.util.*;import org.json.*;

/** Production task store and RemoteViews; detached synthetic account on the owned QA emulator. */
public final class PhaseBNativeTest extends Instrumentation {
    int checks;Context c;Throwable error;
    void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable t){error=t;}});if(error!=null)throw new AssertionError(error);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    JSONObject raw(long now,int count)throws Exception {JSONArray rows=new JSONArray();for(int i=0;i<count;i++)rows.put(new JSONObject().put("Thread",String.format("%032x",i)).put("Turn",String.format("%032x",i+20)).put("State",i%3==0?"running":i%3==1?"completed":"interrupted").put("Name","GPT HUD 개발 · 아주 긴 작업 이름 "+(i+1)).put("At",now).put("Seen",now).put("Sequence",10-i));return new JSONObject().put("epoch","a".repeat(32)).put("device","b".repeat(64)).put("source","local_log_trial").put("checked",now).put("verified",true).put("rows",rows);}
    @Override public void onStart(){Bundle result=new Bundle();AuthTokens original=null;Configuration config=null;String account=null;int code=0;try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa"),"Isolated QA package only");check(android.os.Build.MODEL.contains("sdk"),"Emulator only");
        original=SecureTokenStore.load(c);config=new Configuration(c.getResources().getConfiguration());long now=System.currentTimeMillis();AuthTokens trial=new AuthTokens("PHASE_B_FAKE","PHASE_B_FAKE_REFRESH","",now+86400000,"phase-b-task-synthetic-"+UUID.randomUUID(),"");SecureTokenStore.save(c,trial);account=LanSync.account(c);
        SharedPreferences prefs=c.getSharedPreferences("codex_task_trial",0);check(!prefs.contains(account),"New detached account cannot overwrite a task cache");prefs.edit().putBoolean(account+"_on",true).commit();
        JSONObject wire=raw(now,6);TaskStatusStore.save(c,account,wire);check(TaskStatusStore.snapshot(c).getJSONArray("rows").length()==6,"Production store receives six metadata rows");
        for(int i=3;i<6;i++){JSONObject next=new JSONObject(wire.toString());JSONArray rows=next.getJSONArray("rows");JSONObject changed=null;for(int j=0;j<rows.length();j++)if(rows.getJSONObject(j).getString("Thread").equals(String.format("%032x",i)))changed=rows.getJSONObject(j);String fingerprint=TaskStatusData.widgetFingerprint(TaskStatusStore.snapshot(c),now,0,now,true);changed.put("State","completed").put("Sequence",20+i);JSONArray sorted=new JSONArray().put(changed);for(int j=0;j<rows.length();j++)if(rows.getJSONObject(j)!=changed)sorted.put(rows.getJSONObject(j));next.put("rows",sorted);TaskStatusStore.save(c,account,next);check(!fingerprint.equals(TaskStatusData.widgetFingerprint(TaskStatusStore.snapshot(c),now,0,now,true)),"Fourth-sixth state changes repaint");wire=next;}
        String kept=TaskStatusStore.snapshot(c).toString();boolean rejected=false;try{TaskStatusStore.save(c,account,raw(now,6));}catch(IllegalArgumentException expected){rejected=true;}check(rejected&&kept.equals(TaskStatusStore.snapshot(c).toString()),"Older response keeps last accepted snapshot");
        TaskStatusStore.failed(c,account);check(!TaskStatusStore.connected(c,TaskStatusStore.snapshot(c),TaskStatusStore.received(c),System.currentTimeMillis()),"Failed connection invalidates running display");
        TaskStatusStore.save(c,account,wire.put("checked",System.currentTimeMillis()));check(TaskStatusStore.failed(c)==0,"Identical authenticated retry restores connection");kept=TaskStatusStore.snapshot(c).toString();
        SecureTokenStore.save(c,new AuthTokens("PHASE_B_OTHER","PHASE_B_OTHER","",now+86400000,"phase-b-other-"+UUID.randomUUID(),""));check(TaskStatusStore.snapshot(c)==null&&!TaskStatusStore.enabled(c),"Other account cannot display previous tasks");
        SecureTokenStore.save(c,trial);check(TaskStatusStore.snapshot(c).toString().equals(kept),"Returning account restores its accepted task cache");
        // Rendering uses explicit sanitized metadata; no LAN call, live pairing or user token is involved.
        for(Locale locale:new Locale[]{Locale.KOREAN,Locale.ENGLISH})for(float font:new float[]{1f,1.5f})for(int n=1;n<=6;n++){
            JSONObject clean=TaskStatusData.clean(raw(now,n),now);int count=n;main(()->{Configuration conf=new Configuration(c.getResources().getConfiguration());conf.fontScale=font;conf.setLocale(locale);c.getResources().updateConfiguration(conf,c.getResources().getDisplayMetrics());
                for(int[] size:new int[][]{{200,240},{320,180},{320,240}})try{render(clean,now,size[0],size[1],font,count,locale);}catch(Exception e){throw new RuntimeException(e);}
            });
        }
        main(()->{try{Configuration conf=new Configuration(c.getResources().getConfiguration());conf.fontScale=1;c.getResources().updateConfiguration(conf,c.getResources().getDisplayMetrics());JSONObject clean=TaskStatusData.clean(raw(now,6),now);
            for(boolean failed:new boolean[]{false,true}){View stale=TaskStatusWidget.render(c,320,180,4,clean,failed?now:now-120001,true,failed?now+1:0).apply(c,new FrameLayout(c));TextView state=stale.findViewById(R.id.task_status_state);check(state.getCurrentTextColor()==0xffa7adb5&&!state.getText().toString().equals(c.getString(R.string.task_trial_running)),"Disconnected or stale state is gray and explicit");}
            View empty=TaskStatusWidget.render(c,320,180,4,TaskStatusData.clean(raw(now,0),now),now,true).apply(c,new FrameLayout(c));check(((TextView)empty.findViewById(R.id.task_status_state)).getVisibility()==View.GONE,"Empty connected state does not invent work");
        }catch(Exception e){throw new RuntimeException(e);}});
        result.putString("stream","Phase B native: "+checks+" checks passed; actual task store/RemoteViews, synthetic account, no Samsung/LAN proof.\n");code=-1;
    }catch(Throwable t){result.putString("stream",android.util.Log.getStackTraceString(t));code=0;}finally{try{
        if(c!=null&&account!=null)c.getSharedPreferences("codex_task_trial",0).edit().remove(account).remove(account+"_on").remove(account+"_received").remove(account+"_failed").commit();
        if(c!=null&&original!=null)SecureTokenStore.save(c,original);if(c!=null&&config!=null)c.getResources().updateConfiguration(config,c.getResources().getDisplayMetrics());if(c!=null)TaskStatusWidget.update(c);
    }catch(Throwable restore){result.putString("stream",android.util.Log.getStackTraceString(restore));code=0;}}finish(code,result);}
    void render(JSONObject clean,long now,int width,int height,float font,int requested,Locale locale)throws Exception {
        View view=TaskStatusWidget.render(c,width,height,4,clean,now,true,0).apply(c,new FrameLayout(c));int w=Ui.dp(c,width),h=Ui.dp(c,height);view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);
        LinearLayout grid=view.findViewById(R.id.task_status_tasks);int count=0;for(int i=0;i<grid.getChildCount();i++){LinearLayout line=(LinearLayout)grid.getChildAt(i);check(line.getBottom()<=grid.getHeight(),"Task rows fit widget");for(int j=0;j<line.getChildCount();j++){View cell=line.getChildAt(j);TextView name=cell.findViewById(R.id.task_status_name),state=cell.findViewById(R.id.task_status_state);check(cell.getWidth()>0&&name.getLineCount()==1,"Long title keeps compact ellipsis");check(state.getBottom()<=cell.getHeight()-cell.getPaddingBottom(),"Status view fits");check(name.getContentDescription()!=null,"TalkBack includes full task metadata");check(state.getCurrentTextColor()==(state.getText().toString().equals(c.getString(R.string.task_trial_running))?0xff86dfb6:0xfff28b82),"Observed state text and color agree");count++;}}
        check(count==Math.min(requested,TaskStatusData.widgetCapacity(width,height,font)),"Responsive one-six task count: "+width+"x"+height+" font="+font+" requested="+requested+" actual="+count);
        if(width==320&&height==180){File folder=new File(c.getExternalFilesDir(null),"phase-b-fixtures");folder.mkdirs();Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));try(FileOutputStream output=new FileOutputStream(new File(folder,"tasks-"+requested+"-"+font+"-"+locale.getLanguage()+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();}
    }
}
