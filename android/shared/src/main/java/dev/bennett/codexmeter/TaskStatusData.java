package dev.bennett.codexmeter;

import org.json.JSONArray;
import org.json.JSONObject;

/** Strict metadata allow-list, not a transcript or an inferred project outcome. */
public final class TaskStatusData {
    public static final int MAX_TASKS=6;
    private TaskStatusData() {}
    /** Presentation order only. Keep the received sequence ordering for replay validation. */
    public static JSONArray displayRows(JSONObject data){
        JSONArray result=new JSONArray(),input=data==null?null:data.optJSONArray("rows");
        java.util.List<JSONObject> rows=new java.util.ArrayList<>();
        if(input!=null)for(int i=0;i<input.length();i++)if(input.optJSONObject(i)!=null)rows.add(input.optJSONObject(i));
        rows.sort((a,b)->Integer.compare(priority(a),priority(b)));
        for(JSONObject row:rows)result.put(row);return result;
    }
    private static int priority(JSONObject row){
        if("running".equals(row.optString("state")))return 0;
        if(!row.optString("issue").isEmpty()||"interrupted".equals(row.optString("state")))return 1;
        return 2;
    }
    public static String widgetFingerprint(JSONObject data,long received,long failed,long now,boolean enabled){
        return enabled+"|"+(failed<=received&&connected(data,received,now))+"|"+(data!=null&&data.optBoolean("verified"))+"|"+(received/60000)+"|"+displayRows(data).toString();
    }
    public static int widgetColumns(int width){return width>=280?2:1;}
    public static int widgetCapacity(int width,int height,float font){return widgetColumns(width)*Math.max(1,Math.min(3,(int)((height-94)/(40*Math.max(1,font)))));}
    public static int widgetColor(boolean current,String state){return !current?0xffa7adb5:state.equals("running")?0xff86dfb6:state.equals("completed")||state.equals("interrupted")?0xfff28b82:0xffa7adb5;}
    public static JSONObject clean(JSONObject data,long now)throws Exception {
        if(data==null)return new JSONObject().put("verified",false).put("rows",new JSONArray()).put("checked",0);
        String epoch=data.getString("epoch"),device=data.getString("device");
        if(!epoch.matches("[a-f0-9]{32}")||!device.matches("[a-f0-9]{64}")||!data.getString("source").equals("local_log_trial"))throw new IllegalArgumentException("Invalid observer");
        long checked=data.getLong("checked");if(checked<0||checked>now+300000)throw new IllegalArgumentException("Invalid check time");
        JSONArray input=data.getJSONArray("rows"),rows=new JSONArray();if(input.length()>MAX_TASKS)throw new IllegalArgumentException("Too many tasks");
        long previous=Long.MAX_VALUE;java.util.Set<String> threads=new java.util.HashSet<>();
        for(int i=0;i<input.length();i++){JSONObject r=input.getJSONObject(i);String thread=r.getString("Thread"),turn=r.getString("Turn"),state=r.getString("State");
            long at=r.getLong("At"),seen=r.getLong("Seen"),seq=r.getLong("Sequence");
            if(!threads.add(thread)||!thread.matches("[a-f0-9-]{16,40}")||!turn.matches("[a-f0-9-]{16,40}")||!(state.equals("running")||state.equals("completed")||state.equals("interrupted")))throw new IllegalArgumentException("Invalid task");
            if(at<=0||at>now+300000||seen<at-300000||seen>now+300000||seq<=0||seq>=previous)throw new IllegalArgumentException("Invalid task ordering");previous=seq;
            String issue=r.optString("Issue","");long issueAt=r.optLong("IssueAt",0);
            if(!(issue.equals("command_error")||issue.equals("tool_error"))||issueAt<=0||issueAt>now+300000){issue="";issueAt=0;}
            rows.put(new JSONObject().put("thread",thread).put("turn",turn).put("state",state).put("name",name(r.optString("Name",""))).put("issue",issue).put("issue_at",issueAt).put("at",at).put("seen",seen).put("seq",seq));}
        return new JSONObject().put("epoch",epoch).put("device",device).put("checked",checked).put("verified",data.getBoolean("verified")).put("rows",rows);
    }
    public static boolean newer(JSONObject prior,JSONObject next){
        if(prior==null)return true;
        if(next.optLong("checked")<prior.optLong("checked"))return false;
        if(!prior.optString("epoch").equals(next.optString("epoch")))return true;
        JSONArray left=prior.optJSONArray("rows"),right=next.optJSONArray("rows");
        long previous=left==null||left.length()==0?0:left.optJSONObject(0).optLong("seq");
        long incoming=right==null||right.length()==0?0:right.optJSONObject(0).optLong("seq");
        if(incoming<previous)return false;
        java.util.Map<String,JSONObject> oldRows=new java.util.HashMap<>();
        if(left!=null)for(int i=0;i<left.length();i++){JSONObject row=left.optJSONObject(i);if(row!=null)oldRows.put(row.optString("thread"),row);}
        if(right!=null)for(int i=0;i<right.length();i++){JSONObject row=right.optJSONObject(i),old=row==null?null:oldRows.get(row.optString("thread"));if(old==null)continue;
            if(row.optLong("seq")<old.optLong("seq")||row.optLong("at")<old.optLong("at"))return false;
            if(row.optLong("seq")==old.optLong("seq"))for(String key:new String[]{"turn","state","name","issue","issue_at","at","seen"})if(!java.util.Objects.equals(row.opt(key),old.opt(key)))return false;
        }
        return true;
    }
    public static boolean connected(JSONObject data,long received,long now){
        long checked=data==null?0:data.optLong("checked");
        return received>0&&received<=now&&now-received<=120000&&checked>0&&checked<=now+300000&&Math.abs(now-checked)<=120000;
    }
    public static String name(String value){
        String text=value==null?"":value.replaceAll("[\\p{Cntrl}\\u2028\\u2029]"," ").replaceAll("\\s+"," ").trim();
        text=text.replaceAll("(?i)(bearer\\s+\\S+|sk-[a-z0-9_-]{12,}|eyJ[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+)","[redacted]");
        return text.codePointCount(0,text.length())>80?text.substring(0,text.offsetByCodePoints(0,80))+"…":text;
    }
}
