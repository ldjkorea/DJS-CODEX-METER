package dev.bennett.codexmeter;
import org.json.JSONObject;

/** Metadata-only reaction. Unknown/stale/limited states always retain the existing safety group. */
final class BetaMedalCoach {
    static String group(JSONObject summary,String guard,long now){
        if(summary==null||CoachCatalog.constrained(guard)||!BetaV4.day(now).toString().equals(summary.optString("date")))return guard;
        JSONObject today=summary.optJSONObject("today");if(today==null||!today.optBoolean("active")||today.optBoolean("mixed")||today.optBoolean("uncertain"))return guard;
        JSONObject periods=summary.optJSONObject("periods");String date=BetaV4.day(now).toString();
        if(periods!=null)for(String key:new String[]{"month:"+java.time.YearMonth.from(BetaV4.day(now)),"week:"+BetaV4.week(BetaV4.day(now))}){
            JSONObject fact=periods.optJSONObject(key);if(fact!=null&&date.equals(BetaV4.day(fact.optLong("at")).toString()))return key.startsWith("month:")?"medal_month":"medal_week";
        }
        switch(today.optInt("grade")){case 3:return "medal_gold";case 2:return "medal_silver";case 1:return "medal_bronze";default:return "medal_record";}
    }
}
