package dev.bennett.codexmeter;

import android.content.*;
import java.util.*;
import org.json.*;

/** Account-scoped presentation preferences; earned facts stay in the account journal. */
final class RpgShowcase {
    private RpgShowcase() {}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("codex_rpg_showcase_v1",0);}
    static List<String> selected(Context c,JSONObject state){
        String account=SubscriptionStore.accountKey(c);if(account==null)return Collections.emptyList();
        JSONObject display=RpgProgressStore.peek(c),show=display==null?null:display.optJSONObject("showcase");if(show!=null)return RpgGrowthFacts.showcase(state,show.optJSONArray("ids"),show.optBoolean("custom"));
        JSONArray ids;try{ids=new JSONArray(prefs(c).getString(account+":selection","[]"));}catch(Exception e){ids=new JSONArray();}
        return RpgGrowthFacts.showcase(state,ids,prefs(c).getBoolean(account+":custom",false));
    }
    static boolean save(Context c,String account,List<String> ids,JSONObject state){
        if(account==null||!account.equals(SubscriptionStore.accountKey(c))||ids.size()>3||new HashSet<>(ids).size()!=ids.size()||!RpgGrowthFacts.earned(state).containsAll(ids))return false;
        try{if(!RpgProgressStore.saveShowcase(c,account,ids))return false;}catch(Exception e){return false;}
        prefs(c).edit().putString(account+":selection",new JSONArray(ids).toString()).putBoolean(account+":custom",true).commit();return true;
    }
    static String seenKey(String account,JSONObject s){JSONObject j=AchievementJourney.journey(s);return j==null?account+":seen":account+":journey:"+j.optString("id","not-started")+":seen";}
    // First view establishes a baseline. Historical unlocks are never replayed as new rewards.
    // Commit on the worker prevents replaying the same acquisition notification.
    @android.annotation.SuppressLint("ApplySharedPref")
    static List<String> observe(Context c,String account,JSONObject state){
        if(state==null||account==null||!account.equals(SubscriptionStore.accountKey(c)))return Collections.emptyList();
        List<String> earned=RpgGrowthFacts.earned(state),fresh=new ArrayList<>();SharedPreferences p=prefs(c);
        String key=seenKey(account,state);boolean initialized=p.contains(key);
        try{JSONArray seen=new JSONArray(p.getString(key,"[]"));Set<String> old=new HashSet<>();for(int i=0;i<seen.length();i++)old.add(seen.getString(i));
            if(initialized)for(String id:earned)if(!old.contains(id))fresh.add(id);
            old.addAll(earned);
            if(!p.edit().putString(key,new JSONArray(old).toString()).commit())return Collections.emptyList();
        }catch(Exception e){return Collections.emptyList();}
        try{String titlesKey=account+":titles_seen";boolean baseline=p.contains(titlesKey);JSONArray titles=new JSONArray(p.getString(titlesKey,"[]"));Set<String> old=new HashSet<>();for(int i=0;i<titles.length();i++)old.add(titles.getString(i));for(String id:RpgTitleCatalog.owned(state))if(RpgTitleCatalog.find(id)!=null&&old.add(id)&&baseline)fresh.add(id);if(!p.edit().putString(titlesKey,new JSONArray(old).toString()).commit())return Collections.emptyList();}catch(Exception ignored){return Collections.emptyList();}
        return TierTheme.effects(c)?fresh:Collections.emptyList();
    }
}
