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
        {"rpg3.title_chronicler","시간에 기록을 새긴 %s","%s, A Chronicle Across the Years","황금 개인 연간 기간 3번","Complete 3 gold personal years","3","mythic"}
    };
    static boolean title(String id){return id.equals("rpg3.starter")||id.equals("rpg3.workbench")||id.startsWith("rpg3.title_");}
    static String[] find(String id){for(String[] e:ALL)if(e[0].equals(id))return e;return null;}
    static String name(String id,String name,boolean ko){String[] e=find(id);if(e==null)return id;return title(id)?String.format(Locale.ROOT,e[ko?1:2],name):e[ko?1:2];}
    static int[] progress(JSONObject s,String id){return progress(s,id,System.currentTimeMillis());}
    static int[] progress(JSONObject s,String id,long now){
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
    static String next(JSONObject s){return next(s,System.currentTimeMillis());}
    static String next(JSONObject s,long now){double best=-1;String target="";JSONObject earned=s.optJSONObject("honors");for(String[] e:ALL){if(title(e[0])||earned!=null&&earned.has(e[0]))continue;int[] p=progress(s,e[0],now);double ratio=Math.min(.999,p[0]/(double)p[1]);if(ratio>best){best=ratio;target=e[0];}}return target;}
}
