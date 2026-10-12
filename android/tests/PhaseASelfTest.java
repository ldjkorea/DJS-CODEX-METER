package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Synthetic evidence; no network, phone data or changes to award thresholds. */
public final class PhaseASelfTest {
    static int checks;
    static final long D=86400000L,S=LocalDate.of(2026,10,8).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static LedgerRecord row(long at,String used,long reset){return new LedgerRecord("weekly","pro",at,Double.parseDouble(used),used,reset,604800,"api_precise",false);}
    static JSONObject bill(int days)throws Exception{return new JSONObject().put("plan","pro").put("amount","100").put("currency","USD").put("start","2026-10-08").put("end",LocalDate.of(2026,10,8).plusDays(days).toString()).put("saved_at",S);}
    static JSONObject recap(JSONObject doc,JSONObject b)throws Exception{return doc.getJSONObject("settlements").getJSONObject(AccountProgress.periodId(b));}
    static void run(JSONObject d,List<LedgerRecord> rows,JSONObject b,long now)throws Exception{AccountProgress.reconcile(d,rows,List.of(),List.of(),new JSONArray().put(b),0,now,false);}
    public static void main(String[] args)throws Exception {
        UsageWindow w=new UsageWindow(42,604800,0,(S+7*D)/1000);
        UsageSnapshot snap=new UsageSnapshot("pro",true,false,null,w,S+D);
        check(UsageEvidence.state(snap,w,false,S+D,15)==UsageEvidence.State.CONFIRMED,"Server observation");
        check(UsageEvidence.state(snap,w,true,S+D,15)==UsageEvidence.State.CACHED_AFTER_FAILURE,"Recent failure is not fresh");
        check(UsageEvidence.state(snap,w,false,S+2*D,15)==UsageEvidence.State.STALE,"Old value is old");
        check(UsageEvidence.state(new UsageSnapshot("pro",true,false,null,w,0),w,false,S+D,15)==UsageEvidence.State.UNAVAILABLE,"Unknown capture time is unverified");
        check(UsageEvidence.state(null,null,true,S+D,15)==UsageEvidence.State.UNAVAILABLE,"No cache is unavailable");
        check(UsageEvidence.state(snap,null,false,S+D,15)==UsageEvidence.State.NOT_PROVIDED,"Absent five-hour not zero");
        UsageSnapshot boundary=new UsageSnapshot("pro",true,false,null,w,S+7*D-1000);
        check(UsageEvidence.state(boundary,w,false,S+7*D,15)==UsageEvidence.State.RESET_PASSED,"Reset passage never invents zero");
        check(UsageEvidence.hasQuota(snap)&&!UsageEvidence.hasQuota(new UsageSnapshot("pro",true,false,null,null,S)),"Missing response quota differs from hidden preference");
        check(!UsageEvidence.fractional(w)&&UsageEvidence.fractional(new UsageWindow(42.123,604800,0,0)),"Fractional evidence classification");
        check(UsagePrecision.remaining(w,2).equals("58.00%"),"Existing selected precision unchanged");
        check(UsagePrecision.remaining(new UsageWindow(41.87654,604800,0,0),3).equals("58.123%"),"Same original counter rounded for remaining");
        check(OnboardingFlow.launchAction(true,false,false)==OnboardingFlow.LAUNCH_MAIN,"Completed logout skips repeat guide");
        check(OnboardingFlow.launchAction(false,true,false)==OnboardingFlow.LAUNCH_MAIN_AND_COMPLETE,"Old signed-in install not forced through setup");
        check(OnboardingFlow.launchAction(false,false,false)==OnboardingFlow.LAUNCH_ONBOARDING,"New install gets setup");
        for(int days:new int[]{28,30,31}) {
            JSONObject b=bill(days),doc=AccountProgress.empty();long reset=S+7*D;
            List<LedgerRecord> rows=new ArrayList<>(List.of(row(S+D,"80.123456789",reset)));run(doc,rows,b,S+D);
            JSONObject first=recap(doc,b);int history=first.getJSONArray("calculations").length();double points=first.getJSONObject("buckets").getJSONObject("weekly|pro|604800").getDouble("points");
            rows.add(row(reset+1000,"0",reset+7*D));run(doc,rows,b,reset+1000);
            check(recap(doc,b).getJSONObject("buckets").getJSONObject("weekly|pro|604800").getDouble("points")==points,"Reset preserves monthly amount");
            rows.add(row(reset+D,"40",reset+7*D));run(doc,rows,b,reset+D);
            JSONObject current=recap(doc,b);check(current.getJSONObject("buckets").getJSONObject("weekly|pro|604800").getDouble("points")>points,"Confirmed post-reset use adds");
            check(current.getJSONArray("calculations").length()>history,"Changed consumption has a calculation snapshot");
            int count=current.getJSONArray("calculations").length();run(doc,rows,b,reset+D);
            check(recap(doc,b).getJSONArray("calculations").length()==count,"Replay produces no duplicate calculation");
            SubscriptionCost cost=new SubscriptionCost("100","USD",LocalDate.parse(b.getString("start")),LocalDate.parse(b.getString("end")));
            check(BillingEvidence.matches(current,cost,"pro"),"Current payment matches");
            check(!BillingEvidence.matches(current,new SubscriptionCost("100","KRW",cost.start,cost.end),"pro"),"Do not relabel old currency");
            check(!BillingEvidence.matches(current,new SubscriptionCost("200","USD",cost.start,cost.end),"pro"),"Edited payment waits for recalculation");
            check(!BillingEvidence.matches(current,cost,"free"),"Other plan not current payment");
            run(doc,rows,b,S+(days+1)*D);check(recap(doc,b).getString("status").equals("final"),"Closed period retained");
        }
        JSONObject b=bill(30),doc=AccountProgress.empty();List<LedgerRecord> mixed=List.of(row(S+D,"80",S+7*D),new LedgerRecord("weekly","free",S+2*D,10,"10",S+9*D,604800,"api_precise",false));run(doc,mixed,b,S+2*D);
        check(recap(doc,b).optBoolean("mixed_policies"),"Policy buckets not summed");
        for(String policy:AccountProgress.keys(recap(doc,b).getJSONObject("buckets")))check(recap(doc,b).getJSONObject("buckets").getJSONObject(policy).isNull("reference_value"),"No mixed-policy money");
        System.out.println("Phase A evidence/payment/onboarding: "+checks+" checks passed; synthetic only.");
    }
}
