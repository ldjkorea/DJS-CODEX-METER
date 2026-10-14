package dev.bennett.codexmeter;
import org.json.*;
final class BetaLegacy {
    /** Usage, token observations and grade evaluation continue; previous growth awards stop. */
    static void freezeGrowth(JSONObject previous,JSONObject current)throws JSONException{
        for(String key:new String[]{"xp","level","awards","achievements","titles","equipped_title","recommended_title","mxp_v3","achievement_journey","title_journey","unlocked_ids","unlocked_titles","unlocked_achievements","achievement_enabled","title_enabled"}){
            if(previous.has(key))current.put(key,previous.get(key));else current.remove(key);
        }
    }
}
