package dev.bennett.codexmeter;

import java.math.*;import java.time.*;import java.util.*;import org.json.*;

/** Synthetic observations only; never reads a user's phone, account or authentication. */
public final class RpgStep1SelfTest {
    static int checks;static final long D=86400000L,W=7*D,S=LocalDate.of(2026,10,8).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
    static final String P="weekly|pro|604800";
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static void near(double a,double b,String s){check(Math.abs(a-b)<1e-8,s+": "+b);}
    static LedgerRecord row(long at,String used,long reset){return new LedgerRecord("weekly","pro",at,Double.parseDouble(used),used,reset,604800,"api_precise",false);}
    static JSONObject bill(int days,String amount)throws Exception{return new JSONObject().put("plan","pro").put("amount",amount).put("currency","USD").put("start","2026-10-08").put("end",LocalDate.of(2026,10,8).plusDays(days).toString()).put("saved_at",S);}
    static JSONObject bucket(JSONObject doc,JSONObject bill)throws Exception{return doc.getJSONObject("settlements").getJSONObject(AccountProgress.periodId(bill)).getJSONObject("buckets").getJSONObject(P);}
    static void run(JSONObject doc,List<LedgerRecord> rows,List<Long> credits,JSONArray bills,long floor,long now)throws Exception{AccountProgress.reconcile(doc,rows,List.of(),credits,bills,floor,now);}
    static List<FunInsights.Window> weeks(long end,double... used){List<FunInsights.Window> result=new ArrayList<>();for(int i=0;i<used.length;i++){long e=end-(used.length-1-i)*W;LedgerRecord r=row(e-1000,Double.toString(used[i]),e);result.add(new FunInsights.Window(P,LedgerPeriods.span(e,604800,e-1000),r,40));}return result;}
    public static void main(String[] args)throws Exception{
        JSONObject b=bill(28,"100"),doc=AccountProgress.empty();JSONArray bills=new JSONArray().put(b);
        List<LedgerRecord> rows=new ArrayList<>(List.of(row(S+D,"80.123456789",S+W)));
        run(doc,rows,List.of(),bills,0,S+D);near(80.123456789,new BigDecimal(bucket(doc,b).getString("points")).doubleValue(),"Original server precision");
        int tier=doc.getJSONObject("tier").getInt("tier");
        rows.add(row(S+2*D,"0",S+W+D));run(doc,rows,List.of(),bills,0,S+2*D);near(80.123456789,bucket(doc,b).getDouble("points"),"Unexplained global reset preserves confirmed consumption");check(doc.getJSONObject("tier").getInt("tier")==tier,"Reset is not a demotion");
        rows.add(row(S+2*D+1000,"40",S+W+D));run(doc,rows,List.of(),bills,0,S+2*D+1000);near(120.123456789,bucket(doc,b).getDouble("points"),"Only post-reset consumption added");
        String stable=doc.toString();run(doc,rows,List.of(),bills,0,S+2*D+1000);check(stable.equals(doc.toString()),"Identical recovery and imports are idempotent");
        run(doc,List.of(),List.of(),bills,0,S+2*D+2000);near(120.123456789,bucket(doc,b).getDouble("points"),"Original ledger pruning/reconnect cannot erase settled amount");
        JSONObject restarted=new JSONObject(doc.toString());near(bucket(doc,b).getDouble("points"),bucket(restarted,b).getDouble("points"),"Account document process reopen");
        for(int length:new int[]{28,30,31}){JSONObject payment=bill(length,"100"),d=AccountProgress.empty();run(d,List.of(row(S+D,"80",S+W),row(S+W+1000,"0",S+2*W),row(S+W+2000,"40",S+2*W)),List.of(),new JSONArray().put(payment),0,S+W+2000);near(120*7d/length,bucket(d,payment).getDouble("percent"),"Exact entered "+length+"-day denominator");}
        rows=List.of(row(S+D,"80",S+W),row(S+2*D,"0",S+W),row(S+2*D+1000,"40",S+W));doc=AccountProgress.empty();run(doc,rows,List.of(S+2*D-100),bills,0,S+2*D+1000);near(120,bucket(doc,b).getDouble("points"),"Confirmed credit adds no artificial 100 percent");
        run(doc,rows,List.of(S+2*D-100,S+2*D-100),bills,0,S+2*D+1000);near(120,bucket(doc,b).getDouble("points"),"Credit duplication cannot grow total");
        doc=AccountProgress.empty();rows=List.of(row(S+D,"80",S+W),row(S+2*D,"40",S+W),row(S+2*D+1000,"50",S+W));run(doc,rows,List.of(),bills,0,S+2*D+1000);near(90,bucket(doc,b).getDouble("points"),"Correction is not another whole allowance");
        doc=AccountProgress.empty();rows=List.of(row(S-1000,"50",S+W-10000),row(S+1000,"70",S+W-10000),row(S+2000,"75",S+W-10000));run(doc,rows,List.of(),bills,0,S+2000);near(5,bucket(doc,b).getDouble("points"),"Billing boundary delta is not interpolated");
        check(bucket(doc,b).getJSONArray("unknown").length()>0,"Boundary remains unconfirmed rather than zero use");
        doc=AccountProgress.empty();rows=List.of(row(S+D,"80",S+W));run(doc,rows,List.of(),bills,0,S+D);JSONObject revised=bill(28,"200").put("saved_at",S+D);run(doc,rows,List.of(),new JSONArray().put(b).put(revised),0,S+D);near(40,bucket(doc,revised).getDouble("reference_value"),"Payment edit recalculates reference amount");check(doc.getJSONObject("billing").getJSONObject(AccountProgress.periodId(b)).getJSONArray("revisions").length()==2,"Original charge retained as revision");
        JSONObject next=new JSONObject(b.toString()).put("start","2026-11-05").put("end","2026-12-05").put("saved_at",S+28*D);run(doc,List.of(row(S+28*D+1000,"0",S+35*D)),List.of(),new JSONArray().put(next),0,S+28*D+1000);
        check(doc.getJSONObject("settlements").getJSONObject(AccountProgress.periodId(b)).getString("status").equals("final"),"Renewal seals old bill without month-first reset");check(doc.getJSONObject("settlements").has(AccountProgress.periodId(next)),"New registered billing period separated");
        doc=AccountProgress.empty();JSONObject legacy=new JSONObject().put("rating",new JSONObject().put("tier",7).put("end",S));AccountProgress.importLegacy(doc,"pro-key",legacy,null);AccountProgress.importLegacy(doc,"pro-key",legacy,null);check(doc.getJSONObject("heritage").length()==1&&doc.getJSONObject("tier").getInt("highest")==7,"Legacy import once and permanent highest");
        LedgerRecord free=new LedgerRecord("weekly","free",S+2*D,0,"0",S+W,604800,"api_precise",false);
        AccountProgress.importLegacy(doc,"free-key",new JSONObject().put("rating",new JSONObject().put("tier",0).put("end",S+W)),null);check(doc.getJSONObject("tier").getInt("tier")==7,"Newer empty Free journal cannot overwrite recorded Pro achievement during migration");
        run(doc,List.of(row(S+D,"80",S+W),free,row(S+3*D,"0",S+W),row(S+3*D+1000,"10",S+W)),List.of(),bills,0,S+3*D+1000);check(doc.getJSONObject("tier").getInt("tier")==7,"Pro Free Pro does not lose tier");check(doc.getJSONObject("tier").getInt("highest")==7,"Peak remains account-owned across plans");check(doc.getJSONObject("settlements").getJSONObject(AccountProgress.periodId(b)).getBoolean("mixed_policies"),"Different plan units remain segregated");check(bucket(doc,b).isNull("reference_value"),"Unknown plan allocation cannot claim a combined monetary value");
        double before=bucket(doc,b).getDouble("points");long floor=S+4*D;run(doc,List.of(),List.of(),bills,floor,floor);run(doc,List.of(row(S+D,"80",S+W),row(floor+1000,"10",S+W),row(floor+2000,"20",S+W)),List.of(),bills,floor,floor+2000);near(before+10,bucket(doc,b).getDouble("points"),"Deletion tombstone blocks old raw replay and retains recap");
        JSONObject t=AccountTier.empty();AccountTier.promote(t,5,S);AccountTier.evaluate(t,weeks(S+W,0));check(t.getInt("tier")==5,"One rest week never demotes");AccountTier.evaluate(t,weeks(S+2*W,0,0));check(t.getInt("tier")==5,"Promotion has two-week protection");
        t=AccountTier.empty();t.put("tier",5).put("highest",7).put("end",S-3*W).put("policy",P).put("protected_until",0);
        AccountTier.evaluate(t,weeks(S-2*W,0));check(t.getInt("tier")==5,"First low valid week holds");AccountTier.evaluate(t,weeks(S-W,0,0));check(t.getInt("tier")==5&&t.optBoolean("warning"),"Two low weeks only warn internally");AccountTier.evaluate(t,weeks(S,0,0,0,0));check(t.getInt("tier")==4&&t.getInt("highest")==7,"Three consecutive lows and low long score demote only one step");
        String old=t.toString();AccountTier.evaluate(t,weeks(S,0,0,0,0));check(old.equals(t.toString()),"Repeated closed window is not another demotion");AccountTier.evaluate(t,List.of());check(old.equals(t.toString()),"Missing/error evidence cannot demote");
        t=AccountTier.empty();t.put("tier",5).put("highest",5).put("end",S-4*W);AccountTier.evaluate(t,weeks(S,0));check(t.getInt("tier")==5&&t.getInt("low_streak")==0,"A multiweek observation gap breaks demotion streak");
        List<LedgerRecord> early=List.of(row(S+1000,"0",S+W),row(S+W-1000,"10",S+W),row(S+W-500,"0",S+2*W-500));check(FunInsights.completed(early,P,S+3*W).isEmpty(),"Early reset is not a certified full week");
        LedgerRecord five=new LedgerRecord("five_hour","pro",S+D,100,"100",S+D+18000000,18000,"api_precise",false);doc=AccountProgress.empty();run(doc,List.of(five,row(S+D,"0",S+W)),List.of(),bills,0,S+D);near(0,bucket(doc,b).getDouble("points"),"Five-hour consumption cannot enter monthly weekly total");check(doc.getJSONObject("tier").getInt("tier")==-1,"Missing accumulated evidence does not fabricate a tier");
        doc=AccountProgress.empty();JSONObject archive=new JSONObject().put("settlements",new JSONArray().put(new JSONObject().put("id",P+"@"+(S+W)).put("start",S).put("end",S+W).put("used","80").put("observations",40).put("quality","near_boundary_observations_no_interpolation")));
        AccountProgress.importLegacy(doc,"archive",archive,null);run(doc,List.of(),List.of(),bills,0,S+W+D);near(80,bucket(doc,b).getDouble("points"),"Certified closed recap restores pruned window");
        run(doc,List.of(row(S+W-1000,"40",S+W),row(S+W+1000,"20",S+2*W)),List.of(),bills,0,S+W+D);near(100,bucket(doc,b).getDouble("points"),"Partial overlapping raw recap not double counted and next window still grows");
        String prior=doc.toString();run(doc,List.of(),List.of(),bills,0,S+W+D);check(prior.equals(doc.toString()),"Archived window repeat is idempotent");
        JSONObject shortened=bill(28,"100").put("end","2026-10-15").put("saved_at",S+2*W);run(doc,List.of(),List.of(),new JSONArray().put(shortened),0,S+2*W);
        check(doc.getJSONObject("billing").length()==1&&doc.getJSONObject("settlements").length()==1,"Date edit retains stable period identity without a second bill");near(80,bucket(doc,b).getDouble("points"),"Date edit recalculates actual attribution instead of carrying outside-period spend");
        check(doc.getJSONObject("settlements").getJSONObject(AccountProgress.periodId(b)).getJSONArray("calculations").length()>=2,"Payment/rule/status revisions retain calculation snapshots");
        doc=AccountProgress.empty();LedgerAggregation.Day sealed=new LedgerAggregation.Day(P,"2026-10-09");sealed.points=12.5;sealed.first=S+D+1000;sealed.last=S+2*D-1000;
        AccountProgress.reconcile(doc,List.of(),List.of(sealed),List.of(),bills,0,S+3*D);near(12.5,bucket(doc,b).getDouble("points"),"Sealed nonlegacy daily total recovers pruned evidence");
        sealed.points=0;AccountProgress.reconcile(doc,List.of(row(S+W+1000,"20",S+2*W)),List.of(sealed),List.of(),bills,0,S+W+D);near(32.5,bucket(doc,b).getDouble("points"),"Pruned/rebuilt daily source cannot erase recap or swallow later new consumption");
        JSONObject state=AccountTier.empty();state.put("tier",7).put("highest",7);doc.put("tier",state);AccountProgress.reconcile(doc,List.of(),List.of(),List.of(),bills,0,S+3*D,false);check(doc.getJSONObject("tier").getInt("tier")==7,"Server failure prohibits weekly tier evaluation");
        doc=AccountProgress.empty();run(doc,List.of(row(S+D,"80",S+W),row(S+W+1000,"40",S+2*W)),List.of(S+W),bills,0,S+W+1000);near(120,bucket(doc,b).getDouble("points"),"Credit near regular rollover cannot hide new measured spend");
        check(AccountProgress.keys(doc.getJSONObject("settlements")).size()==1,"Portable Android JSON iteration");
        System.out.println("RPG Step 1: "+checks+" assertions passed (synthetic; no phone recovery proof).");
    }
}
