package dev.bennett.codexmeter;

import java.util.Map;
import org.json.JSONObject;

/** Fixed tier prerequisites for the restarted collection; never alters observed tokens. */
public final class CollectionTierGoals {
    private CollectionTierGoals() {}
    public static int required(RpgCatalog.Entry e) {
        if (e.metric.equals("tier")) return (int)Math.min(9, e.target - 1);
        if (e.metric.equals("tokens")) {
            long n=e.target;
            return n>=1_000_000_000_000L?9:n>=100_000_000_000L?8:n>=10_000_000_000L?7:
                    n>=5_000_000_000L?6:n>=1_000_000_000L?4:n>=500_000_000L?3:n>=100_000_000L?2:n>=10_000_000L?1:0;
        }
        return e.rarity.equals("legendary")?7:e.rarity.equals("epic")?4:e.rarity.equals("mythic")?9:0;
    }
    public static boolean allowed(JSONObject state,RpgCatalog.Entry e,Map<String,Long> metrics) {
        JSONObject j=AchievementJourney.journey(state);
        if(j==null||j.optInt("collection_revision")<CollectionRestart.REVISION)return true;
        return required(e)==0 || metrics.getOrDefault("tier",0L)>=required(e)+1L;
    }
}
