package dev.bennett.codexmeter;

import java.math.BigInteger;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.json.*;

/** RPG 3.0. Pure deterministic accounting; callers own the SQLite transaction and account guard. */
public final class MxpEconomy {
    public static final String KEY="mxp_v3", NAMESPACE="rpg-mxp-v3";
    public static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    private MxpEconomy() {}
    public enum EvidenceStatus { VERIFIED, SELF_REPORTED, UNKNOWN, UNSUPPORTED }
    public static long cost(int level) {
        if(level<1)throw new IllegalArgumentException("Level");
        return level<10?5:level<20?10:level<30?15:level<40?40+10L*(level-30):level<50?200+50L*(level-40):700;
    }
    public static long threshold(int level) {
        if(level<1)throw new IllegalArgumentException("Level");
        long sum=0;for(int l=1;l<Math.min(level,50);l++)sum+=cost(l);
        return Math.addExact(sum,Math.multiplyExact(Math.max(0L,level-50L),700));
    }
    public static LocalDate date(long at){return Instant.ofEpochMilli(at).atZone(ZONE).toLocalDate();}
    public static String week(LocalDate d){return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();}
    public static int weeklyTarget(int n){return n>=5?30:n>=3?20:0;}
    public static int monthlyTarget(int n){return n>=14?120:n>=10?80:n>=7?40:0;}
    public static int annualTarget(int bronze,int silver,int gold){return gold>=10?600:silver>=8?400:bronze>=6?200:0;}
    public static JSONObject state(JSONObject r){return r==null?null:r.optJSONObject(KEY);}
    private static JSONObject copy(JSONObject o)throws JSONException{return new JSONObject(o.toString());}
    private static long integer(JSONObject o,String key)throws JSONException{try{return new java.math.BigDecimal(o.get(key).toString()).longValueExact();}catch(NumberFormatException|ArithmeticException e){throw new IllegalArgumentException("Non-integral MXP field "+key,e);}}
    public static boolean prepare(JSONObject r,String account,long now)throws JSONException {
        JSONObject m=state(r);if(m!=null){if(m.getInt("version")!=3||!account.equals(m.getString("account")))throw new IllegalArgumentException("MXP account/version");if(r.getLong("xp")!=m.getJSONObject("checkpoint").getLong("legacy_xp"))throw new IllegalArgumentException("Legacy XP changed after cutover; preserve both states and stop migration");return false;}
        if(now<=0||!account.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("MXP identity/time");
        long xp=r.getLong("xp");int level=RpgEngine.level(xp);long numerator=xp-RpgEngine.threshold(level),denominator=Math.multiplyExact(100L,level);
        if(r.has("level")&&r.getInt("level")!=level)throw new IllegalArgumentException("Legacy level/XP conflict; original preserved");
        // Preserve the exact source fraction and all old facts; never regenerate old awards as MXP.
        JSONObject checkpoint=new JSONObject().put("account",account).put("cutover",now).put("legacy_xp",xp).put("level",level)
                .put("residual_numerator",numerator).put("residual_denominator",denominator);
        m=new JSONObject().put("version",3).put("namespace",NAMESPACE).put("account",account).put("checkpoint",checkpoint)
                .put("legacy",copy(r)).put("started_at",0).put("total",0).put("level",level)
                .put("days",new JSONObject()).put("evidence_ids",new JSONObject()).put("weeks",new JSONObject()).put("months",new JSONObject())
                .put("rewards",new JSONObject()).put("honors",new JSONObject()).put("titles",new JSONObject()).put("period_honors",new JSONObject()).put("month_weeks",new JSONObject())
                .put("title_months",new JSONObject()).put("title_seasons",new JSONObject()).put("title_started_at",0)
                .put("switches",new JSONArray()).put("updated_at",now);
        r.put(KEY,m);switches(m,AchievementJourney.enabled(r),TitleJourney.enabled(r),now);balance(m);return true;
    }
    public static void switches(JSONObject m,boolean achievements,boolean titles,long now)throws JSONException {
        JSONArray a=m.getJSONArray("switches");JSONObject last=a.length()==0?null:a.getJSONObject(a.length()-1);
        if(last!=null&&now<last.getLong("at"))throw new IllegalArgumentException("Clock moved before collection switch");
        if(last!=null&&last.getBoolean("achievements")==achievements&&last.getBoolean("titles")==titles)return;
        JSONObject next=new JSONObject().put("at",now).put("achievements",achievements).put("titles",titles);
        if(last!=null&&last.getLong("at")==now)a.put(a.length()-1,next);else a.put(next);
        m.put("updated_at",now);
        if(achievements&&m.getLong("started_at")==0){m.put("started_at",now);LocalDate d=date(now);m.put("season_start",(d.getDayOfMonth()==1&&now==d.atStartOfDay(ZONE).toInstant().toEpochMilli()?d:d.withDayOfMonth(1).plusMonths(1)).toString());}
        if(titles&&m.optLong("title_started_at")==0){m.put("title_started_at",now);LocalDate d=date(now);m.put("title_season_start",(d.getDayOfMonth()==1&&now==d.atStartOfDay(ZONE).toInstant().toEpochMilli()?d:d.withDayOfMonth(1).plusMonths(1)).toString());}
    }
    private static boolean enabledThroughout(JSONObject m,long from,long until,String flag)throws JSONException {
        JSONArray switches=m.getJSONArray("switches");boolean on=false;
        for(int i=0;i<switches.length();i++){JSONObject s=switches.getJSONObject(i);long at=s.getLong("at");
            if(at<=from)on=s.getBoolean(flag);else if(at<=until&&!s.getBoolean(flag))return false;else if(at>until)break;
        }return on;
    }
    /** One bounded, timestamped activity interval, not a conversation, token estimate or task success. */
    public static boolean accept(JSONObject m,String account,String id,String service,EvidenceStatus status,long from,long occurredAt,long observedAt,long now)throws JSONException {
        if(!account.equals(m.getString("account")))throw new IllegalArgumentException("Evidence account");
        long cutover=m.getJSONObject("checkpoint").getLong("cutover");
        if(status!=EvidenceStatus.VERIFIED||!service.equals("codex")||id==null||!id.matches("[a-zA-Z0-9:@._-]{1,180}"))return false;
        if(from<cutover||occurredAt<=from||observedAt<occurredAt||observedAt>now||occurredAt>now||!date(from).equals(date(occurredAt)))return false;
        JSONObject ids=m.getJSONObject("evidence_ids");String identity=from+":"+occurredAt+":"+service;if(ids.has(id)&&!identity.equals(ids.getString(id)))throw new IllegalArgumentException("Conflicting evidence ID");
        boolean core=enabledThroughout(m,from,occurredAt,"achievements"),title=enabledThroughout(m,from,occurredAt,"titles");
        String day=date(occurredAt).toString();JSONObject days=m.getJSONObject("days"),d=days.optJSONObject(day);
        if(d!=null&&(!core||d.has("core"))&&(!title||d.has("title")))return false;
        ids.put(id,identity);
        JSONObject fact=new JSONObject().put("id",id).put("service",service).put("status",status.name()).put("from",from)
                .put("occurred_at",occurredAt).put("observed_at",observedAt).put("zone","Asia/Seoul").put("scope","account")
                .put("coverage","verified_activity_interval; absence_unknown");
        if(d==null){d=new JSONObject().put("base",fact);days.put(day,d);pay(m,"day",day,"base",2,now);}
        if(core&&!d.has("core")){
            m.put("verified_days",m.optInt("verified_days")+1);
            d.put("core",fact);pay(m,"day",day,"core",4,now);
            medal(m,"day:"+day,1,now);
            if(m.getLong("started_at")==0){m.put("started_at",occurredAt);LocalDate start=date(occurredAt);long midnight=start.atStartOfDay(ZONE).toInstant().toEpochMilli();
                m.put("season_start",(start.getDayOfMonth()==1&&occurredAt==midnight?start:start.withDayOfMonth(1).plusMonths(1)).toString());}
            String w=week(date(occurredAt)),month=YearMonth.from(date(occurredAt)).toString();increment(m.getJSONObject("weeks"),w);increment(m.getJSONObject("months"),month);
            if(m.getJSONObject("weeks").getInt(w)==3)m.put("completed_weeks",m.optInt("completed_weeks")+1);
            if(m.getJSONObject("months").getInt(month)==14)m.put("gold_months",m.optInt("gold_months")+1);
            int wd=m.getJSONObject("weeks").getInt(w),md=m.getJSONObject("months").getInt(month);
            medal(m,"week:"+w,wd>=5?2:wd>=3?1:0,now);medal(m,"month:"+month,md>=14?3:md>=10?2:md>=7?1:0,now);
            medal(m,"codex-week:"+w,wd>=2?1:0,now);medal(m,"codex-month:"+month,md>=5?1:0,now);
            JSONObject monthWeeks=m.getJSONObject("month_weeks").optJSONObject(month);if(monthWeeks==null){monthWeeks=new JSONObject();m.getJSONObject("month_weeks").put(month,monthWeeks);}monthWeeks.put(w,true);
            medal(m,"four-weeks:"+month,monthWeeks.length()>=4?1:0,now);honor(m,"rpg3.month_four_weeks",monthWeeks.length()>=4,now);
            pay(m,"week",w,"core",weeklyTarget(m.getJSONObject("weeks").getInt(w)),now);
            pay(m,"month",month,"core",monthlyTarget(m.getJSONObject("months").getInt(month)),now);
        }
        if(title&&!d.has("title")){
            d.put("title",fact);m.put("title_days",m.optInt("title_days")+1);
            String month=YearMonth.from(date(occurredAt)).toString();increment(m.getJSONObject("title_months"),month);
            if(m.getJSONObject("title_months").getInt(month)==14)m.put("title_gold_months",m.optInt("title_gold_months")+1);
        }
        settle(m,now);balance(m);honors(m,now);m.put("updated_at",now);return true;
    }
    private static void increment(JSONObject counts,String key)throws JSONException{counts.put(key,counts.optInt(key)+1);}
    private static void medal(JSONObject m,String id,int stage,long now)throws JSONException{if(stage<=0)return;JSONObject medals=m.getJSONObject("period_honors"),old=medals.optJSONObject(id);if(old==null)medals.put(id,new JSONObject().put("stage",stage).put("first_earned_at",now).put("upgraded_at",now));else if(old.getInt("stage")<stage)old.put("stage",stage).put("upgraded_at",now);}
    private static String rewardKey(JSONObject m,String kind,String period,String channel)throws JSONException{return m.getString("account")+"|"+NAMESPACE+"|"+kind+"|"+period+"|"+channel;}
    private static void pay(JSONObject m,String kind,String period,String channel,long target,long now)throws JSONException {
        JSONObject rewards=m.getJSONObject("rewards");String key=rewardKey(m,kind,period,channel);JSONObject old=rewards.optJSONObject(key);long paid=old==null?0:old.getLong("target");
        if(target<=paid)return;
        JSONArray audit=old==null?new JSONArray():old.getJSONArray("audit");audit.put(new JSONObject().put("at",now).put("delta",target-paid).put("target",target));
        rewards.put(key,new JSONObject().put("kind",kind).put("period",period).put("channel",channel).put("target",target).put("audit",audit));
        if(kind.equals("year")&&target==600&&paid<600)m.put("gold_years",m.optInt("gold_years")+1);
        m.put("total",Math.addExact(m.getLong("total"),target-paid)).put("updated_at",now);
    }
    public static void settle(JSONObject m,long now)throws JSONException {
        if(m.has("title_season_start")){
            JSONObject completed=m.getJSONObject("title_seasons");
            for(LocalDate start=LocalDate.parse(m.getString("title_season_start"));!start.plusYears(1).isAfter(date(now));start=start.plusYears(1)){
                int gold=0;for(int i=0;i<12;i++)if(m.getJSONObject("title_months").optInt(YearMonth.from(start.plusMonths(i)).toString())>=14)gold++;
                if(gold>=10&&!completed.has(start.toString())){completed.put(start.toString(),true);m.put("title_gold_years",m.optInt("title_gold_years")+1).put("updated_at",now);}
            }
        }
        if(!m.has("season_start")){balance(m);honors(m,now);return;}
        LocalDate first=LocalDate.parse(m.getString("season_start")),today=date(now);JSONObject months=m.getJSONObject("months");
        // At most twelve months per season; annual awards are conditional, revisable cumulative targets.
        for(LocalDate start=first;!start.plusYears(1).isAfter(today);start=start.plusYears(1)){
            int bronze=0,silver=0,gold=0;for(int i=0;i<12;i++){int n=months.optInt(YearMonth.from(start.plusMonths(i)).toString());if(n>=7)bronze++;if(n>=10)silver++;if(n>=14)gold++;}
            pay(m,"year",start.toString(),"core",annualTarget(bronze,silver,gold),now);
            medal(m,"year:"+start,annualTarget(bronze,silver,gold)/200,now);
        }balance(m);honors(m,now);
    }
    private static void balance(JSONObject m)throws JSONException {
        JSONObject c=m.getJSONObject("checkpoint");int original=c.getInt("level");BigInteger den=BigInteger.valueOf(c.getLong("residual_denominator"));
        BigInteger credit=BigInteger.valueOf(c.getLong("residual_numerator")).multiply(BigInteger.valueOf(cost(original)));
        BigInteger all=credit.add(BigInteger.valueOf(threshold(original)+m.getLong("total")).multiply(den));
        int level=original;while(level<50&&all.compareTo(BigInteger.valueOf(threshold(level+1)).multiply(den))>=0)level++;
        if(level>=50){long extra=all.divide(den).longValueExact()-threshold(50);level=Math.toIntExact(50+extra/700);}
        BigInteger remainder=all.subtract(BigInteger.valueOf(threshold(level)).multiply(den));
        m.put("level",level).put("progress_numerator",remainder.toString()).put("progress_denominator",den.toString()).put("next_cost",cost(level));
    }
    public static double progress(JSONObject m){try{return new java.math.BigDecimal(m.getString("progress_numerator")).divide(new java.math.BigDecimal(m.getString("progress_denominator")),java.math.MathContext.DECIMAL64).doubleValue();}catch(Exception e){return 0;}}
    private static void honor(JSONObject m,String id,boolean earned,long now)throws JSONException{if(earned&&!m.getJSONObject("honors").has(id))m.getJSONObject("honors").put(id,now);}
    private static void honors(JSONObject m,long now)throws JSONException {
        int days=m.optInt("verified_days"),titleDays=m.optInt("title_days"),weeks=m.optInt("completed_weeks"),gold=m.optInt("gold_months"),annualGold=m.optInt("gold_years");
        honor(m,"rpg3.first_day",days>=1,now);honor(m,"rpg3.daily_7",days>=7,now);honor(m,"rpg3.weekly_4",weeks>=4,now);
        honor(m,"rpg3.first_week",weeks>=1,now);
        honor(m,"rpg3.month_gold_3",gold>=3,now);honor(m,"rpg3.month_gold_6",gold>=6,now);honor(m,"rpg3.annual_gold_1",annualGold>=1,now);honor(m,"rpg3.annual_gold_3",annualGold>=3,now);
        honor(m,"rpg3.frame_30",m.getInt("level")>=30&&days>=14,now);honor(m,"rpg3.frame_50",m.getInt("level")>=50&&annualGold>=1,now);
        JSONObject titles=m.getJSONObject("titles");if(titleDays>=1&&!titles.has("rpg3.starter"))titles.put("rpg3.starter",now);
        if(titleDays>=14&&m.getInt("level")>=30&&!titles.has("rpg3.workbench"))titles.put("rpg3.workbench",now);
        if(titleDays>=7&&!titles.has("rpg3.title_rhythm"))titles.put("rpg3.title_rhythm",now);
        if(m.optInt("title_gold_months")>=6&&!titles.has("rpg3.title_orbit"))titles.put("rpg3.title_orbit",now);
        if(m.optInt("title_gold_years")>=3&&!titles.has("rpg3.title_chronicler"))titles.put("rpg3.title_chronicler",now);
        m.put("verified_days",days).put("title_days",titleDays).put("completed_weeks",weeks).put("gold_months",gold).put("gold_years",annualGold);
    }
    /** Existing API observations prove a Codex activity interval, never individual turns or ChatGPT use. */
    public static void capture(JSONObject r,String account,List<LedgerRecord> records,List<Long> credits,long now)throws JSONException {
        JSONObject m=state(r);if(m==null)return;List<LedgerRecord> rows=new ArrayList<>(records);rows.sort(Comparator.comparing((LedgerRecord x)->x.meter).thenComparingLong(x->x.at));
        for(int i=1;i<rows.size();i++){LedgerRecord a=rows.get(i-1),b=rows.get(i);
            if(!a.meter.equals(b.meter)||!a.policy().equals(b.policy())||a.source.equals("legacy")||b.source.equals("legacy")||b.used<=a.used||LedgerAggregation.classify(a,b,credits)!=LedgerAggregation.Change.NORMAL)continue;
            accept(m,account,"quota:"+b.meter+":"+a.at+":"+b.at,"codex",EvidenceStatus.VERIFIED,a.at,b.at,b.at,now);
        }settle(m,now);
    }
    /** Whole-file validation reconstructs targets from evidence, never trusts an imported XP total. */
    public static void validate(JSONObject m,String account,long now)throws JSONException {
        if(m.getInt("version")!=3||!NAMESPACE.equals(m.getString("namespace"))||!account.equals(m.getString("account")))throw new IllegalArgumentException("MXP schema/account");
        JSONObject c=m.getJSONObject("checkpoint");long cut=c.getLong("cutover"),xp=c.getLong("legacy_xp"),num=c.getLong("residual_numerator"),den=c.getLong("residual_denominator");int level=c.getInt("level");
        for(String k:new String[]{"cutover","legacy_xp","level","residual_numerator","residual_denominator"})integer(c,k);
        for(String k:new String[]{"version","total","level","started_at","title_started_at","updated_at","next_cost"})integer(m,k);
        long asOf=m.getLong("updated_at");if(asOf<cut||asOf>now)throw new IllegalArgumentException("MXP snapshot time");
        if(!account.equals(c.getString("account"))||cut<=0||cut>now||xp<0||level!=RpgEngine.level(xp)||num!=xp-RpgEngine.threshold(level)||den!=100L*level||num<0||num>=den)throw new IllegalArgumentException("MXP checkpoint");
        if(m.getJSONObject("legacy").getLong("xp")!=xp)throw new IllegalArgumentException("Legacy XP preserved");
        long previous=0,firstOn=0;JSONArray sw=m.getJSONArray("switches");for(int i=0;i<sw.length();i++){JSONObject v=sw.getJSONObject(i);long at=integer(v,"at");if(at<cut||at<=previous||at>asOf)throw new IllegalArgumentException("Switch order");if(v.getBoolean("achievements")&&firstOn==0)firstOn=at;v.getBoolean("titles");previous=at;}
        if(m.getLong("started_at")!=firstOn)throw new IllegalArgumentException("Growth start cannot move");
        if(firstOn>0){LocalDate start=date(firstOn);String season=(start.getDayOfMonth()==1&&firstOn==start.atStartOfDay(ZONE).toInstant().toEpochMilli()?start:start.withDayOfMonth(1).plusMonths(1)).toString();if(!season.equals(m.getString("season_start")))throw new IllegalArgumentException("Personal season anchor");}
        long titleOn=0;for(int i=0;i<sw.length();i++)if(sw.getJSONObject(i).getBoolean("titles")){titleOn=sw.getJSONObject(i).getLong("at");break;}
        if(m.getLong("title_started_at")!=titleOn)throw new IllegalArgumentException("Title start cannot move");
        if(titleOn>0){LocalDate d=date(titleOn);String season=(d.getDayOfMonth()==1&&titleOn==d.atStartOfDay(ZONE).toInstant().toEpochMilli()?d:d.withDayOfMonth(1).plusMonths(1)).toString();if(!season.equals(m.getString("title_season_start")))throw new IllegalArgumentException("Title season anchor");}
        // An old, valid backup need not already contain seasons that ended after it was saved.
        JSONObject rebuilt=rebuild(m,asOf);if(m.getLong("total")!=rebuilt.getLong("total")||m.getInt("level")!=rebuilt.getInt("level"))throw new IllegalArgumentException("MXP total/level");
        for(String k:new String[]{"verified_days","title_days","completed_weeks","gold_months","gold_years","title_gold_months","title_gold_years","progress_numerator","progress_denominator","next_cost"})if(!m.optString(k,"0").equals(rebuilt.optString(k,"0")))throw new IllegalArgumentException("MXP derived value "+k);
        for(String field:new String[]{"honors","titles"}){JSONObject facts=m.getJSONObject(field),proof=rebuilt.getJSONObject(field);if(facts.length()!=proof.length())throw new IllegalArgumentException("MXP honor count");for(String id:AccountProgress.keys(facts))if(!proof.has(id)||facts.getLong(id)<cut||facts.getLong(id)>now)throw new IllegalArgumentException("MXP honor provenance");}
        for(String field:new String[]{"weeks","months","title_months"}){JSONObject facts=m.getJSONObject(field),proof=rebuilt.getJSONObject(field);if(facts.length()!=proof.length())throw new IllegalArgumentException("MXP period count");for(String id:AccountProgress.keys(facts))if(integer(facts,id)!=proof.getLong(id))throw new IllegalArgumentException("MXP distinct days");}
        JSONObject seasons=m.getJSONObject("title_seasons"),seasonProof=rebuilt.getJSONObject("title_seasons");if(seasons.length()!=seasonProof.length())throw new IllegalArgumentException("Title seasons");for(String id:AccountProgress.keys(seasons))if(!seasons.getBoolean(id)||!seasonProof.optBoolean(id))throw new IllegalArgumentException("Title season proof");
        JSONObject medals=m.getJSONObject("period_honors"),medalProof=rebuilt.getJSONObject("period_honors");if(medals.length()!=medalProof.length())throw new IllegalArgumentException("MXP medal count");for(String id:AccountProgress.keys(medals)){JSONObject f=medals.getJSONObject(id);if(integer(f,"stage")!=medalProof.getJSONObject(id).getLong("stage")||integer(f,"first_earned_at")<cut||integer(f,"upgraded_at")<f.getLong("first_earned_at")||f.getLong("upgraded_at")>now)throw new IllegalArgumentException("MXP medal provenance");}
        JSONObject rewards=m.getJSONObject("rewards"),expected=rebuilt.getJSONObject("rewards");if(rewards.length()!=expected.length())throw new IllegalArgumentException("MXP reward count");
        for(String key:AccountProgress.keys(rewards)){JSONObject a=rewards.getJSONObject(key),b=expected.getJSONObject(key);if(integer(a,"target")!=b.getLong("target"))throw new IllegalArgumentException("MXP reward target");long sum=0;JSONArray audit=a.getJSONArray("audit");for(int i=0;i<audit.length();i++){JSONObject item=audit.getJSONObject(i);long delta=integer(item,"delta");if(delta<=0||integer(item,"at")<cut||item.getLong("at")>now)throw new IllegalArgumentException("MXP audit");sum=Math.addExact(sum,delta);if(sum!=integer(item,"target"))throw new IllegalArgumentException("MXP audit target");}if(sum!=a.getLong("target"))throw new IllegalArgumentException("MXP audit sum");}
    }
    private static JSONObject rebuild(JSONObject source,long now)throws JSONException {
        JSONObject r=copy(source);for(String k:new String[]{"days","evidence_ids","weeks","months","rewards","honors","titles","period_honors","month_weeks","title_months","title_seasons"})r.put(k,new JSONObject());r.put("total",0);for(String k:new String[]{"verified_days","title_days","completed_weeks","gold_months","gold_years","title_gold_months","title_gold_years"})r.put(k,0);
        List<String> days=AccountProgress.keys(source.getJSONObject("days"));Collections.sort(days);
        for(String day:days){JSONObject row=source.getJSONObject("days").getJSONObject(day);for(String kind:new String[]{"base","core","title"})if(row.has(kind)){JSONObject e=row.getJSONObject(kind);
            for(String k:new String[]{"from","occurred_at","observed_at"})integer(e,k);
            if(!day.equals(date(e.getLong("occurred_at")).toString())||!"Asia/Seoul".equals(e.getString("zone"))||!"account".equals(e.getString("scope")))throw new IllegalArgumentException("Evidence calendar");
            boolean accepted=accept(r,r.getString("account"),e.getString("id"),e.getString("service"),EvidenceStatus.valueOf(e.getString("status")),e.getLong("from"),e.getLong("occurred_at"),e.getLong("observed_at"),now);
            if(!accepted&&!r.getJSONObject("days").has(day))throw new IllegalArgumentException("Invalid evidence");
        }}settle(r,now);balance(r);return r;
    }
    public static void merge(JSONObject ownRpg,JSONObject otherRpg,String account,long now)throws JSONException {
        JSONObject other=state(otherRpg),own=state(ownRpg);if(other==null)return;validate(other,account,now);
        if(otherRpg.getLong("xp")!=other.getJSONObject("checkpoint").getLong("legacy_xp"))throw new IllegalArgumentException("Archive legacy XP changed after cutover");
        if(own==null){ownRpg.put(KEY,copy(other));return;}
        if(!sameCheckpoint(own,other)){
            if(own.getLong("total")==0&&own.getJSONObject("days").length()==0&&own.getJSONObject("checkpoint").getLong("legacy_xp")==0&&own.getLong("started_at")==0){
                ownRpg.put(KEY,copy(other));ownRpg.put("xp",otherRpg.getLong("xp")).put("awards",copy(otherRpg.getJSONObject("awards"))).put("level",otherRpg.getInt("level"));return;
            }
            throw new IllegalArgumentException("Conflicting MXP checkpoints; preserve both originals, restore cancelled");
        }
        JSONObject joined=copy(own);JSONArray a=own.getJSONArray("switches"),b=other.getJSONArray("switches");
        for(int i=0;i<Math.min(a.length(),b.length());i++){JSONObject x=a.getJSONObject(i),y=b.getJSONObject(i);if(x.getLong("at")!=y.getLong("at")||x.getBoolean("achievements")!=y.getBoolean("achievements")||x.getBoolean("titles")!=y.getBoolean("titles"))throw new IllegalArgumentException("Conflicting collection switch history");}
        if(b.length()>a.length()){
            joined.put("switches",new JSONArray(b.toString()));
            for(String key:new String[]{"started_at","season_start","title_started_at","title_season_start"})if(other.has(key))joined.put(key,other.get(key));
        }
        JSONObject ds=joined.getJSONObject("days"),incoming=other.getJSONObject("days");for(String day:AccountProgress.keys(incoming)){
            JSONObject old=ds.optJSONObject(day),next=incoming.getJSONObject(day);if(old==null){ds.put(day,copy(next));continue;}
            for(String k:new String[]{"base","core","title"})if(next.has(k)&&(!old.has(k)||next.getJSONObject(k).getLong("occurred_at")<old.getJSONObject(k).getLong("occurred_at")))old.put(k,copy(next.getJSONObject(k)));
        }
        JSONObject rebuilt=rebuild(joined,now);for(String field:new String[]{"honors","titles"}){JSONObject target=rebuilt.getJSONObject(field);for(JSONObject s:new JSONObject[]{own,other})for(String id:AccountProgress.keys(s.getJSONObject(field))){long at=s.getJSONObject(field).getLong(id);if(!target.has(id)||at<=0||at>now)throw new IllegalArgumentException("Unproven MXP honor");target.put(id,Math.min(at,target.getLong(id)));}}
        JSONObject rewards=rebuilt.getJSONObject("rewards");for(String id:AccountProgress.keys(rewards)){
            JSONObject finalReward=rewards.getJSONObject(id),prior=null;for(JSONObject source:new JSONObject[]{own,other}){JSONObject candidate=source.getJSONObject("rewards").optJSONObject(id);if(candidate!=null&&(prior==null||candidate.getLong("target")>prior.getLong("target")))prior=candidate;}
            if(prior!=null){JSONArray audit=new JSONArray(prior.getJSONArray("audit").toString());long finalTarget=finalReward.getLong("target"),paid=prior.getLong("target");if(finalTarget>paid)audit.put(new JSONObject().put("at",now).put("delta",finalTarget-paid).put("target",finalTarget));finalReward.put("audit",audit);}
        }
        JSONObject medals=rebuilt.getJSONObject("period_honors");for(String id:AccountProgress.keys(medals)){JSONObject result=medals.getJSONObject(id);for(JSONObject source:new JSONObject[]{own,other}){JSONObject prior=source.getJSONObject("period_honors").optJSONObject(id);if(prior!=null){result.put("first_earned_at",Math.min(result.getLong("first_earned_at"),prior.getLong("first_earned_at")));if(result.getInt("stage")==prior.getInt("stage"))result.put("upgraded_at",Math.min(result.getLong("upgraded_at"),prior.getLong("upgraded_at")));}}}
        ownRpg.put(KEY,rebuilt);
    }
    private static boolean sameCheckpoint(JSONObject a,JSONObject b)throws JSONException {JSONObject x=a.getJSONObject("checkpoint"),y=b.getJSONObject("checkpoint");for(String k:AccountProgress.keys(x))if(!x.get(k).toString().equals(y.get(k).toString()))return false;return x.length()==y.length();}
    public static JSONObject summary(JSONObject m,long now)throws JSONException {
        JSONObject out=new JSONObject();for(String k:new String[]{"version","total","level","progress_numerator","progress_denominator","next_cost","started_at","season_start","verified_days","title_days","completed_weeks","gold_months","gold_years","honors","titles"})if(m.has(k))out.put(k,m.get(k));
        LocalDate d=date(now);JSONObject day=m.getJSONObject("days").optJSONObject(d.toString());out.put("as_of_day",d.toString()).put("day",day!=null&&day.has("core")?1:0).put("week",m.getJSONObject("weeks").optInt(week(d))).put("month",m.getJSONObject("months").optInt(YearMonth.from(d).toString()));
        out.put("day_verified",day!=null&&day.has("core"));
        if(m.has("season_start")){
            LocalDate start=LocalDate.parse(m.getString("season_start"));while(!start.plusYears(1).isAfter(d))start=start.plusYears(1);
            int bronze=0,silver=0,gold=0;for(int i=0;i<12;i++){int n=m.getJSONObject("months").optInt(YearMonth.from(start.plusMonths(i)).toString());if(n>=7)bronze++;if(n>=10)silver++;if(n>=14)gold++;}
            out.put("current_season_start",start.toString()).put("current_season_end",start.plusYears(1).toString()).put("year_bronze",bronze).put("year_silver",silver).put("year_gold",gold);
        }return out;
    }
}
