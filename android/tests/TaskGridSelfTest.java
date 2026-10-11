package dev.bennett.codexmeter;
import org.json.*;import java.nio.file.*;
public final class TaskGridSelfTest {
 public static void main(String[] args)throws Exception {
  JSONObject input=new JSONObject(Files.readString(Path.of(args[0])));long now=System.currentTimeMillis();JSONObject clean=TaskStatusData.clean(input,now);
  if(clean.getJSONArray("rows").length()!=6)throw new AssertionError("Production Windows JSON must receive all six");
  input.getJSONArray("rows").put(input.getJSONArray("rows").getJSONObject(0));boolean rejected=false;try{TaskStatusData.clean(input,now);}catch(IllegalArgumentException expected){rejected=true;}if(!rejected)throw new AssertionError("Reject more than six");
  if(TaskStatusData.widgetCapacity(320,180,1)!=4||TaskStatusData.widgetCapacity(320,240,1)!=6||TaskStatusData.widgetColumns(200)!=1)throw new AssertionError("Compact size policy");
  if(TaskStatusData.widgetColor(true,"running")!=0xff86dfb6||TaskStatusData.widgetColor(true,"completed")!=0xfff28b82||TaskStatusData.widgetColor(true,"interrupted")!=0xfff28b82||TaskStatusData.widgetColor(false,"running")!=0xffa7adb5||TaskStatusData.widgetColor(true,"unknown")!=0xffa7adb5)throw new AssertionError("Status colors");
  if(TaskStatusData.connected(clean,now-180000,now))throw new AssertionError("Stale running must be disconnected");
  System.out.println("Windows production JSON -> Android six-task validation, limits, 5x3 capacity and stale/color checks passed; no real PC-phone network claim.");
 }
}
