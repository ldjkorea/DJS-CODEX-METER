package dev.bennett.codexmeter;

import java.math.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Versioned account document; all callers work on a copy and commit atomically. No auth fields. */
public final class AccountProgress {
    public static final int SCHEMA=1;
    public static JSONObject empty() throws JSONException {return new JSONObject().put("schema",SCHEMA).put("floor",0)
        .put("evidence",new JSONObject()).put("days",new JSONObject()).put("credits",new JSONArray()).put("billing",new JSONObject())
        .put("settlements",new JSONObject()).put("prefixes",new JSONObject()).put("heritage",new JSONObject())
        .put("tier",AccountTier.empty()).put("future",new JSONObject());}
    public static void importLegacy(JSONObject doc,String key,JSONObject fun,JSONObject tier) throws JSONException {
        JSONObject heritage=doc.getJSONObject("heritage");if(heritage.has(key))return;
        heritage.put(key,new JSONObject().put("fun",fun==null?JSONObject.NULL:fun).put("tier",tier==null?JSONObject.NULL:tier));
        JSONObject state=doc.getJSONObject("tier");
        if(fun!=null&&fun.optJSONObject("rating")!=null)seed(state,fun.getJSONObject("rating"));
        if(fun!=null&&fun.optJSONArray("settlements")!=null){JSONArray entries=fun.getJSONArray("settlements");for(int i=0;i<entries.length();i++)seedHighest(state,entries.optJSONObject(i));}
        if(tier!=null){JSONObject policies=tier.optJSONObject("policies");if(policies!=null)for(String p:keys(policies)){
            JSONObject journal=policies.optJSONObject(p);if(journal==null)continue;
            JSONObject old=journal.optJSONObject("state");if(old!=null)seed(state,old);
            JSONArray history=journal.optJSONArray("evaluations");if(history!=null)for(int i=0;i<history.length();i++)seedHighest(state,history.optJSONObject(i));
        }}
    }
    private static void seedHighest(JSONObject state,JSONObject row) throws JSONException {if(row==null)return;int rank=row.optInt("tier",-1);if(rank>=0&&rank<=9)state.put("highest",Math.max(state.getInt("highest"),rank));}
    private static void seed(JSONObject state,JSONObject row) throws JSONException {
        seedHighest(state,row);int rank=row.optInt("tier",-1);long end=row.optLong("end");
        // A newer Free/empty plan journal is not evidence of an account-wide demotion.
        if(rank>=0&&rank<=9&&end>0&&rank>state.getInt("tier")){state.put("tier",rank).put("legacy_at",end).put("protected_until",end+2*TierEvolution.WEEK);}
    }
    public static void reconcile(JSONObject doc,List<LedgerRecord> observations,List<LedgerAggregation.Day> daily,
            List<Long> credits,JSONArray bills,long floor,long now) throws Exception {
        reconcile(doc,observations,daily,credits,bills,floor,now,true);
    }
    public static void reconcile(JSONObject doc,List<LedgerRecord> observations,List<LedgerAggregation.Day> daily,
            List<Long> credits,JSONArray bills,long floor,long now,boolean evaluateWeeks) throws Exception {
        if(doc.getInt("schema")!=SCHEMA)throw new IllegalStateException("Unsupported account journal");
        if(floor>doc.getLong("floor")){
            // Explicit deletion/sign-out retains financial recaps, but prevents old raw reimport.
            JSONObject prefixes=doc.getJSONObject("prefixes"),settlements=doc.getJSONObject("settlements");
            for(String id:keys(settlements))prefixes.put(id,new JSONObject(settlements.getJSONObject(id).toString()));
            doc.put("evidence",new JSONObject()).put("days",new JSONObject()).put("credits",new JSONArray()).put("floor",floor);
        }
        long admitted=doc.getLong("floor");JSONObject mirror=doc.getJSONObject("evidence"),days=doc.getJSONObject("days");
        for(LedgerRecord r:observations)if(r.at>admitted&&r.at<=now){String id=r.meter+"@"+r.at;if(!mirror.has(id))mirror.put(id,new JSONObject().put("meter",r.meter).put("plan",r.plan).put("at",r.at).put("used",r.decimal).put("reset",r.reset).put("seconds",r.seconds).put("source",r.source));}
        for(LedgerAggregation.Day d:daily)if(d.first>admitted){String id=d.policy+"@"+d.date;JSONObject oldDay=days.optJSONObject(id);
            if(oldDay==null||d.points>=oldDay.getDouble("points"))days.put(id,new JSONObject().put("policy",d.policy).put("date",d.date).put("points",d.points).put("first",d.first).put("last",d.last).put("legacy",d.legacy));
        }
        JSONArray savedCredits=doc.getJSONArray("credits");Set<Long> creditSet=new TreeSet<>();for(int i=0;i<savedCredits.length();i++)creditSet.add(savedCredits.getLong(i));for(long c:credits)if(c>admitted)creditSet.add(c);doc.put("credits",new JSONArray(creditSet));
        List<LedgerRecord> rows=new ArrayList<>();for(String id:keys(mirror)){JSONObject r=mirror.getJSONObject(id);rows.add(new LedgerRecord(r.getString("meter"),r.getString("plan"),r.getLong("at"),new BigDecimal(r.getString("used")).doubleValue(),r.getString("used"),r.getLong("reset"),r.getLong("seconds"),r.getString("source"),false));}
        List<LedgerAggregation.Day> fallback=new ArrayList<>();for(String id:keys(days)){JSONObject r=days.getJSONObject(id);LedgerAggregation.Day d=new LedgerAggregation.Day(r.getString("policy"),r.getString("date"));d.points=r.getDouble("points");d.first=r.getLong("first");d.last=r.getLong("last");d.legacy=r.getBoolean("legacy");fallback.add(d);}
        JSONObject aliases=doc.optJSONObject("billing_aliases");if(aliases==null){aliases=new JSONObject();doc.put("billing_aliases",aliases);}
        JSONObject billing=doc.getJSONObject("billing");for(int i=0;i<bills.length();i++){
            JSONObject b=bills.getJSONObject(i);String requested=periodId(b),id=aliases.optString(requested,requested);JSONObject period=billing.optJSONObject(id);
            if(period==null){
                // An overlapping same-plan date correction is a revision, not a second subscription.
                for(String existing:keys(billing)){JSONObject prior=billing.getJSONObject(existing).getJSONObject("bill");
                    if(prior.getString("plan").equals(b.getString("plan"))&&LocalDate.parse(b.getString("start")).isBefore(LocalDate.parse(prior.getString("end")))&&LocalDate.parse(prior.getString("start")).isBefore(LocalDate.parse(b.getString("end")))){id=existing;period=billing.getJSONObject(id);break;}
                }
            }
            aliases.put(requested,id);
            if(period==null){period=new JSONObject().put("revisions",new JSONArray());billing.put(id,period);}
            JSONArray revisions=period.getJSONArray("revisions");String fingerprint=b.getString("amount")+"|"+b.getString("currency")+"|"+b.getString("start")+"|"+b.getString("end")+"|"+b.optLong("saved_at");
            boolean exists=false;for(int j=0;j<revisions.length();j++)if(revisions.getJSONObject(j).getString("fingerprint").equals(fingerprint))exists=true;
            if(!exists)revisions.put(new JSONObject(b.toString()).put("fingerprint",fingerprint));
            if(!period.has("bill")||b.optLong("saved_at")>=period.getJSONObject("bill").optLong("saved_at"))period.put("bill",new JSONObject(b.toString()));
        }
        JSONObject settlements=doc.getJSONObject("settlements");for(String id:keys(billing)){
            JSONObject b=billing.getJSONObject(id).getJSONObject("bill");SubscriptionCost cost=new SubscriptionCost(b.getString("amount"),b.getString("currency"),LocalDate.parse(b.getString("start")),LocalDate.parse(b.getString("end")));
            long start=millis(cost.start),end=millis(cost.end);Map<String,MonthlyLedger.Bucket> measured=MonthlyLedger.calculate(rows,fallback,new ArrayList<>(creditSet),start,end,admitted,now);
            // Certified old closed windows may outlive raw retention. Never add them over raw/daily evidence.
            Set<String> restoredWindows=new HashSet<>();JSONObject heritage=doc.getJSONObject("heritage");
            for(String key:keys(heritage)){
                JSONObject fun=heritage.getJSONObject(key).optJSONObject("fun");JSONArray archived=fun==null?null:fun.optJSONArray("settlements");if(archived==null)continue;
                for(int i=0;i<archived.length();i++){
                    JSONObject entry=archived.optJSONObject(i);if(entry==null||!"near_boundary_observations_no_interpolation".equals(entry.optString("quality"))||entry.optInt("observations")<3)continue;
                    String windowId=entry.optString("id");int at=windowId.lastIndexOf('@');if(at<0)continue;String policy=windowId.substring(0,at);String[] parts=policy.split("\\|");
                    if(parts.length!=3||!parts[0].equals("weekly")||!parts[1].equals(b.getString("plan"))||!parts[2].equals("604800"))continue;
                    long from=entry.optLong("start"),to=entry.optLong("end");if(to-from!=TierEvolution.WEEK||from<Math.max(start,admitted)||to>end||to>now||!restoredWindows.add(windowId))continue;
                    BigDecimal amount=new BigDecimal(entry.getString("used"));if(amount.signum()<0||amount.compareTo(BigDecimal.valueOf(100))>0)continue;
                    MonthlyLedger.Bucket represented=MonthlyLedger.calculate(rows,fallback,new ArrayList<>(creditSet),from,to,admitted,now).get(policy);
                    BigDecimal already=represented==null?BigDecimal.ZERO:represented.points;
                    MonthlyLedger.Bucket bucket=measured.get(policy);if(bucket==null){bucket=new MonthlyLedger.Bucket(policy,start,end,604800);measured.put(policy,bucket);}
                    bucket.points=bucket.points.add(amount.subtract(already).max(BigDecimal.ZERO));bucket.covered.add(new long[]{from,to});bucket.unknown.add("archived_certified_window:"+windowId);
                }
            }
            JSONObject old=settlements.optJSONObject(id),prefix=doc.getJSONObject("prefixes").optJSONObject(id),buckets=new JSONObject();
            Set<String> policies=new LinkedHashSet<>(measured.keySet());if(prefix!=null)policies.addAll(keys(prefix.getJSONObject("buckets")));if(old!=null)policies.addAll(keys(old.getJSONObject("buckets")));
            for(String policy:policies){String[] pieces=policy.split("\\|");MonthlyLedger.Bucket fresh=measured.get(policy);
                BigDecimal points=fresh==null?BigDecimal.ZERO:fresh.points;
                boolean prefixMatches=prefix!=null&&sameBounds(prefix.getJSONObject("bill"),b);
                if(prefixMatches&&prefix.getJSONObject("buckets").has(policy))points=points.add(new BigDecimal(prefix.getJSONObject("buckets").getJSONObject(policy).getString("points")));
                JSONObject prior=old==null?null:old.getJSONObject("buckets").optJSONObject(policy);if(prior!=null&&sameBounds(old.getJSONObject("bill"),b))points=points.max(new BigDecimal(prior.getString("points")));
                BigDecimal allocation=BigDecimal.valueOf(end-start).divide(BigDecimal.valueOf(Long.parseLong(pieces[2])*1000L),MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100));
                BigDecimal percent=points.multiply(BigDecimal.valueOf(100)).divide(allocation,MathContext.DECIMAL128);
                JSONArray unknown=new JSONArray();if(fresh!=null)for(String reason:fresh.unknown)unknown.put(reason);if(prefix!=null)unknown.put("deleted_source_preserved_recap");
                JSONObject v=new JSONObject().put("points",points.toPlainString()).put("allocation",allocation.toPlainString()).put("percent",percent.toPlainString()).put("unit","quota_percentage_points:"+policy).put("partial",true).put("unknown",unknown).put("rule_version",MonthlyLedger.VERSION);
                // Never combine plan-relative percentages or compare a foreign plan against this bill.
                if(prefix!=null&&!prefixMatches)unknown.put("billing_edit_deleted_evidence_unattributable");
                v.put("reference_value",pieces[1].equals(b.getString("plan"))&&policies.size()==1&&(prefix==null||prefixMatches)?cost.amount.multiply(percent).divide(BigDecimal.valueOf(100),MathContext.DECIMAL128).toPlainString():JSONObject.NULL);
                buckets.put(policy,v);
            }
            JSONObject settled=new JSONObject().put("id",id).put("bill",b).put("buckets",buckets).put("status",now>=end?"final":"provisional").put("rule_version",MonthlyLedger.VERSION).put("basis","observed_quota_lower_bound").put("mixed_policies",policies.size()>1).put("allocation_basis","billing_duration / server_window_duration; separate policy units");
            JSONArray segments=new JSONArray();Map<String,JSONObject> lastSegments=new HashMap<>();
            rows.sort(Comparator.comparingLong(row->row.at));
            for(LedgerRecord row:rows)if(row.at>=start&&row.at<end&&(row.meter.equals("weekly")||row.meter.equals("monthly"))){
                JSONObject segment=lastSegments.get(row.meter);
                if(segment==null||!segment.getString("policy").equals(row.policy())){
                    segment=new JSONObject().put("policy",row.policy()).put("first_observed",row.at).put("effective_change_time","unknown_between_observations");segments.put(segment);lastSegments.put(row.meter,segment);
                }
                segment.put("last_observed",row.at);
            }settled.put("plan_segments",segments);
            JSONArray calculations=old==null?new JSONArray():old.optJSONArray("calculations");if(calculations==null)calculations=new JSONArray();
            String fingerprint=b.toString()+":"+settled.getString("status")+":"+MonthlyLedger.VERSION;if(calculations.length()==0||!calculations.getJSONObject(calculations.length()-1).getString("fingerprint").equals(fingerprint))calculations.put(new JSONObject().put("at",now).put("fingerprint",fingerprint).put("buckets",new JSONObject(buckets.toString())));
            settled.put("calculations",calculations);settlements.put(id,settled);
        }
        // Completed evidence across plans stays account-owned, but each week retains its policy.
        List<FunInsights.Window> windows=new ArrayList<>();Set<String> policies=new HashSet<>();for(LedgerRecord r:rows)if(r.meter.equals("weekly"))policies.add(r.policy());
        long observedUntil=0;for(LedgerRecord row:rows)if(row.meter.equals("weekly"))observedUntil=Math.max(observedUntil,row.at);
        for(String policy:policies)windows.addAll(FunInsights.completed(rows,policy,Math.min(now,observedUntil)));
        if(evaluateWeeks)AccountTier.evaluate(doc.getJSONObject("tier"),windows);
        for(String id:keys(settlements)){JSONObject s=settlements.getJSONObject(id),b=s.getJSONObject("bill");if(now<millis(LocalDate.parse(b.getString("start")))||now>=millis(LocalDate.parse(b.getString("end"))))continue;
            JSONObject buckets=s.getJSONObject("buckets");for(String policy:keys(buckets)){JSONObject value=buckets.getJSONObject(policy);double percent=new BigDecimal(value.getString("percent")).doubleValue();JSONObject inputs=doc.optJSONObject("promotion_inputs");if(inputs==null){inputs=new JSONObject();doc.put("promotion_inputs",inputs);}String input=id+"|"+policy;double points=new BigDecimal(value.getString("points")).doubleValue();if(points>inputs.optDouble(input,0)){if(!s.optBoolean("mixed_policies"))AccountTier.promote(doc.getJSONObject("tier"),AccountTier.rank(percent),now);inputs.put(input,points);}}
        }
    }
    public static List<String> keys(JSONObject object){List<String> result=new ArrayList<>();Iterator<String> it=object.keys();while(it.hasNext())result.add(it.next());return result;}
    public static String periodId(JSONObject bill) throws JSONException{return bill.getString("plan")+":"+bill.getString("start")+":"+bill.getString("end");}
    private static boolean sameBounds(JSONObject a,JSONObject b) throws JSONException{return a.getString("start").equals(b.getString("start"))&&a.getString("end").equals(b.getString("end"));}
    private static long millis(LocalDate d){return d.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();}
}
