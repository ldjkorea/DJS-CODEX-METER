package dev.bennett.codexmeter;

import java.time.*;
import java.util.*;
import org.json.*;

/** Opt-in, account-owned achievement evidence. Lifetime XP and tiers remain independent. */
public final class AchievementJourney {
    public static final String KEY="achievement_journey";
    private AchievementJourney(){}
    public static JSONObject journey(JSONObject s){return s==null?null:s.optJSONObject(KEY);}
    public static boolean enabled(JSONObject s){JSONObject j=journey(s);return j!=null&&j.optBoolean("enabled");}
    public static boolean prepare(JSONObject document,long now)throws JSONException {
        JSONObject s=RpgEngine.state(document);if(journey(s)!=null){if(journey(s).getInt("version")!=1)throw new IllegalStateException("Preserve newer journey");return false;}
        JSONObject legacy=new JSONObject().put("achievements",new JSONObject(s.getJSONObject("achievements").toString()));
        for(String key:new String[]{"equipped_title","recommended_title"})if(s.has(key))legacy.put(key,s.get(key));
        JSONObject show=document.getJSONObject("future").optJSONObject("showcase");if(show!=null)legacy.put("showcase",new JSONObject(show.toString()));
        JSONObject j=new JSONObject().put("version",1).put("enabled",false).put("first_started_at",0).put("from",0).put("legacy_archived_at",now).put("legacy",legacy)
            .put("activity",new JSONObject()).put("weeks",new JSONObject()).put("plans",new JSONObject()).put("billing",new JSONObject()).put("local",new JSONObject()).put("tokens",0).put("tier",0).put("dawn",false);
        s.put(KEY,j).put("achievements",emptyFacts()).put("level",RpgEngine.level(s.getLong("xp")));
        for(String key:new String[]{"equipped_title","recommended_title"})if(RpgGrowthFacts.entry(s.optString(key))!=null)s.remove(key);
        document.getJSONObject("future").put("showcase",new JSONObject().put("ids",new JSONArray()).put("custom",false));
        return true;
    }
    private static JSONObject emptyFacts()throws JSONException {
        JSONObject out=new JSONObject();for(RpgCatalog.Entry e:RpgCatalog.ALL)out.put(e.id,new JSONObject().put("id",e.id).put("name_ko",e.ko).put("name_en",e.en).put("rarity",e.rarity).put("reward_xp",e.bonus).put("unlocked",false).put("target",e.target).put("progress",0).put("progress_days",0).put("progress_weeks",0).put("progress_ratio",0).put("required_days",e.days).put("required_weeks",e.weeks));return out;
    }
    public static boolean setEnabled(JSONObject document,boolean value,long now)throws JSONException {
        prepare(document,now);JSONObject s=RpgEngine.state(document),j=journey(s);if(j.getBoolean("enabled")==value)return false;
        j.put("enabled",value);if(value){if(j.getLong("first_started_at")==0)j.put("first_started_at",now).put("id",UUID.randomUUID().toString());j.put("from",now).put("official_armed",false).put("official_delta",0).put("local",new JSONObject()).put("segment_tokens",j.getLong("tokens"));
            int peak=-1;for(String region:new String[]{"KR","WORLD"})peak=Math.max(peak,s.getJSONObject(region).optInt("highest",-1));j.put("tier_baseline",Math.max(peak,document.getJSONObject("tier").optInt("highest",-1)));
            JSONObject official=s.getJSONObject("tokens").getJSONObject("official");long at=official.optLong("at");if(at>0&&at<=now&&now-at<=300000L&&official.has("lifetimeTokens"))j.put("official_baseline",official.getLong("lifetimeTokens")).put("official_armed",true);
            JSONObject sessions=s.getJSONObject("tokens").getJSONObject("sessions");for(String id:AccountProgress.keys(sessions)){JSONObject row=sessions.getJSONObject(id);long observed=row.optLong("at");if(observed>0&&observed<=now&&now-observed<=300000L)j.getJSONObject("local").put(id,new JSONObject().put("baseline",row.getJSONObject("total").getLong("totalTokens")).put("delta",0));}
        }return true;
    }
    public static boolean capture(JSONObject document,List<LedgerRecord> input,List<LedgerAggregation.Day> days,List<Long> credits,List<FunInsights.Window> windows,long now)throws JSONException {
        JSONObject s=RpgEngine.state(document),j=journey(s);if(j==null||!enabled(s))return false;
        String before=j.toString();long from=j.getLong("from");List<LedgerRecord> rows=new ArrayList<>(input);rows.sort(Comparator.comparing((LedgerRecord x)->x.meter).thenComparingLong(x->x.at));
        JSONObject activity=j.getJSONObject("activity"),plans=j.getJSONObject("plans"),weeks=j.getJSONObject("weeks");
        for(int i=1;i<rows.size();i++){LedgerRecord a=rows.get(i-1),b=rows.get(i);if(a.at<=from||b.at>now||a.source.equals("legacy")||b.source.equals("legacy")||!a.meter.equals(b.meter)||!LedgerAggregation.day(a.at).equals(LedgerAggregation.day(b.at))||LedgerAggregation.classify(a,b,credits)!=LedgerAggregation.Change.NORMAL||b.used<=a.used)continue;
            String day=LedgerAggregation.day(b.at).toString();if(!activity.has(day))activity.put(day,b.at);if(!b.plan.equals("unknown"))plans.put(b.plan,true);if(Instant.ofEpochMilli(b.at).atZone(LedgerAggregation.ZONE).getHour()<6)j.put("dawn",true);
        }
        for(LedgerAggregation.Day d:days)if(!d.legacy&&d.first>from&&d.last<=now&&d.points>0){if(!activity.has(d.date))activity.put(d.date,d.last);String[] p=d.policy.split("\\|");if(p.length==3&&!p[1].equals("unknown"))plans.put(p[1],true);}
        for(FunInsights.Window w:windows)if(w.span.start>from&&w.span.end<=now)weeks.put(Long.toString(w.span.end),true);
        // Same-policy actual billing denominator, with disjoint enabled segments. Never sum limits.
        JSONObject billing=document.getJSONObject("billing"),progress=j.getJSONObject("billing");for(String id:AccountProgress.keys(billing)){JSONObject bill=billing.getJSONObject(id).getJSONObject("bill");long start=LocalDate.parse(bill.getString("start")).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),end=LocalDate.parse(bill.getString("end")).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
            for(MonthlyLedger.Bucket b:MonthlyLedger.calculate(rows,days,credits,start,end,from,now).values()){String key=id+"#"+b.policy;JSONObject segments=progress.optJSONObject(key);if(segments==null){segments=new JSONObject();progress.put(key,segments);}String segment=Long.toString(from);segments.put(segment,Math.max(segments.optDouble(segment,0),b.percent().doubleValue()));}
        }
        return !before.equals(j.toString());
    }
    public static void tokens(JSONObject s,long now)throws JSONException {
        JSONObject j=journey(s);if(j==null||!enabled(s))return;long from=j.getLong("from");JSONObject token=s.getJSONObject("tokens"),official=token.getJSONObject("official");long officialDelta=j.optLong("official_delta");
        if(official.optLong("at")>from&&official.optLong("at")<=now&&official.has("lifetimeTokens")){long total=official.getLong("lifetimeTokens");if(!j.optBoolean("official_armed")){j.put("official_baseline",total).put("official_armed",true).put("official_delta",0);officialDelta=0;}else{officialDelta=Math.max(officialDelta,Math.max(0,total-j.getLong("official_baseline")));j.put("official_delta",officialDelta);}}
        JSONObject sessions=token.getJSONObject("sessions"),local=j.getJSONObject("local");long localDelta=0;for(String id:AccountProgress.keys(sessions)){JSONObject row=sessions.getJSONObject(id);if(row.getLong("at")<=from||row.getLong("at")>now)continue;long total=row.getJSONObject("total").getLong("totalTokens");JSONObject v=local.optJSONObject(id);if(v==null){v=new JSONObject().put("baseline",total).put("delta",0);local.put(id,v);}v.put("delta",Math.max(v.optLong("delta"),Math.max(0,total-v.getLong("baseline"))));}
        for(String id:AccountProgress.keys(local))localDelta=Math.addExact(localDelta,local.getJSONObject(id).getLong("delta"));
        // Two sources may describe the same work. First observation is a baseline, not past earnings.
        j.put("tokens",Math.max(j.getLong("tokens"),Math.addExact(j.optLong("segment_tokens"),Math.max(officialDelta,localDelta))));
    }
    public static Map<String,Long> metrics(JSONObject s,boolean fresh)throws JSONException {
        JSONObject j=journey(s);Map<String,Long> m=new HashMap<>();for(String k:new String[]{"tokens","days","streak","weeks","tier","utilization","plans","dawn","weekend","heritage"})m.put(k,0L);if(j==null)return m;
        List<String> dates=AccountProgress.keys(j.getJSONObject("activity"));Collections.sort(dates);long longest=0,n=0,weekend=0;LocalDate prev=null;for(String date:dates){LocalDate day=LocalDate.parse(date);n=prev!=null&&prev.plusDays(1).equals(day)?n+1:1;longest=Math.max(longest,n);prev=day;if(day.getDayOfWeek().getValue()>=6)weekend++;}
        if(enabled(s)&&fresh&&(!dates.isEmpty()||j.getLong("tokens")>0)){int peak=-1;for(String region:new String[]{"KR","WORLD"})peak=Math.max(peak,s.getJSONObject(region).optInt("highest",-1));if(peak>j.optInt("tier_baseline",9)||j.optInt("collection_revision")>=CollectionRestart.REVISION&&peak>=0)j.put("tier",Math.max(j.optLong("tier"),peak+1L));}
        double util=0;JSONObject billing=j.getJSONObject("billing");for(String id:AccountProgress.keys(billing)){double sum=0;JSONObject segments=billing.getJSONObject(id);for(String segment:AccountProgress.keys(segments))sum+=segments.getDouble(segment);util=Math.max(util,sum);}
        m.put("tokens",j.getLong("tokens"));m.put("days",(long)dates.size());m.put("streak",longest);m.put("weeks",(long)j.getJSONObject("weeks").length());m.put("tier",j.getLong("tier"));m.put("utilization",(long)Math.floor(util));m.put("plans",(long)j.getJSONObject("plans").length());m.put("dawn",j.optBoolean("dawn")?1L:0L);m.put("weekend",weekend);m.put("heritage",dates.isEmpty()?0L:1L);return m;
    }
    public static void validate(JSONObject s,long now)throws JSONException {
        JSONObject j=journey(s);if(j==null)return;if(j.has("collection_revision")&&(j.getInt("collection_revision")!=CollectionRestart.REVISION||j.getLong("reset_at")<0||j.getLong("reset_at")>now))throw new IllegalArgumentException("Collection restart version/time");if(j.getInt("version")!=1||j.getLong("first_started_at")<0||j.getLong("first_started_at")>now||j.getLong("from")<0||j.getLong("from")>now||j.getLong("tokens")<0)throw new IllegalArgumentException("Achievement journey evidence");
        if(j.optBoolean("enabled")&&(j.getLong("first_started_at")==0||j.getLong("from")<j.getLong("first_started_at")))throw new IllegalArgumentException("Achievement start");
        if(j.getLong("first_started_at")>0)UUID.fromString(j.getString("id"));
        if(j.getLong("from")<j.getLong("first_started_at")||j.optLong("segment_tokens")<0||j.optLong("segment_tokens")>j.getLong("tokens")||j.optLong("official_baseline")<0||j.optLong("official_delta")<0)throw new IllegalArgumentException("Journey counters");
        for(String k:new String[]{"activity","weeks","plans","billing","local"})j.getJSONObject(k);
        for(String key:AccountProgress.keys(j.getJSONObject("weeks"))){long end=Long.parseLong(key);if(end<=j.getLong("first_started_at")||end>now||!j.getJSONObject("weeks").getBoolean(key))throw new IllegalArgumentException("Journey week");}
        for(String key:AccountProgress.keys(j.getJSONObject("billing"))){JSONObject segments=j.getJSONObject("billing").getJSONObject(key);for(String segment:AccountProgress.keys(segments)){long from=Long.parseLong(segment);double value=segments.getDouble(segment);if(from<j.getLong("first_started_at")||from>now||!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Journey billing");}}
        for(String key:AccountProgress.keys(j.getJSONObject("local"))){JSONObject v=j.getJSONObject("local").getJSONObject(key);if(v.getLong("baseline")<0||v.getLong("delta")<0)throw new IllegalArgumentException("Journey local tokens");}
        for(String day:AccountProgress.keys(j.getJSONObject("activity"))){LocalDate.parse(day);long at=j.getJSONObject("activity").getLong(day);if(at<=j.getLong("first_started_at")||at>now)throw new IllegalArgumentException("Achievement activity");}
        for(String id:AccountProgress.keys(s.getJSONObject("achievements"))){JSONObject a=s.getJSONObject("achievements").getJSONObject(id);if(a.optBoolean("unlocked")&&(j.getLong("first_started_at")==0||a.optLong("first_earned_at")<j.getLong("first_started_at")))throw new IllegalArgumentException("Pre-start achievement");}
    }
    public static void merge(JSONObject own,JSONObject incoming)throws JSONException {
        JSONObject a=journey(own),b=journey(incoming);if(a!=null&&b!=null){JSONObject x=a.getJSONObject("legacy").optJSONObject("achievements"),y=b.getJSONObject("legacy").optJSONObject("achievements");if(x!=null&&y!=null)for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject f=y.optJSONObject(e.id),old=x.optJSONObject(e.id);if(f!=null&&(old==null||f.optBoolean("unlocked")&&(!old.optBoolean("unlocked")||f.optLong("first_earned_at")<old.optLong("first_earned_at"))))x.put(e.id,new JSONObject(f.toString()));}}
        if(!same(own,incoming))return;
        for(String k:new String[]{"activity","weeks","plans"}){JSONObject x=a.getJSONObject(k),y=b.getJSONObject(k);for(String id:AccountProgress.keys(y))if(!x.has(id))x.put(id,y.get(id));}
        JSONObject x=a.getJSONObject("billing"),y=b.getJSONObject("billing");for(String id:AccountProgress.keys(y)){JSONObject xs=x.optJSONObject(id),ys=y.getJSONObject(id);if(xs==null){x.put(id,new JSONObject(ys.toString()));continue;}for(String segment:AccountProgress.keys(ys))xs.put(segment,Math.max(xs.optDouble(segment,0),ys.getDouble(segment)));}
        for(String k:new String[]{"tokens","tier"})a.put(k,Math.max(a.getLong(k),b.getLong(k)));a.put("dawn",a.optBoolean("dawn")||b.optBoolean("dawn"));
        // Merge earned facts, not the other device's enabled switch or token baselines.
    }
    public static boolean same(JSONObject own,JSONObject other){JSONObject a=journey(own),b=journey(other);return a!=null&&b!=null&&a.optLong("first_started_at")>0&&a.optString("id").equals(b.optString("id"))&&a.optLong("first_started_at")==b.optLong("first_started_at");}
}
