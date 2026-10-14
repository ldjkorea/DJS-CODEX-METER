package dev.bennett.codexmeter;
import org.json.*;
import java.util.*;

/** Stable IDs, complete Korean/English copy. Titles are templates, never identity keys. */
final class BetaCatalog {
    // id, Korean, English, Korean condition, English condition, category, rarity
    static final String[][] ALL={
        {"rpg3.first_day","첫 발자국","First Footstep","서버에서 사용량을 확인한 날 1일","Check usage from the server on 1 day","0","common"},
        {"rpg3.first_week","첫 주의 페이스","First Week Pace","한 주에 서로 다른 3일 사용량 확인","Check usage on 3 different days in one week","0","common"},
        {"rpg3.daily_7","일곱 번의 출근 도장","Seven Stamps","서로 다른 날 7일 사용량 확인 · 연속일 필요 없음","Check usage on 7 different days; no streak required","1","rare"},
        {"rpg3.weekly_4","한 달의 리듬","Monthly Rhythm","3일 이상 확인한 주를 4번 달성","Complete 4 weeks with checks on at least 3 days","1","rare"},
        {"rpg3.month_four_weeks","달력을 잇는 발자국","Across Four Weeks","같은 달의 서로 다른 4주에서 사용량 확인","Check usage in 4 distinct weeks of one calendar month","2","rare"},
        {"rpg3.month_gold_3","세 번의 황금달","Three Golden Months","14일 이상 확인한 달 3번 달성","Complete 3 months with checks on at least 14 days","2","epic"},
        {"rpg3.month_gold_6","반년의 금빛 기록","Six Golden Months","14일 이상 확인한 달 6번 달성","Complete 6 months with checks on at least 14 days","2","legendary"},
        {"rpg3.frame_30","작업대 개방","Workbench Frame","레벨 30과 누적 확인 14일 달성","Reach level 30 and 14 total check days","3","epic"},
        {"rpg3.annual_gold_1","한 바퀴의 황금 궤도","Golden Orbit","개인 연간 기간에 황금달 10번 · 시작 다음 달부터 집계","Complete 10 gold months in a personal year starting next month","3","legendary"},
        {"rpg3.annual_gold_3","세 바퀴의 기록자","Three Orbits","황금 개인 연간 기간을 3번 달성","Complete 3 gold personal years","3","mythic"},
        {"rpg3.frame_50","황금 궤도의 문장","Golden Orbit Frame","레벨 50과 황금 개인 연간 기간 1번","Reach level 50 and complete 1 gold personal year","3","mythic"},
        {"rpg3.starter","첫 도전에 응답한 %s","%s, Taking the First Step","새 성장 기록에서 사용량 확인 1일","Check usage on 1 day in the new growth record","0","common"},
        {"rpg3.title_rhythm","일상에 꾸준함을 더한 %s","%s, Building a Daily Rhythm","서로 다른 날 사용량 확인 7일","Check usage on 7 different days","1","rare"},
        {"rpg3.workbench","성장에 진심인 %s","%s, Committed to Growth","레벨 30과 사용량 확인 14일","Reach level 30 and 14 check days","1","epic"},
        {"rpg3.title_orbit","달력에 꾸준함을 새긴 %s","%s, Leaving a Mark on the Calendar","14일 이상 확인한 달 6번","Complete 6 months with at least 14 check days","2","legendary"},
        {"rpg3.title_chronicler","시간에 기록을 새긴 %s","%s, A Chronicle Across the Years","황금 개인 연간 기간 3번","Complete 3 gold personal years","3","mythic"},
        {"rpg4.days_3","세 번째 발자국","Third Footstep","서로 다른 날 사용량 확인 3일 · 연속일 필요 없음","Check usage on 3 distinct days; no streak required","0","common","days","3"},
        {"rpg4.days_14","열네 날의 리듬","Fourteen-Day Rhythm","서로 다른 날 사용량 확인 14일 · 연속일 필요 없음","Check usage on 14 distinct days; no streak required","1","rare","days","14"},
        {"rpg4.days_30","서른 날의 기록","Thirty Check Days","서로 다른 날 사용량 확인 30일 · 연속일 필요 없음","Check usage on 30 distinct days; no streak required","1","rare","days","30"},
        {"rpg4.days_60","육십 날의 발자국","Sixty Footsteps","서로 다른 날 사용량 확인 60일 · 연속일 필요 없음","Check usage on 60 distinct days; no streak required","1","epic","days","60"},
        {"rpg4.days_100","백 번의 확인","One Hundred Checks","서로 다른 날 사용량 확인 100일 · 연속일 필요 없음","Check usage on 100 distinct days; no streak required","1","epic","days","100"},
        {"rpg4.days_180","백팔십 날의 습관","One Hundred Eighty Days","서로 다른 날 사용량 확인 180일 · 연속일 필요 없음","Check usage on 180 distinct days; no streak required","3","legendary","days","180"},
        {"rpg4.days_365","삼백예순다섯 날의 기록","Three Hundred Sixty-Five Days","서로 다른 날 사용량 확인 365일 · 연속일 필요 없음","Check usage on 365 distinct days; no streak required","3","legendary","days","365"},
        {"rpg4.days_730","두 해 분량의 발자국","Seven Hundred Thirty Days","서로 다른 날 사용량 확인 730일 · 연속일 필요 없음","Check usage on 730 distinct days; no streak required","3","mythic","days","730"},
        {"rpg4.weeks_8","여덟 주의 루틴","Eight Weeks of Rhythm","서로 다른 3일 이상 확인한 주 8번","Complete 8 weeks with checks on at least 3 distinct days","1","rare","weeks","8"},
        {"rpg4.weeks_12","열두 주의 기록","Twelve Weeks of Checks","서로 다른 3일 이상 확인한 주 12번","Complete 12 weeks with checks on at least 3 distinct days","1","epic","weeks","12"},
        {"rpg4.weeks_26","스물여섯 주의 페이스","Twenty-Six Weeks","서로 다른 3일 이상 확인한 주 26번","Complete 26 weeks with checks on at least 3 distinct days","1","legendary","weeks","26"},
        {"rpg4.weeks_52","쉰두 주의 꾸준함","Fifty-Two Weeks","서로 다른 3일 이상 확인한 주 52번","Complete 52 weeks with checks on at least 3 distinct days","1","mythic","weeks","52"},
        {"rpg4.full_weeks_1","첫 다섯 날의 주간","First Five-Day Week","서로 다른 5일 이상 확인한 주 1번","Complete 1 week with checks on at least 5 distinct days","2","common","perfect","1"},
        {"rpg4.full_weeks_4","네 번의 단단한 주간","Four Five-Day Weeks","서로 다른 5일 이상 확인한 주 4번","Complete 4 weeks with checks on at least 5 distinct days","2","rare","perfect","4"},
        {"rpg4.full_weeks_12","열두 번의 주간 루틴","Twelve Five-Day Weeks","서로 다른 5일 이상 확인한 주 12번","Complete 12 weeks with checks on at least 5 distinct days","2","epic","perfect","12"},
        {"rpg4.full_weeks_26","스물여섯 번의 주간 완주","Twenty-Six Five-Day Weeks","서로 다른 5일 이상 확인한 주 26번","Complete 26 weeks with checks on at least 5 distinct days","2","legendary","perfect","26"},
        {"rpg4.bronze_month","첫 일곱 날의 달","First Seven-Day Month","서로 다른 7일 이상 확인한 달 1번","Complete 1 calendar month with checks on at least 7 days","2","common","bronze","1"},
        {"rpg4.silver_month","첫 열 날의 달","First Ten-Day Month","서로 다른 10일 이상 확인한 달 1번","Complete 1 calendar month with checks on at least 10 days","2","rare","silver","1"},
        {"rpg4.gold_month","첫 황금달","First Golden Month","서로 다른 14일 이상 확인한 달 1번","Complete 1 calendar month with checks on at least 14 days","2","rare","gold","1"},
        {"rpg4.gold_months_12","열두 번의 황금달","Twelve Golden Months","서로 다른 14일 이상 확인한 달 12번","Complete 12 calendar months with checks on at least 14 days","2","mythic","gold","12"},
        {"rpg4.level_5","레벨 5의 이정표","Level 5 Milestone","새 성장 기록에서 레벨 5 달성","Reach level 5 in the new growth record","0","common","level","5"},
        {"rpg4.level_10","레벨 10의 이정표","Level 10 Milestone","새 성장 기록에서 레벨 10 달성","Reach level 10 in the new growth record","0","common","level","10"},
        {"rpg4.level_20","레벨 20의 이정표","Level 20 Milestone","새 성장 기록에서 레벨 20 달성","Reach level 20 in the new growth record","1","rare","level","20"},
        {"rpg4.level_40","레벨 40의 이정표","Level 40 Milestone","새 성장 기록에서 레벨 40 달성","Reach level 40 in the new growth record","3","epic","level","40"},
        {"rpg4.level_60","레벨 60의 이정표","Level 60 Milestone","새 성장 기록에서 레벨 60 달성","Reach level 60 in the new growth record","3","legendary","level","60"},
        {"rpg4.level_100","레벨 100의 이정표","Level 100 Milestone","새 성장 기록에서 레벨 100 달성","Reach level 100 in the new growth record","3","mythic","level","100"},
        {"rpg4.title_days_3","첫 리듬을 찾은 %s","%s, Finding a Rhythm","서로 다른 날 사용량 확인 3일 · 연속일 필요 없음","Check usage on 3 distinct days; no streak required","0","common","days","3"},
        {"rpg4.title_days_14","꾸준함에 시동을 건 %s","%s, Starting a Steady Habit","서로 다른 날 사용량 확인 14일 · 연속일 필요 없음","Check usage on 14 distinct days; no streak required","1","rare","days","14"},
        {"rpg4.title_days_30","서른 날의 발자국을 남긴 %s","%s, Leaving Thirty Footsteps","서로 다른 날 사용량 확인 30일 · 연속일 필요 없음","Check usage on 30 distinct days; no streak required","1","rare","days","30"},
        {"rpg4.title_days_60","육십 날의 리듬을 쌓은 %s","%s, Building Sixty Days of Rhythm","서로 다른 날 사용량 확인 60일 · 연속일 필요 없음","Check usage on 60 distinct days; no streak required","1","epic","days","60"},
        {"rpg4.title_days_100","백 번의 확인을 쌓은 %s","%s, A Hundred Checks Strong","서로 다른 날 사용량 확인 100일 · 연속일 필요 없음","Check usage on 100 distinct days; no streak required","1","epic","days","100"},
        {"rpg4.title_days_180","백팔십 날의 습관을 만든 %s","%s, Building a 180-Day Habit","서로 다른 날 사용량 확인 180일 · 연속일 필요 없음","Check usage on 180 distinct days; no streak required","3","legendary","days","180"},
        {"rpg4.title_days_365","삼백예순다섯 날을 채운 %s","%s, Completing 365 Check Days","서로 다른 날 사용량 확인 365일 · 연속일 필요 없음","Check usage on 365 distinct days; no streak required","3","mythic","days","365"},
        {"rpg4.title_weeks_8","주간 리듬을 지킨 %s","%s, Keeping a Weekly Rhythm","3일 이상 확인한 주 8번","Complete 8 weeks with checks on at least 3 days","2","rare","weeks","8"},
        {"rpg4.title_weeks_26","스물여섯 주의 기록을 모은 %s","%s, Collecting Twenty-Six Weeks","3일 이상 확인한 주 26번","Complete 26 weeks with checks on at least 3 days","2","legendary","weeks","26"},
        {"rpg4.title_full_week","한 주의 루틴을 채운 %s","%s, Completing a Five-Day Week","5일 이상 확인한 주 1번","Complete 1 week with checks on at least 5 days","0","common","perfect","1"},
        {"rpg4.title_full_weeks_12","한 주를 꾸준히 채우는 %s","%s, Keeping Twelve Five-Day Weeks","5일 이상 확인한 주 12번","Complete 12 weeks with checks on at least 5 days","2","epic","perfect","12"},
        {"rpg4.title_gold_month","첫 황금달을 만든 %s","%s, Earning a First Golden Month","14일 이상 확인한 달 1번","Complete 1 months with checks on at least 14 days","0","rare","gold","1"},
        {"rpg4.title_gold_months_3","세 달의 금빛을 모은 %s","%s, Collecting Three Golden Months","14일 이상 확인한 달 3번","Complete 3 months with checks on at least 14 days","0","epic","gold","3"},
        {"rpg4.title_gold_months_12","달력에 열두 번 빛을 더한 %s","%s, Adding Twelve Golden Months","14일 이상 확인한 달 12번","Complete 12 months with checks on at least 14 days","2","mythic","gold","12"},
        {"rpg4.title_level_10","성장의 발판을 놓은 %s","%s, Building a Foundation for Growth","새 성장 기록에서 레벨 10 달성","Reach level 10 in the new growth record","3","common","level","10"},
        {"rpg4.title_level_20","꾸준함을 레벨로 보여준 %s","%s, Turning Consistency into Levels","새 성장 기록에서 레벨 20 달성","Reach level 20 in the new growth record","3","rare","level","20"},
        {"rpg4.title_level_40","다음 구간에 도착한 %s","%s, Reaching the Next Stage","새 성장 기록에서 레벨 40 달성","Reach level 40 in the new growth record","3","epic","level","40"},
        {"rpg4.title_level_50","쉰 레벨의 기록을 새긴 %s","%s, Marking Fifty Levels","새 성장 기록에서 레벨 50 달성","Reach level 50 in the new growth record","3","legendary","level","50"},
        {"rpg4.title_level_100","백 레벨의 기록을 쌓은 %s","%s, Recording One Hundred Levels","새 성장 기록에서 레벨 100 달성","Reach level 100 in the new growth record","3","mythic","level","100"}
    };
    static boolean title(String id){return id.startsWith("rpg4.title_")||id.equals("rpg3.starter")||id.equals("rpg3.workbench")||id.startsWith("rpg3.title_");}
    static String[] find(String id){for(String[] e:ALL)if(e[0].equals(id))return e;return null;}
    static String name(String id,String name,boolean ko){String[] e=find(id);if(e==null)return id;return title(id)?String.format(Locale.ROOT,e[ko?1:2],name):e[ko?1:2];}
    static int[] progress(JSONObject s,String id){return progress(s,id,System.currentTimeMillis());}
    static int[] progress(JSONObject s,String id,long now){
        String[] entry=find(id);if(entry!=null&&entry.length>7)return new int[]{metric(s,entry[7]),Integer.parseInt(entry[8])};
        int n=s.optInt("checked_days"),w=s.optInt("completed_weeks"),g=s.optInt("gold_months"),y=s.optInt("gold_years");
        if(id.equals("rpg3.first_day")||id.equals("rpg3.starter"))return new int[]{n,1};
        if(id.equals("rpg3.first_week")){JSONObject weeks=s.optJSONObject("weeks");int count=s.optJSONObject("honors").has(id)?3:weeks==null?0:weeks.optInt(BetaV4.week(BetaV4.day(now)));return new int[]{count,3};}
        if(id.equals("rpg3.daily_7")||id.equals("rpg3.title_rhythm"))return new int[]{n,7};
        if(id.equals("rpg3.weekly_4"))return new int[]{w,4};
        if(id.equals("rpg3.month_four_weeks")){Map<String,Set<String>> months=new HashMap<>();JSONObject days=s.optJSONObject("days");if(days!=null){Iterator<String> keys=days.keys();while(keys.hasNext()){String d=keys.next();if(!d.substring(0,7).equals(java.time.YearMonth.from(BetaV4.day(now)).toString()))continue;months.computeIfAbsent(d.substring(0,7),k->new HashSet<>()).add(BetaV4.week(java.time.LocalDate.parse(d)));}}int max=0;for(Set<String> value:months.values())max=Math.max(max,value.size());return new int[]{s.optJSONObject("honors").has(id)?4:max,4};}
        if(id.equals("rpg3.month_gold_3"))return new int[]{g,3};
        if(id.equals("rpg3.month_gold_6")||id.equals("rpg3.title_orbit"))return new int[]{g,6};
        if(id.equals("rpg3.annual_gold_1"))return new int[]{y,1};
        if(id.equals("rpg3.annual_gold_3")||id.equals("rpg3.title_chronicler"))return new int[]{y,3};
        if(id.equals("rpg3.frame_30")||id.equals("rpg3.workbench"))return new int[]{Math.min(s.optInt("level"),n>=14?30:0),30};
        return new int[]{Math.min(s.optInt("level"),y>=1?50:0),50};
    }
    static int count(boolean titles){int n=0;for(String[] e:ALL)if(title(e[0])==titles)n++;return n;}
    static boolean legacy(String id){return id.startsWith("rpg3.");}
    static JSONObject legacyFacts(JSONObject all)throws JSONException{JSONObject out=new JSONObject();Iterator<String> keys=all.keys();while(keys.hasNext()){String id=keys.next();if(legacy(id))out.put(id,all.get(id));}return out;}
    static int metric(JSONObject s,String m){switch(m){case "days":return s.optInt("checked_days");case "weeks":return s.optInt("completed_weeks");case "perfect":return s.optInt("perfect_weeks");case "bronze":return s.optInt("bronze_months");case "silver":return s.optInt("silver_months");case "gold":return s.optInt("gold_months");case "level":return s.optInt("level");default:throw new IllegalArgumentException("Metric");}}
    static String next(JSONObject s){return next(s,System.currentTimeMillis());}
    static String next(JSONObject s,long now){double best=-1;String target="";JSONObject earned=s.optJSONObject("honors");for(String[] e:ALL){if(title(e[0])||earned!=null&&earned.has(e[0]))continue;int[] p=progress(s,e[0],now);double ratio=Math.min(.999,p[0]/(double)p[1]);if(ratio>best){best=ratio;target=e[0];}}return target;}
}
