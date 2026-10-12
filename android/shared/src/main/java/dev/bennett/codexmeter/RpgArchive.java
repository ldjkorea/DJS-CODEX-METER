package dev.bennett.codexmeter;

import java.math.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Validates the whole archive before returning an isolated monotone merge. */
public final class RpgArchive {
    public static final int VERSION=1;
    private static final String FORMAT="gpt-hud-account-achievements";
    private RpgArchive(){}
    public static JSONObject create(JSONObject doc,String account,JSONArray showcase,boolean custom,long now)throws Exception {
        JSONObject result=new JSONObject().put("format",FORMAT).put("version",VERSION).put("account",account).put("created_at",now)
            .put("scope","rpg,billing,evidence,showcase; no authentication").put("document",new JSONObject(doc.toString()))
            .put("showcase",new JSONObject().put("ids",showcase).put("custom",custom));
        validate(result,account,now);return result;
    }
    private static void noSecrets(Object value,int depth)throws Exception {
        if(depth>40)throw new IllegalArgumentException("Archive nesting");
        if(value instanceof JSONObject){JSONObject o=(JSONObject)value;for(String k:AccountProgress.keys(o)){
            String privateKey=k.replaceAll("[_-]","").toLowerCase(Locale.ROOT);
            if(privateKey.matches("access(token)?|refresh(token)?|idtoken|password|authorization|clientsecret|sessionsecret|pairing(secret|key|code)?|pairsecret|secret|prompt|command|conversation|sourcecode|rawlog|privatekey"))throw new IllegalArgumentException("Private content is not portable RPG data");
            noSecrets(o.get(k),depth+1);
        }}else if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++)noSecrets(a.get(i),depth+1);}
    }
    private static long integer(JSONObject o,String key)throws Exception {return new BigDecimal(o.get(key).toString()).longValueExact();}
    private static void money(JSONObject b)throws Exception {new SubscriptionCost(b.getString("amount"),b.getString("currency"),LocalDate.parse(b.getString("start")),LocalDate.parse(b.getString("end")));if(!LedgerRecord.cleanPlan(b.getString("plan")).equals(b.getString("plan")))throw new IllegalArgumentException("Billing plan");}
    private static void recap(JSONObject row,String id)throws Exception {
        if(!id.equals(row.getString("id"))||row.getInt("rule_version")!=MonthlyLedger.VERSION)throw new IllegalArgumentException("Recap version");money(row.getJSONObject("bill"));
        JSONObject buckets=row.getJSONObject("buckets");for(String p:AccountProgress.keys(buckets)){if(!p.matches("(?:weekly|monthly)\\|[a-z0-9_-]{1,40}\\|[1-9][0-9]{0,8}"))throw new IllegalArgumentException("Policy");
            JSONObject v=buckets.getJSONObject(p);for(String k:new String[]{"points","allocation","percent"})if(new BigDecimal(v.getString(k)).signum()<0)throw new IllegalArgumentException("Recap amount");
            BigDecimal a=new BigDecimal(v.getString("allocation"));if(a.signum()<=0)throw new IllegalArgumentException("Allocation");
            BigDecimal expected=new BigDecimal(v.getString("points")).multiply(BigDecimal.valueOf(100)).divide(a,MathContext.DECIMAL128);
            if(expected.subtract(new BigDecimal(v.getString("percent"))).abs().compareTo(new BigDecimal("0.000000001"))>0)throw new IllegalArgumentException("Recap ratio");
            if(!v.getString("unit").equals("quota_percentage_points:"+p)||v.getInt("rule_version")!=MonthlyLedger.VERSION)throw new IllegalArgumentException("Recap unit");v.getJSONArray("unknown");
            if(!v.isNull("reference_value")&&new BigDecimal(v.getString("reference_value")).signum()<0)throw new IllegalArgumentException("Reference");
        }
    }
    public static void validate(JSONObject file,String account,long now)throws Exception {
        if(!FORMAT.equals(file.getString("format"))||integer(file,"version")!=VERSION||!account.equals(file.getString("account"))||!account.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Archive account/version");
        long made=file.getLong("created_at");if(made<=0||made>now+300000L)throw new IllegalArgumentException("Archive time");noSecrets(file,0);
        JSONObject doc=file.getJSONObject("document");if(doc.getInt("schema")!=AccountProgress.SCHEMA||doc.getLong("floor")<0||doc.getLong("floor")>now)throw new IllegalArgumentException("Account schema");
        JSONObject r=doc.getJSONObject("future").getJSONObject("rpg");long xp=0;JSONObject awards=r.getJSONObject("awards");for(String k:AccountProgress.keys(awards))xp=Math.addExact(xp,integer(awards,k));if(xp!=integer(r,"xp")||RpgEngine.level(xp)!=r.getInt("level"))throw new IllegalArgumentException("XP provenance");
        // Reuse the journal's per-award/token/tier validation on a disposable document.
        RpgEngine.merge(AccountProgress.empty(),account,RpgEngine.export(doc,account),now);
        JSONObject ach=r.getJSONObject("achievements");if(ach.length()!=RpgCatalog.ALL.size())throw new IllegalArgumentException("Incomplete achievement catalog");for(String id:AccountProgress.keys(ach)){
            RpgCatalog.Entry e=RpgGrowthFacts.entry(id);if(e==null)throw new IllegalArgumentException("Unknown achievement");JSONObject a=ach.getJSONObject(id);
            if(!id.equals(a.getString("id"))||a.getLong("target")!=e.target||a.optLong("progress")<0||a.optInt("progress_days")<0||a.optInt("progress_weeks")<0)throw new IllegalArgumentException("Achievement fact");
            if(a.has("progress"))integer(a,"progress");if(a.has("progress_days"))integer(a,"progress_days");if(a.has("progress_weeks"))integer(a,"progress_weeks");
            if(a.getBoolean("unlocked")){if(a.getLong("first_earned_at")<=0||a.getLong("first_earned_at")>now||awards.optLong("achievement:"+id)!=e.bonus)throw new IllegalArgumentException("Achievement award");}
        }
        JSONObject evidence=doc.getJSONObject("evidence");for(String id:AccountProgress.keys(evidence)){JSONObject v=evidence.getJSONObject(id);long at=v.getLong("at");BigDecimal used=new BigDecimal(v.getString("used"));
            if(!id.equals(v.getString("meter")+"@"+at)||at<=0||at>now||used.signum()<0||used.compareTo(BigDecimal.valueOf(100))>0||v.getLong("seconds")<=0||v.getLong("reset")<=0)throw new IllegalArgumentException("Observation");
        }
        JSONObject days=doc.getJSONObject("days");for(String id:AccountProgress.keys(days)){JSONObject d=days.getJSONObject(id);LocalDate.parse(d.getString("date"));if(!id.equals(d.getString("policy")+"@"+d.getString("date"))||!Double.isFinite(d.getDouble("points"))||d.getDouble("points")<0||d.getLong("first")<=0||d.getLong("last")<d.getLong("first")||d.getLong("last")>now)throw new IllegalArgumentException("Day");}
        JSONArray credits=doc.getJSONArray("credits");for(int i=0;i<credits.length();i++)if(credits.getLong(i)<=0||credits.getLong(i)>now)throw new IllegalArgumentException("Credit");
        JSONObject billing=doc.getJSONObject("billing");for(String id:AccountProgress.keys(billing)){JSONObject p=billing.getJSONObject(id);money(p.getJSONObject("bill"));JSONArray rev=p.getJSONArray("revisions");for(int i=0;i<rev.length();i++)money(rev.getJSONObject(i));}
        for(String field:new String[]{"settlements","prefixes"}){JSONObject s=doc.getJSONObject(field);for(String id:AccountProgress.keys(s))recap(s.getJSONObject(id),id);}
        JSONObject show=file.getJSONObject("showcase");JSONArray ids=show.getJSONArray("ids");Set<String> seen=new HashSet<>();if(ids.length()>3)throw new IllegalArgumentException("Showcase limit");
        for(int i=0;i<ids.length();i++)if(!seen.add(ids.getString(i))||!RpgGrowthFacts.earned(r).contains(ids.getString(i)))throw new IllegalArgumentException("Showcase ownership");show.getBoolean("custom");
    }
    private static void recaps(JSONObject target,JSONObject incoming)throws Exception {
        for(String id:AccountProgress.keys(incoming)){JSONObject old=target.optJSONObject(id),row=incoming.getJSONObject(id);if(old==null){target.put(id,new JSONObject(row.toString()));continue;}
            // Preserve current bill edits; never add two snapshots of the same policy.
            if(!old.getJSONObject("bill").getString("start").equals(row.getJSONObject("bill").getString("start"))||!old.getJSONObject("bill").getString("end").equals(row.getJSONObject("bill").getString("end")))continue;
            JSONObject own=old.getJSONObject("buckets"),other=row.getJSONObject("buckets");for(String p:AccountProgress.keys(other)){JSONObject current=own.optJSONObject(p),v=other.getJSONObject(p);if(current==null||new BigDecimal(v.getString("points")).compareTo(new BigDecimal(current.getString("points")))>0)own.put(p,new JSONObject(v.toString()));}
            old.put("mixed_policies",own.length()>1);
            JSONObject bill=old.getJSONObject("bill");for(String p:AccountProgress.keys(own)){JSONObject v=own.getJSONObject(p);v.put("reference_value",own.length()==1&&p.split("\\|")[1].equals(bill.getString("plan"))?new BigDecimal(bill.getString("amount")).multiply(new BigDecimal(v.getString("percent"))).divide(BigDecimal.valueOf(100),MathContext.DECIMAL128).toPlainString():JSONObject.NULL);}
            JSONArray histories=old.optJSONArray("calculations"),imported=row.optJSONArray("calculations");if(histories==null){histories=new JSONArray();old.put("calculations",histories);}if(imported!=null){Set<String> seen=new HashSet<>();for(int i=0;i<histories.length();i++)seen.add(histories.getJSONObject(i).getString("fingerprint"));for(int i=0;i<imported.length();i++)if(seen.add(imported.getJSONObject(i).getString("fingerprint")))histories.put(new JSONObject(imported.getJSONObject(i).toString()));}
        }
    }
    public static JSONObject merge(JSONObject current,JSONObject file,String account,long now)throws Exception {
        validate(file,account,now);JSONObject result=new JSONObject(current.toString()),incoming=file.getJSONObject("document");
        JSONObject baseState=RpgEngine.state(result),ownJourney=AchievementJourney.journey(baseState),importedJourney=AchievementJourney.journey(RpgEngine.state(incoming));
        // Explicit, validated backup restore may recover a started collection onto a not-started device.
        if(ownJourney!=null&&ownJourney.optLong("first_started_at")==0&&importedJourney!=null&&importedJourney.optLong("first_started_at")>0){result.getJSONObject("future").put("achievement_before_restore",new JSONObject(ownJourney.toString()));baseState.remove(AchievementJourney.KEY);}
        JSONObject ownTitle=TitleJourney.journey(baseState),incomingTitle=TitleJourney.journey(RpgEngine.state(incoming));if(ownTitle!=null&&ownTitle.optLong("first_started_at")==0&&incomingTitle!=null&&incomingTitle.optLong("first_started_at")>0){result.getJSONObject("future").put("title_before_restore",new JSONObject(ownTitle.toString()));baseState.remove(TitleJourney.KEY);}
        RpgEngine.merge(result,account,RpgEngine.export(incoming,account),now);
        long floor=Math.max(result.getLong("floor"),incoming.getLong("floor"));result.put("floor",floor);
        JSONObject raw=result.getJSONObject("evidence");for(String k:AccountProgress.keys(incoming.getJSONObject("evidence"))){JSONObject row=incoming.getJSONObject("evidence").getJSONObject(k);if(row.getLong("at")<=floor)continue;
            JSONObject old=raw.optJSONObject(k);if(old!=null&&(!old.getString("used").equals(row.getString("used"))||old.getLong("reset")!=row.getLong("reset")||!old.getString("plan").equals(row.getString("plan"))))throw new IllegalArgumentException("Conflicting observation");if(old==null)raw.put(k,new JSONObject(row.toString()));}
        for(String field:new String[]{"heritage","billing_aliases"}){JSONObject a=result.optJSONObject(field),b=incoming.optJSONObject(field);if(b==null)continue;if(a==null){a=new JSONObject();result.put(field,a);}for(String k:AccountProgress.keys(b))if(!a.has(k))a.put(k,b.get(k));}
        JSONObject ownDays=result.getJSONObject("days"),incomingDays=incoming.getJSONObject("days");for(String id:AccountProgress.keys(incomingDays)){JSONObject other=incomingDays.getJSONObject(id),old=ownDays.optJSONObject(id);if(other.getLong("first")<=floor)continue;if(old==null||other.getDouble("points")>old.getDouble("points"))ownDays.put(id,new JSONObject(other.toString()));}
        JSONObject ownBills=result.getJSONObject("billing"),incomingBills=incoming.getJSONObject("billing");for(String id:AccountProgress.keys(incomingBills)){JSONObject other=incomingBills.getJSONObject(id),old=ownBills.optJSONObject(id);if(old==null){ownBills.put(id,new JSONObject(other.toString()));continue;}JSONArray revisions=old.getJSONArray("revisions"),add=other.getJSONArray("revisions");Set<String> seen=new HashSet<>();for(int i=0;i<revisions.length();i++)seen.add(revisions.getJSONObject(i).toString());for(int i=0;i<add.length();i++)if(seen.add(add.getJSONObject(i).toString()))revisions.put(new JSONObject(add.getJSONObject(i).toString()));}
        Set<Long> credits=new TreeSet<>();for(JSONObject d:new JSONObject[]{result,incoming}){JSONArray a=d.getJSONArray("credits");for(int i=0;i<a.length();i++)credits.add(a.getLong(i));}result.put("credits",new JSONArray(credits));
        recaps(result.getJSONObject("settlements"),incoming.getJSONObject("settlements"));
        // An archive captured before an explicit deletion belongs wholly to the retained prefix.
        if(file.getLong("created_at")<=floor)recaps(result.getJSONObject("prefixes"),incoming.getJSONObject("settlements"));
        // Prefixes represent removed raw evidence; only import when current account has none.
        if(current.getJSONObject("evidence").length()==0&&current.getJSONObject("prefixes").length()==0)recaps(result.getJSONObject("prefixes"),incoming.getJSONObject("prefixes"));
        return result;
    }
}
