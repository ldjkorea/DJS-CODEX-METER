package dev.bennett.codexmeter;

import java.util.*;
import org.json.*;

/** Independent opt-in evidence; reuses conservative counting without awarding XP. */
public final class TitleJourney {
    public static final String KEY="title_journey";
    private TitleJourney(){}
    public static JSONObject journey(JSONObject s){return s==null?null:s.optJSONObject(KEY);}
    public static boolean enabled(JSONObject s){JSONObject j=journey(s);return j!=null&&j.optBoolean("enabled");}
    public static boolean prepare(JSONObject d,long now)throws JSONException {
        JSONObject s=RpgEngine.state(d);if(journey(s)!=null)return false;
        JSONObject shadow=new JSONObject(d.toString());JSONObject v=RpgEngine.state(shadow);v.remove(AchievementJourney.KEY);AchievementJourney.prepare(shadow,now);
        JSONObject j=AchievementJourney.journey(v),old=new JSONObject();JSONObject a=AchievementJourney.journey(s),legacy=a==null?null:a.optJSONObject("legacy");
        JSONObject medals=legacy==null?s.getJSONObject("achievements"):legacy.getJSONObject("achievements");
        for(String id:AccountProgress.keys(medals))if(medals.getJSONObject(id).optBoolean("unlocked"))old.put(id,new JSONObject(medals.getJSONObject(id).toString()));
        for(String id:AccountProgress.keys(s.getJSONObject("achievements")))if(s.getJSONObject("achievements").getJSONObject(id).optBoolean("unlocked"))old.put(id,new JSONObject(s.getJSONObject("achievements").getJSONObject(id).toString()));
        JSONObject titles=s.optJSONObject("titles");if(titles!=null)for(String id:AccountProgress.keys(titles))if(titles.getJSONObject(id).optBoolean("unlocked"))old.put(id,new JSONObject(titles.getJSONObject(id).toString()));
        j.put("legacy",old).put("facts",new JSONObject());s.put(KEY,j);
        if(!s.has("equipped_title")&&legacy!=null&&legacy.has("equipped_title")&&old.has(legacy.getString("equipped_title")))s.put("equipped_title",legacy.getString("equipped_title"));
        return true;
    }
    // Detached scope keeps achievement and title switches, baselines and pauses independent.
    private static JSONObject scope(JSONObject d)throws JSONException {JSONObject copy=new JSONObject(d.toString());RpgEngine.state(copy).put(AchievementJourney.KEY,journey(RpgEngine.state(d)));return copy;}
    public static boolean setEnabled(JSONObject d,boolean enabled,long now)throws JSONException {prepare(d,now);JSONObject copy=scope(d);return AchievementJourney.setEnabled(copy,enabled,now);}
    public static boolean capture(JSONObject d,List<LedgerRecord> rows,List<LedgerAggregation.Day> days,List<Long> credits,List<FunInsights.Window> weeks,long now)throws JSONException {if(!enabled(RpgEngine.state(d)))return false;return AchievementJourney.capture(scope(d),rows,days,credits,weeks,now);}
    public static void reconcile(JSONObject s,long now)throws JSONException {
        JSONObject j=journey(s);if(j==null||!enabled(s))return;JSONObject scoped=new JSONObject(s.toString()).put(AchievementJourney.KEY,j);AchievementJourney.tokens(scoped,now);Map<String,Long> m=AchievementJourney.metrics(scoped,s.optBoolean("quota_fresh"));JSONObject facts=j.getJSONObject("facts");
        for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject f=facts.optJSONObject(e.id);if(f==null){f=new JSONObject().put("unlocked",false);facts.put(e.id,f);}long n=m.get(e.metric);double ratio=Math.min(1,n/(double)e.target);if(e.days>0)ratio=Math.min(ratio,m.get("days")/(double)e.days);if(e.weeks>0)ratio=Math.min(ratio,m.get("weeks")/(double)e.weeks);
            f.put("progress",Math.max(f.optLong("progress"),n)).put("target",e.target).put("progress_days",m.get("days")).put("progress_weeks",m.get("weeks")).put("progress_ratio",Math.max(f.optDouble("progress_ratio",0),ratio));
            if(!f.optBoolean("unlocked")&&ratio>=1&&CollectionTierGoals.allowed(scoped,e,m))f.put("unlocked",true).put("first_earned_at",now);
        }
        JSONObject conditions=new JSONObject(s.toString()).put("activity_days",m.get("days")).put("valid_weeks",m.get("weeks"));
        // Compound title rewards explicitly require new medals acquired after title opt-in.
        JSONObject linked=j.optJSONObject("linked_medals");if(linked==null){linked=new JSONObject();j.put("linked_medals",linked);}
        for(String id:AccountProgress.keys(s.getJSONObject("achievements"))){JSONObject f=s.getJSONObject("achievements").getJSONObject(id);long at=f.optLong("first_earned_at");
            if(f.optBoolean("unlocked")&&at>=j.getLong("from")&&at<=now&&!linked.has(id))linked.put(id,at);
        }
        JSONObject medals=new JSONObject();for(String id:AccountProgress.keys(linked))medals.put(id,new JSONObject().put("unlocked",true).put("first_earned_at",linked.getLong(id)));
        if(j.optInt("collection_revision")>=CollectionRestart.REVISION)for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject f=facts.optJSONObject(e.id);if(f!=null&&f.optBoolean("unlocked"))medals.put(e.id,f);}
        conditions.put("achievements",medals);
        for(RpgTitleCatalog.Title t:RpgTitleCatalog.ALL){JSONObject f=facts.optJSONObject(t.display.id);if(f==null){f=new JSONObject().put("unlocked",false);facts.put(t.display.id,f);}double ratio=t.ratio(conditions);f.put("progress_ratio",Math.max(f.optDouble("progress_ratio",0),ratio)).put("progress",ratio>=1?1:0).put("target",1);if(!f.optBoolean("unlocked")&&ratio>=1)f.put("unlocked",true).put("first_earned_at",now);}
    }
    public static JSONObject fact(JSONObject s,String id){JSONObject j=journey(s);if(j==null)return null;JSONObject f=j.optJSONObject("facts").optJSONObject(id);return j.optInt("collection_revision")>=CollectionRestart.REVISION?f:f!=null&&f.optBoolean("unlocked")?f:j.optJSONObject("legacy").has(id)?j.optJSONObject("legacy").optJSONObject(id):f;}
    public static JSONObject view(JSONObject s)throws JSONException {JSONObject j=journey(s);if(j==null)return s;JSONObject v=new JSONObject(s.toString()),facts=new JSONObject(j.getJSONObject("facts").toString());if(j.optInt("collection_revision")<CollectionRestart.REVISION)for(String id:AccountProgress.keys(j.getJSONObject("legacy")))if(!facts.has(id)||!facts.getJSONObject(id).optBoolean("unlocked"))facts.put(id,j.getJSONObject("legacy").getJSONObject(id));v.put(AchievementJourney.KEY,j).put("achievements",facts).put("titles",facts).put("activity_days",j.getJSONObject("activity").length()).put("valid_weeks",j.getJSONObject("weeks").length());return v;}
    public static List<String> owned(JSONObject s){List<String> ids=new ArrayList<>();JSONObject j=journey(s);if(j==null)return ids;for(RpgCatalog.Entry e:RpgTitleCatalog.displayEntries()){JSONObject f=fact(s,e.id);if(f!=null&&f.optBoolean("unlocked"))ids.add(e.id);}return ids;}
    public static void merge(JSONObject own,JSONObject incoming,long now)throws JSONException {
        JSONObject b=journey(incoming);if(b==null){JSONObject a=journey(own);if(a!=null){JSONObject legacy=a.getJSONObject("legacy");for(RpgCatalog.Entry e:RpgTitleCatalog.displayEntries()){JSONObject all=incoming.optJSONObject(RpgTitleCatalog.find(e.id)==null?"achievements":"titles"),f=all==null?null:all.optJSONObject(e.id);if(f!=null&&f.optBoolean("unlocked")){long at=f.optLong("first_earned_at");if(at<=0||at>now)throw new IllegalArgumentException("Legacy title time");JSONObject previous=legacy.optJSONObject(e.id);if(previous==null||at<previous.optLong("first_earned_at"))legacy.put(e.id,new JSONObject(f.toString()));}}}return;}
        JSONObject check=new JSONObject(incoming.toString()).put(AchievementJourney.KEY,b).put("achievements",b.getJSONObject("facts"));AchievementJourney.validate(check,now);
        for(String map:new String[]{"facts","legacy"})for(String id:AccountProgress.keys(b.getJSONObject(map))){JSONObject f=b.getJSONObject(map).getJSONObject(id);double ratio=f.optDouble("progress_ratio",0);if(RpgGrowthFacts.titleEntry(id)==null||!Double.isFinite(ratio)||ratio<0||ratio>1||f.optLong("progress")<0||f.optBoolean("unlocked")&&(f.optLong("first_earned_at")<=0||f.optLong("first_earned_at")>now))throw new IllegalArgumentException("Title journal fact");}
        JSONObject evidence=b.optJSONObject("linked_medals");if(b.has("linked_medals")&&evidence==null)throw new IllegalArgumentException("Title medal evidence");
        if(evidence!=null)for(String id:AccountProgress.keys(evidence)){long at=evidence.getLong(id);if(RpgGrowthFacts.entry(id)==null||at<b.getLong("first_started_at")||at<=0||at>now)throw new IllegalArgumentException("Title medal time");}
        JSONObject a=journey(own);if(a==null){own.put(KEY,new JSONObject(b.toString()));return;}
        JSONObject previous=a.getJSONObject("legacy"),archive=b.getJSONObject("legacy");for(String id:AccountProgress.keys(archive)){JSONObject f=archive.getJSONObject(id),old=previous.optJSONObject(id);if(old==null||f.optBoolean("unlocked")&&(!old.optBoolean("unlocked")||f.optLong("first_earned_at")<old.optLong("first_earned_at")))previous.put(id,new JSONObject(f.toString()));}
        JSONObject x=new JSONObject().put(AchievementJourney.KEY,a),y=new JSONObject().put(AchievementJourney.KEY,b);if(!AchievementJourney.same(x,y))return;AchievementJourney.merge(x,y);
        if(evidence!=null){JSONObject linked=a.optJSONObject("linked_medals");if(linked==null){linked=new JSONObject();a.put("linked_medals",linked);}for(String id:AccountProgress.keys(evidence))linked.put(id,linked.has(id)?Math.min(linked.getLong(id),evidence.getLong(id)):evidence.getLong(id));}
        for(String map:new String[]{"facts","legacy"}){JSONObject dest=a.getJSONObject(map),src=b.getJSONObject(map);for(String id:AccountProgress.keys(src)){if(RpgGrowthFacts.titleEntry(id)==null)throw new IllegalArgumentException("Unknown title");JSONObject v=src.getJSONObject(id),old=dest.optJSONObject(id);if(v.optBoolean("unlocked")&&(v.optLong("first_earned_at")<=0||v.optLong("first_earned_at")>now))throw new IllegalArgumentException("Title time");if(old==null)dest.put(id,new JSONObject(v.toString()));else{old.put("progress_ratio",Math.max(old.optDouble("progress_ratio",0),v.optDouble("progress_ratio",0)));if(v.optBoolean("unlocked")){long at=old.optBoolean("unlocked")?Math.min(old.getLong("first_earned_at"),v.getLong("first_earned_at")):v.getLong("first_earned_at");old.put("unlocked",true).put("first_earned_at",at);}}}}
    }
}
