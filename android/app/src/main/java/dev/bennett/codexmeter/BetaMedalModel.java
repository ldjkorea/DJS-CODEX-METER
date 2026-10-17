package dev.bennett.codexmeter;

import java.time.*;
import java.util.*;
import org.json.*;

/** Personal observed usage only. No XP, tier changes, inferred tokens or work assessment. */
final class BetaMedalModel {
    static final int VERSION=1;
    static final String FORMAT="gpt-hud-personal-medals";
    static final String[] HONORS={"medal.days7","medal.month1","medal.month3"};
    static final String[] TITLES={"medal.title1","medal.title3"};
    static JSONObject empty(String account,long now,boolean demo)throws JSONException {
        return new JSONObject().put("format",FORMAT).put("version",VERSION).put("account",account)
            .put("started",now).put("demo",demo).put("days",new JSONObject()).put("periods",new JSONObject())
            .put("honors",new JSONObject()).put("titles",new JSONObject()).put("equipped","");
    }
    static String policy(LedgerRecord r){return r.policy();}
    static boolean observe(JSONObject state,String account,LedgerRecord row,long now)throws JSONException {
        validate(state,account,now);
        if(row==null||!row.source.startsWith("api_")||row.at>now||now-row.at>900000||row.reset<=row.at
                ||row.at<=state.optLong("watermark"))return false;
        JSONObject days=state.getJSONObject("days"),cursor=state.optJSONObject("cursor");
        if(cursor!=null&&row.at<=cursor.optLong("at"))return false;
        LocalDate date=BetaV4.day(row.at);String key=date.toString();JSONObject day=days.optJSONObject(key);
        if(day==null){
            double reference=reference(days,date,policy(row));
            day=new JSONObject().put("policy",policy(row)).put("points",0d).put("reference",reference)
                .put("grade",0).put("active",false).put("first",row.at).put("last",row.at).put("samples",0).put("measured",false);
            days.put(key,day);
        }
        if(!day.getString("policy").equals(policy(row)))day.put("mixed",true);
        day.put("last",row.at).put("samples",Math.addExact(day.getInt("samples"),1));
        if(cursor!=null&&cursor.has("record")&&!day.optBoolean("mixed")&&!day.optBoolean("uncertain")){
            LedgerRecord before=record(cursor.getJSONObject("record"));
            LedgerAggregation.Change change=LedgerAggregation.classify(before,row,Collections.emptyList());
            if(BetaV4.day(before.at).equals(date)&&(change==LedgerAggregation.Change.CORRECTION||change==LedgerAggregation.Change.EARLY_WINDOW))day.put("uncertain",true);
            if(BetaV4.day(before.at).equals(date)&&change==LedgerAggregation.Change.NORMAL){
                java.math.BigDecimal delta=new java.math.BigDecimal(row.decimal).subtract(new java.math.BigDecimal(before.decimal));
                day.put("measured",true);
                day.put("points",java.math.BigDecimal.valueOf(day.getDouble("points")).add(delta).doubleValue());
                if(day.getDouble("points")>0){
                    day.put("active",true);if(!day.has("earned"))day.put("earned",row.at);
                    double ref=day.getDouble("reference");int grade=personalGrade(day.getDouble("points"),ref);
                    day.put("grade",Math.max(day.getInt("grade"),grade));
                }
            }
        }
        state.put("cursor",new JSONObject().put("at",row.at).put("record",json(row)));
        periods(state,date,row.at);validate(state,account,now);return true;
    }
    static double reference(JSONObject days,LocalDate date,String policy)throws JSONException {
        List<Double> values=new ArrayList<>();
        for(int i=1;i<=28&&values.size()<14;i++){
            JSONObject day=days.optJSONObject(date.minusDays(i).toString());
            if(day!=null&&!day.optBoolean("mixed")&&!day.optBoolean("uncertain")&&day.optString("policy").equals(policy)&&day.optDouble("points")>0)values.add(day.getDouble("points"));
        }
        if(values.size()<7)return 0;Collections.sort(values);int n=values.size();
        return n%2==1?values.get(n/2):(values.get(n/2-1)+values.get(n/2))/2;
    }
    static int count(JSONObject days,LocalDate from,LocalDate to){
        int count=0;for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1)){JSONObject day=days.optJSONObject(d.toString());if(day!=null&&day.optBoolean("active"))count++;}return count;
    }
    static int grade(int count,boolean month){return month?(count>=14?3:count>=10?2:count>=7?1:0):(count>=5?3:count>=4?2:count>=3?1:0);}
    static int personalGrade(double points,double reference){if(reference<=0||points<=0)return 0;java.math.BigDecimal p=java.math.BigDecimal.valueOf(points),r=java.math.BigDecimal.valueOf(reference);return p.compareTo(r)>=0?3:p.compareTo(r.multiply(new java.math.BigDecimal("0.7")))>=0?2:1;}
    static JSONObject nextPeriod(JSONObject summary,long now)throws JSONException {
        if(summary==null||!BetaV4.day(now).toString().equals(summary.optString("date")))return null;
        LocalDate date=BetaV4.day(now);boolean done=summary.optJSONObject("today")!=null&&summary.getJSONObject("today").optBoolean("active");JSONObject best=null;int remaining=Integer.MAX_VALUE;long bestDeadline=Long.MAX_VALUE;
        for(boolean month:new boolean[]{false,true}){
            int current=summary.optInt(month?"month":"week"),available=(month?date.lengthOfMonth()-date.getDayOfMonth()+1:8-date.getDayOfWeek().getValue())-(done?1:0);
            for(int target:month?new int[]{7,10,14}:new int[]{3,4,5})if(target>current){
                int needed=target-current;long deadline=(month?date.withDayOfMonth(date.lengthOfMonth()):LocalDate.parse(BetaV4.week(date)).plusDays(6)).toEpochDay();
                if(needed<=available&&(needed<remaining||needed==remaining&&deadline<bestDeadline)){remaining=needed;bestDeadline=deadline;best=new JSONObject().put("month",month).put("current",current).put("target",target).put("grade",grade(target,month));}break;
            }
        }return best;
    }
    static void periods(JSONObject state,LocalDate date,long at)throws JSONException {
        JSONObject days=state.getJSONObject("days"),periods=state.getJSONObject("periods");
        LocalDate week=LocalDate.parse(BetaV4.week(date)),month=date.withDayOfMonth(1);
        award(periods,"week:"+week,grade(count(days,week,week.plusDays(6)),false),at);
        award(periods,"month:"+YearMonth.from(date),grade(count(days,month,month.withDayOfMonth(month.lengthOfMonth())),true),at);
        int active=0;Iterator<String> dates=days.keys();while(dates.hasNext())if(days.getJSONObject(dates.next()).optBoolean("active"))active++;
        int gold=0;Iterator<String> keys=periods.keys();while(keys.hasNext()){String key=keys.next();if(key.startsWith("month:")&&periods.getJSONObject(key).getInt("grade")==3)gold++;}
        award(state.getJSONObject("honors"),HONORS[0],active>=7?1:0,at);
        for(int i=0;i<2;i++){boolean earned=gold>=(i==0?1:3);award(state.getJSONObject("honors"),HONORS[i+1],earned?1:0,at);award(state.getJSONObject("titles"),TITLES[i],earned?1:0,at);}
    }
    private static void award(JSONObject facts,String key,int grade,long at)throws JSONException {
        if(grade==0)return;JSONObject old=facts.optJSONObject(key);
        if(old==null)facts.put(key,new JSONObject().put("grade",grade).put("first",at).put("at",at));
        else if(grade>old.getInt("grade"))old.put("grade",grade).put("at",at);
    }
    static JSONObject summary(JSONObject state,long now)throws JSONException {
        LocalDate date=BetaV4.day(now),week=LocalDate.parse(BetaV4.week(date)),month=date.withDayOfMonth(1);
        JSONObject days=state.getJSONObject("days"),today=days.optJSONObject(date.toString());
        JSONObject visible=new JSONObject(),all=state.getJSONObject("periods");int gold=0;
        for(String key:new String[]{"week:"+week,"month:"+YearMonth.from(date)})if(all.has(key))visible.put(key,all.getJSONObject(key));
        Iterator<String> ids=all.keys();while(ids.hasNext()){String id=ids.next();if(id.startsWith("month:")&&all.getJSONObject(id).getInt("grade")==3)gold++;}
        return new JSONObject().put("account",state.getString("account")).put("date",date.toString()).put("demo",state.getBoolean("demo"))
            .put("today",today==null?new JSONObject():new JSONObject(today.toString()))
            .put("week",count(days,week,week.plusDays(6))).put("month",count(days,month,month.withDayOfMonth(month.lengthOfMonth())))
            .put("periods",visible).put("gold_months",gold)
            .put("honors",new JSONObject(state.getJSONObject("honors").toString())).put("titles",new JSONObject(state.getJSONObject("titles").toString()))
            .put("equipped",state.getString("equipped"));
    }
    static JSONObject merge(JSONObject current,JSONObject incoming,String account,long now)throws JSONException {
        validate(incoming,account,now);validate(current,account,now);
        if(current.getBoolean("demo")!=incoming.getBoolean("demo"))throw new IllegalArgumentException("Synthetic scope");
        JSONObject out=new JSONObject(current.toString()),days=out.getJSONObject("days"),other=incoming.getJSONObject("days");long mark=out.optLong("watermark");
        Iterator<String> keys=other.keys();while(keys.hasNext()){
            String key=keys.next();JSONObject a=days.optJSONObject(key),b=other.getJSONObject(key);mark=Math.max(mark,b.getLong("last"));
            if(a==null){days.put(key,new JSONObject(b.toString()).put("restored",true));continue;}
            boolean uncertain=a.optBoolean("uncertain")||b.optBoolean("uncertain"),mixed=a.optBoolean("mixed")||b.optBoolean("mixed");
            long first=Math.min(a.getLong("first"),b.getLong("first")),last=Math.max(a.getLong("last"),b.getLong("last"));int samples=Math.max(a.getInt("samples"),b.getInt("samples"));
            if(a.getString("policy").equals(b.getString("policy"))){
                if(b.getDouble("points")>a.getDouble("points")){JSONObject chosen=new JSONObject(b.toString());if(a.getInt("grade")>b.getInt("grade"))chosen.put("reference",a.getDouble("reference"));chosen.put("grade",Math.max(a.getInt("grade"),b.getInt("grade"))).put("restored",true);days.put(key,chosen);}
                else{if(b.getInt("grade")>a.getInt("grade"))a.put("reference",b.getDouble("reference"));a.put("grade",Math.max(a.getInt("grade"),b.getInt("grade")));}
            }else{a.put("mixed",true).put("active",a.getBoolean("active")||b.getBoolean("active")).put("grade",Math.max(a.getInt("grade"),b.getInt("grade")));}
            JSONObject merged=days.getJSONObject(key);merged.put("first",first).put("last",last).put("samples",samples);if(uncertain)merged.put("uncertain",true);if(mixed)merged.put("mixed",true);
        }
        for(String section:new String[]{"periods","honors","titles"}){
            JSONObject dest=out.getJSONObject(section),source=incoming.getJSONObject(section);Iterator<String> ids=source.keys();
            while(ids.hasNext()){String id=ids.next();JSONObject fact=source.getJSONObject(id),old=dest.optJSONObject(id);
                if(old==null)dest.put(id,new JSONObject(fact.toString()));
                else{old.put("first",Math.min(old.getLong("first"),fact.getLong("first")));if(fact.getInt("grade")>old.getInt("grade"))old.put("grade",fact.getInt("grade")).put("at",fact.getLong("at"));}
            }
        }
        // Never replay an older live cursor over restored observations or sum two copies of a day.
        long at=out.optJSONObject("cursor")==null?0:out.getJSONObject("cursor").optLong("at");
        if(mark>at){out.remove("cursor");out.put("watermark",mark);}
        if(out.getString("equipped").isEmpty()&&out.getJSONObject("titles").has(incoming.getString("equipped")))out.put("equipped",incoming.getString("equipped"));
        validate(out,account,now);return out;
    }
    static void validate(JSONObject s,String account,long now)throws JSONException {
        if(!FORMAT.equals(s.getString("format"))||s.getInt("version")!=VERSION||!account.equals(s.getString("account"))||s.getLong("started")<=0||s.getLong("started")>now)throw new IllegalArgumentException("Medal identity/version/time");
        Set<String> allowed=new HashSet<>(Arrays.asList("format","version","account","started","demo","days","periods","honors","titles","equipped","cursor","watermark"));
        Iterator<String> fields=s.keys();while(fields.hasNext())if(!allowed.contains(fields.next()))throw new IllegalArgumentException("Unknown medal field");
        s.getBoolean("demo");JSONObject days=s.getJSONObject("days");if(days.length()>36600)throw new IllegalArgumentException("Medal history bound");
        Iterator<String> dates=days.keys();while(dates.hasNext()){
            String date=dates.next();JSONObject d=days.getJSONObject(date);LocalDate parsed=LocalDate.parse(date);
            only(d,"policy","points","reference","grade","active","first","last","samples","measured","mixed","uncertain","earned","restored");
            double points=d.getDouble("points"),ref=d.getDouble("reference");int grade=d.getInt("grade");
            if(parsed.isAfter(BetaV4.day(now))||!Double.isFinite(points)||!Double.isFinite(ref)||points<0||points>1000||ref<0||ref>1000||grade<0||grade>3||d.getInt("samples")<1||d.getLong("first")>d.getLong("last")||d.getLong("last")>now||!BetaV4.day(d.getLong("first")).equals(parsed)||!BetaV4.day(d.getLong("last")).equals(parsed)||d.getString("policy").length()>150||d.getBoolean("active")!=(points>0)&&!d.optBoolean("mixed"))throw new IllegalArgumentException("Medal day");
            if(!d.optBoolean("mixed")&&grade>personalGrade(points,ref))throw new IllegalArgumentException("Medal grade evidence");
        }
        for(String section:new String[]{"periods","honors","titles"}){
            JSONObject facts=s.getJSONObject(section);if(facts.length()>10000)throw new IllegalArgumentException("Award bound");
            Iterator<String> ids=facts.keys();while(ids.hasNext()){String id=ids.next();JSONObject fact=facts.getJSONObject(id);
                only(fact,"grade","first","at");
                if(section.equals("honors")&&!Arrays.asList(HONORS).contains(id)||section.equals("titles")&&!Arrays.asList(TITLES).contains(id)||section.equals("periods")&&!id.matches("(?:week:\\d{4}-\\d{2}-\\d{2}|month:\\d{4}-\\d{2})"))throw new IllegalArgumentException("Award id");
                if(fact.getInt("grade")<1||fact.getInt("grade")>3||fact.getLong("first")<=0||fact.getLong("first")>fact.getLong("at")||fact.getLong("at")>now)throw new IllegalArgumentException("Award proof");
                if(section.equals("periods")){
                    boolean month=id.startsWith("month:");LocalDate from=month?YearMonth.parse(id.substring(6)).atDay(1):LocalDate.parse(id.substring(5));
                    if(!month&&from.getDayOfWeek()!=DayOfWeek.MONDAY)throw new IllegalArgumentException("Week boundary");
                    LocalDate to=month?from.withDayOfMonth(from.lengthOfMonth()):from.plusDays(6);
                    if(fact.getInt("grade")>grade(count(days,from,to),month))throw new IllegalArgumentException("Period evidence");
                }
            }
        }
        if(!s.getString("equipped").isEmpty()&&!s.getJSONObject("titles").has(s.getString("equipped")))throw new IllegalArgumentException("Unowned title");
        if(s.optLong("watermark")<0||s.optLong("watermark")>now)throw new IllegalArgumentException("Watermark");
        JSONObject cursor=s.optJSONObject("cursor");if(cursor!=null){only(cursor,"at","record");JSONObject r=cursor.getJSONObject("record");only(r,"meter","plan","at","used","decimal","reset","seconds","source");LedgerRecord value=record(r);if(cursor.getLong("at")!=value.at||value.at>now)throw new IllegalArgumentException("Cursor");}
    }
    private static void only(JSONObject value,String... keys){Set<String> allowed=new HashSet<>(Arrays.asList(keys));Iterator<String> names=value.keys();while(names.hasNext())if(!allowed.contains(names.next()))throw new IllegalArgumentException("Unknown medal metadata");}
    private static JSONObject json(LedgerRecord r)throws JSONException{return new JSONObject().put("meter",r.meter).put("plan",r.plan).put("at",r.at).put("used",r.used).put("decimal",r.decimal).put("reset",r.reset).put("seconds",r.seconds).put("source",r.source);}
    private static LedgerRecord record(JSONObject r)throws JSONException{return new LedgerRecord(r.getString("meter"),r.getString("plan"),r.getLong("at"),r.getDouble("used"),r.getString("decimal"),r.getLong("reset"),r.getLong("seconds"),r.getString("source"),false);}
}
