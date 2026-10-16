package dev.bennett.codexmeter;

import android.content.Context;
import org.json.*;

final class BetaPreferences {
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("beta_experience_v1",0);}
    static String preset(Context c){return prefs(c).getString("preset","detail");}
    static String pinned(Context c,String account){return account==null?"":prefs(c).getString(account+":goal","");}
    static boolean pin(Context c,String account,String id){
        if(!BetaGrowthStore.current(c,account)||!id.isEmpty()&&(BetaCatalog.find(id)==null||BetaCatalog.title(id)))return false;
        boolean ok=prefs(c).edit().putString(account+":goal",id).commit();if(ok)BetaGrowthStore.refreshDisplay(c);return ok;
    }
    static JSONObject export(Context c,String account)throws JSONException {
        return new JSONObject().put("name",BetaIdentity.prefs(c).getString("name",""))
            .put("setup",BetaIdentity.done(c)).put("preset",preset(c)).put("goal",pinned(c,account))
            .put("frame",prefs(c).getString(account+":frame","")).put("tone",FunStore.tone(c)).put("showcase",c.getSharedPreferences("beta_profile_showcase",0).getString(account,"[]"));
    }
    static void validate(JSONObject p)throws JSONException {
        if(!java.util.Arrays.asList("detail","simple","growth").contains(p.getString("preset")))throw new IllegalArgumentException("Preset");
        if(!java.util.Arrays.asList("off","spicy","calm","playful").contains(p.getString("tone")))throw new IllegalArgumentException("Tone");
        String goal=p.getString("goal");if(!goal.isEmpty()&&(BetaCatalog.find(goal)==null||BetaCatalog.title(goal)))throw new IllegalArgumentException("Goal");
        if(!java.util.Arrays.asList("","rpg3.frame_30","rpg3.frame_50").contains(p.optString("frame")))throw new IllegalArgumentException("Frame");
        if(p.getString("name").length()>80)throw new IllegalArgumentException("Name");JSONArray ids=new JSONArray(p.getString("showcase"));if(ids.length()>3)throw new IllegalArgumentException("Showcase");
        for(int i=0;i<ids.length();i++)if(BetaCatalog.find(ids.getString(i))==null)throw new IllegalArgumentException("Medal");
    }
    static void restore(Context c,String account,JSONObject p)throws Exception {
        validate(p);if(!BetaGrowthStore.current(c,account))throw new IllegalStateException("Account changed");
        if(!prefs(c).edit().putString("preset",p.getString("preset")).putString(account+":goal",p.getString("goal")).putString(account+":frame",p.optString("frame")).commit())throw new java.io.IOException("Preferences save failed");
        if(!BetaIdentity.prefs(c).edit().putString("name",ProfileCard.nickname(p.getString("name"))).putBoolean("setup_done",p.getBoolean("setup")).commit())throw new java.io.IOException("Identity save failed");
        if(!FunStore.tone(c,p.getString("tone"))||!c.getSharedPreferences("beta_profile_showcase",0).edit().putString(account,p.getString("showcase")).commit())throw new java.io.IOException("Display save failed");
        BetaGrowthStore.refreshDisplay(c);
    }
}
