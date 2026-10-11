package dev.bennett.codexmeter;

import java.util.*;
import org.json.*;

/** Read-only presentation facts. Selecting a showcase never earns XP. */
public final class RpgGrowthFacts {
    private RpgGrowthFacts() {}
    public static RpgCatalog.Entry entry(String id) {
        for (RpgCatalog.Entry e:RpgCatalog.ALL) if(e.id.equals(id)) return e;
        return null;
    }
    public static List<String> earned(JSONObject state) {
        List<String> ids=new ArrayList<>();
        if(state==null) return ids;
        JSONArray compact=state.optJSONArray("unlocked_ids");
        JSONObject facts=state.optJSONObject("achievements");
        for(RpgCatalog.Entry e:RpgCatalog.ALL) {
            boolean unlocked=facts!=null&&facts.optJSONObject(e.id)!=null&&facts.optJSONObject(e.id).optBoolean("unlocked");
            if(compact!=null) for(int i=0;i<compact.length();i++) unlocked|=e.id.equals(compact.optString(i));
            if(unlocked) ids.add(e.id);
        }
        return ids;
    }
    public static List<String> showcase(JSONObject state,JSONArray selected,boolean customized) {
        List<String> eligible=earned(state),result=new ArrayList<>();
        if(customized) {
            if(selected!=null) for(int i=0;i<selected.length()&&result.size()<3;i++) {
                String id=selected.optString(i);
                if(eligible.contains(id)&&!result.contains(id)) result.add(id);
            }
        } else {
            eligible.sort((x,y)->Integer.compare(entry(y).bonus,entry(x).bonus));
            Set<String> categories=new HashSet<>();
            for(String id:eligible)if(result.size()<3&&categories.add(entry(id).metric))result.add(id);
            for(String id:eligible)if(result.size()<3&&!result.contains(id))result.add(id);
        }
        return result;
    }
    public static long[] xpSources(JSONObject state) {
        long[] totals=new long[4];
        JSONObject awards=state==null?null:state.optJSONObject("awards");
        if(awards!=null) for(String id:AccountProgress.keys(awards)) {
            int category=id.equals("activity-day-count")?0:id.startsWith("token-day:")?1:id.startsWith("quota-day:")||id.startsWith("valid-week:")?2:id.startsWith("achievement:")?3:-1;
            if(category>=0) totals[category]=Math.addExact(totals[category],Math.max(0,awards.optLong(id)));
        }
        return totals;
    }
    public static List<String> recentAchievements(JSONObject state) {
        List<String> ids=earned(state);JSONObject facts=state==null?null:state.optJSONObject("achievements");
        ids.removeIf(id->facts==null||facts.optJSONObject(id)==null||facts.optJSONObject(id).optLong("first_earned_at")<=0);
        ids.sort((x,y)->Long.compare(facts.optJSONObject(y).optLong("first_earned_at"),facts.optJSONObject(x).optLong("first_earned_at")));
        return ids.subList(0,Math.min(3,ids.size()));
    }
}
