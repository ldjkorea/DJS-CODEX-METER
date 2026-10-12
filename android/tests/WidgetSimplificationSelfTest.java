package dev.bennett.codexmeter;
import org.json.*;

/** Synthetic PC wire/state evidence, independent of a phone or live PC connection. */
public final class WidgetSimplificationSelfTest {
 static int checks;static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  long now=1_800_000_020_000L;JSONObject raw=new JSONObject().put("epoch","a".repeat(32)).put("device","b".repeat(64)).put("source","local_log_trial").put("checked",now).put("verified",true);JSONArray rows=new JSONArray();
  for(int i=0;i<6;i++)rows.put(new JSONObject().put("Thread",String.format("%032x",i)).put("Turn",String.format("%032x",i+10)).put("At",now).put("Seen",now).put("Sequence",6-i).put("State",i==4?"running":i==2?"interrupted":"completed").put("Name","작업 "+i));raw.put("rows",rows);
  JSONObject clean=TaskStatusData.clean(raw,now);JSONArray ordered=TaskStatusData.displayRows(clean);
  check(ordered.length()==6,"Six tasks retained");check(ordered.getJSONObject(0).getString("name").equals("작업 4"),"Running first despite wire order");check(ordered.getJSONObject(1).getString("state").equals("interrupted"),"Interrupted ahead of completed");check(clean.getJSONArray("rows").getJSONObject(0).getLong("seq")==6,"Do not mutate replay ordering");
  String base=TaskStatusData.widgetFingerprint(clean,now,0,now,true);
  JSONObject heartbeat=new JSONObject(clean.toString()).put("checked",now+5000);
  check(base.equals(TaskStatusData.widgetFingerprint(heartbeat,now+5000,0,now+5000,true)),"Same-minute heartbeat skips expensive bitmap repaint");
  for(int i=3;i<6;i++)for(String field:new String[]{"state","name","issue"}){
   JSONObject changed=new JSONObject(clean.toString());changed.getJSONArray("rows").getJSONObject(i).put(field,field.equals("state")?"interrupted":"changed");
   check(!base.equals(TaskStatusData.widgetFingerprint(changed,now,0,now,true)),"Task 4-6 changes repaint: "+field);
  }
  check(!base.equals(TaskStatusData.widgetFingerprint(clean,now,now+1,now,true)),"Explicit connection failure repaint");
  check(!base.equals(TaskStatusData.widgetFingerprint(clean,now,0,now+120001,true)),"Freshness expiry repaint");
  check(!base.equals(TaskStatusData.widgetFingerprint(clean,now+60000,0,now+60000,true)),"Last sync minute repaint");
  check(!base.equals(TaskStatusData.widgetFingerprint(clean,now,0,now,false)),"OFF repaint");
  check(TaskStatusData.widgetCapacity(320,180,1)==4&&TaskStatusData.widgetCapacity(320,240,1)==6,"Four/six task capacity");
  check(TaskStatusData.widgetColor(false,"running")==0xffa7adb5,"Disconnected gray");
  rows.getJSONObject(0).put("State","waiting_for_approval");boolean rejected=false;try{TaskStatusData.clean(raw,now);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Unreported waiting cannot be invented by receiver");rows.getJSONObject(0).put("State","completed");
  rows.getJSONObject(0).put("Issue","command_error").put("IssueAt",now);clean=TaskStatusData.clean(raw,now);check(clean.getJSONArray("rows").getJSONObject(0).getString("state").equals("completed"),"Command error does not turn completion into full task failure");
  check(TaskStatusData.name("Bearer abcsecret123456").contains("redacted"),"Title secrets filtered");
  System.out.println("Widget simplification: "+checks+" checks passed; synthetic task evidence only.");
 }
}
