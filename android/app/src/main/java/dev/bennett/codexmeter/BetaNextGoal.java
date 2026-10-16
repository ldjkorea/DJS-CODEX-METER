package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.time.YearMonth;
import org.json.JSONObject;

/** Read-only ranking of existing honors. Scores rank effort, never predict an award date. */
final class BetaNextGoal {
    static JSONObject choose(JSONObject state,long now)throws org.json.JSONException{
        LocalDate date=BetaV4.day(now);JSONObject earned=state.optJSONObject("honors");
        String selected="";long best=Long.MAX_VALUE,bestDeadline=Long.MAX_VALUE;int current=0,target=0;String metric="";
        for(String[] entry:BetaCatalog.ALL){
            if(BetaCatalog.title(entry[0])||earned!=null&&earned.has(entry[0]))continue;
            int[] progress=BetaCatalog.progress(state,entry[0],now);int remaining=Math.max(0,progress[1]-progress[0]);
            String kind=entry.length>7?entry[7]:legacyMetric(entry[0]);
            long score,deadline=Long.MAX_VALUE;
            switch(kind){
                case "days":score=remaining;break;
                case "week_now":score=Math.max(1,3-state.optJSONObject("weeks").optInt(BetaV4.week(date)));deadline=date.plusDays(7-date.getDayOfWeek().getValue()).toEpochDay();if(score>deadline-date.toEpochDay()+1-(state.optJSONObject("days").has(date.toString())?1:0))score=3;break;
                case "weeks":case "perfect":{
                    int stage=kind.equals("weeks")?3:5;int count=state.optJSONObject("weeks").optInt(BetaV4.week(date));
                    int needed=stage-Math.min(count,stage);int available=8-date.getDayOfWeek().getValue()-(state.optJSONObject("days").has(date.toString())?1:0);
                    score=Math.max(1,(long)remaining*stage-(count<stage&&needed<=available?count:0));deadline=date.plusDays(7-date.getDayOfWeek().getValue()).toEpochDay();break;
                }
                case "bronze":case "silver":case "gold":{
                    int stage=kind.equals("bronze")?7:kind.equals("silver")?10:14;
                    int count=state.optJSONObject("months").optInt(YearMonth.from(date).toString());int needed=stage-Math.min(count,stage);
                    int available=date.lengthOfMonth()-date.getDayOfMonth()+1-(state.optJSONObject("days").has(date.toString())?1:0);
                    score=Math.max(1,(long)remaining*stage-(count<stage&&needed<=available?count:0));deadline=date.withDayOfMonth(date.lengthOfMonth()).toEpochDay();break;
                }
                case "level":score=Math.max(1,(long)Math.ceil(Math.max(0,BetaV4.threshold(progress[1])-state.optLong("total"))/8.0));break;
                case "month_weeks":score=remaining*7L;break;
                case "combined":{int level=entry[0].equals("rpg3.frame_30")?30:50;long xp=Math.max(0,BetaV4.threshold(level)-state.optLong("total"));long requirement=level==30?Math.max(0,14-state.optInt("checked_days")):state.optInt("gold_years")>0?0:140;score=Math.max(1,Math.max(requirement,(long)Math.ceil(xp/8.0)));break;}
                default:score=remaining*140L;break;
            }
            // Expired period opportunities remain valid lifetime goals but lose a deadline tie-break.
            if(deadline!=Long.MAX_VALUE&&deadline<=date.toEpochDay()&&score>1)deadline=Long.MAX_VALUE;
            if(score<best||score==best&&deadline<bestDeadline){selected=entry[0];best=score;bestDeadline=deadline;current=progress[0];target=progress[1];metric=kind;}
        }
        return new JSONObject().put("id",selected).put("current",current).put("target",target).put("metric",metric)
            .put("today_done",state.optJSONObject("days").has(date.toString()));
    }
    static String metric(String id){String[] entry=BetaCatalog.find(id);return entry!=null&&entry.length>7?entry[7]:legacyMetric(id);}
    private static String legacyMetric(String id){
        switch(id){case "rpg3.first_day":case "rpg3.daily_7":return "days";case "rpg3.first_week":return "week_now";
            case "rpg3.weekly_4":return "weeks";case "rpg3.month_four_weeks":return "month_weeks";
            case "rpg3.month_gold_3":case "rpg3.month_gold_6":return "gold";
            case "rpg3.frame_30":case "rpg3.frame_50":return "combined";default:return "years";}
    }
}
