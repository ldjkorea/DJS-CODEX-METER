package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Read-only views over existing evidence and awards. No writes or reward calculation. */
final class BetaExperience {
    static JSONObject goal(JSONObject state,long now,String pinned)throws JSONException {
        String[] entry=BetaCatalog.find(pinned);
        if(entry==null||BetaCatalog.title(pinned)||state.getJSONObject("honors").has(pinned))return BetaNextGoal.choose(state,now);
        int[] progress=BetaCatalog.progress(state,pinned,now);
        return new JSONObject().put("id",pinned).put("current",progress[0]).put("target",progress[1])
            .put("metric",BetaNextGoal.metric(pinned)).put("pinned",true)
            .put("today_done",state.getJSONObject("days").has(BetaV4.day(now).toString()));
    }
    static JSONObject recap(JSONObject state,long now,boolean month)throws JSONException {
        LocalDate today=BetaV4.day(now),from=month?today.withDayOfMonth(1):LocalDate.parse(BetaV4.week(today));
        LocalDate to=month?today.withDayOfMonth(today.lengthOfMonth()):from.plusDays(6);
        long start=from.atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli(),end=to.plusDays(1).atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli();
        int days=0;JSONObject source=state.getJSONObject("days");Iterator<String> dates=source.keys();
        while(dates.hasNext()){LocalDate d=LocalDate.parse(dates.next());if(!d.isBefore(from)&&!d.isAfter(to)&&!d.isAfter(today))days++;}
        long xp=0;JSONObject rewards=state.getJSONObject("rewards");Iterator<String> keys=rewards.keys();
        while(keys.hasNext()){JSONObject reward=rewards.getJSONObject(keys.next());long at=reward.optLong("at");if(at>=start&&at<end&&at<=now)xp=Math.addExact(xp,reward.getLong("xp"));}
        JSONArray earned=new JSONArray();
        for(String kind:new String[]{"honors","titles"}){JSONObject facts=state.getJSONObject(kind);Iterator<String> ids=facts.keys();while(ids.hasNext()){String id=ids.next();long at=facts.getLong(id);if(at>=start&&at<end&&at<=now)earned.put(new JSONObject().put("id",id).put("at",at));}}
        return new JSONObject().put("from",from.toString()).put("to",to.toString()).put("days",days).put("xp",xp).put("earned",earned).put("elapsed",today.toEpochDay()-from.toEpochDay()+1);
    }
    static List<String> timeline(JSONObject state)throws JSONException {
        Map<String,Long> facts=new HashMap<>();for(String kind:new String[]{"honors","titles"}){JSONObject values=state.getJSONObject(kind);Iterator<String> ids=values.keys();while(ids.hasNext()){String id=ids.next();facts.put(id,values.getLong(id));}}
        List<String> ids=new ArrayList<>(facts.keySet());ids.sort(Comparator.<String>comparingLong(facts::get).reversed().thenComparing(Comparator.naturalOrder()));return ids;
    }
}
