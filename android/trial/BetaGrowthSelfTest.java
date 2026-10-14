package dev.bennett.codexmeter;
import org.json.*;
/** Beta presentation regression. All inputs are synthetic detached documents. */
public final class BetaGrowthSelfTest {
    static int checks;
    static void check(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
    static final String ACCOUNT="a".repeat(64);
    static final long NOW=java.time.Instant.parse("2026-10-14T03:00:00Z").toEpochMilli();
    static BetaGrowthModel read(JSONObject d,boolean auto,boolean failed,boolean demo){return BetaGrowthModel.create(d,ACCOUNT,NOW,auto,failed,demo);}
    public static void main(String[] args)throws Exception {
        JSONObject doc=AccountProgress.empty(),r=RpgEngine.state(doc);
        AchievementJourney.prepare(doc,NOW-86400000);TitleJourney.prepare(doc,NOW-86400000);
        AchievementJourney.setEnabled(doc,true,NOW-86400000);TitleJourney.setEnabled(doc,true,NOW-86400000);
        MxpEconomy.prepare(r,ACCOUNT,NOW-86400000);
        // RpgProgressStore.export exposes future.rpg as the detached top-level rpg.
        doc.put("rpg",r);
        String original=doc.toString();BetaGrowthModel m=read(doc,true,false,false);
        check(m.status==BetaGrowthModel.Status.BASELINE,"First observation absent");check(m.known(),"Saved growth zero remains known");check(m.summary.getInt("day")==0,"No invented activity");
        check(read(doc,true,false,true).status==BetaGrowthModel.Status.DEMO,"Explicit demo label");
        check(read(doc,true,true,false).status==BetaGrowthModel.Status.ERROR,"Fetch failure distinct from zero");
        check(BetaGrowthModel.create(doc,"b".repeat(64),NOW,true,false,false).summary==null,"Cross-account rejected");
        check(read(null,true,false,false).summary==null,"Missing document unknown");
        JSONObject malformed=new JSONObject(doc.toString());MxpEconomy.state(malformed.getJSONObject("rpg")).remove("weeks");
        check(read(malformed,true,false,false).summary==null,"Malformed summary not default zero");
        check(original.equals(doc.toString()),"Reading states does not mutate ledger");
        r.put("quota_observed_at",NOW-1000);
        check(read(doc,true,false,false).status==BetaGrowthModel.Status.PENDING,"Connected observed zero");check(read(doc,true,false,false).lastObserved==NOW-1000,"Account observation time");
        check(read(doc,false,false,false).status==BetaGrowthModel.Status.STALE,"Automatic collection off");
        r.put("quota_observed_at",NOW-25*3600000L);
        check(read(doc,true,false,false).status==BetaGrowthModel.Status.STALE,"Old observation marked saved");
        r.put("quota_observed_at",NOW+1000);
        check(read(doc,true,false,false).lastObserved==0,"Future timestamp not shown as collected");
        r.put("quota_observed_at",NOW-1000);
        MxpEconomy.accept(MxpEconomy.state(r),ACCOUNT,"qa:today","codex",MxpEconomy.EvidenceStatus.VERIFIED,NOW-2000,NOW-1000,NOW,NOW);
        r.remove("quota_observed_at");check(read(doc,true,false,false).lastObserved==NOW,"Verified interval supplies exported observation time");
        check(read(doc,true,false,false).status==BetaGrowthModel.Status.COMPLETE,"Confirmed daily activity");
        AchievementJourney.setEnabled(doc,false,NOW);
        check(read(doc,true,false,false).status==BetaGrowthModel.Status.OFF,"Honor collection off");
        check(read(doc,true,false,false).summary.getInt("day")==1,"Off retains earned activity");
        check(BetaGrowthModel.next(0,3,5)==3,"First weekly checkpoint");check(BetaGrowthModel.next(3,3,5)==5,"Second weekly checkpoint");check(BetaGrowthModel.next(6,3,5)==-1,"Beyond target complete");
        check(BetaGrowthModel.rewardDelta(0,true)==20,"Weekly first bonus");check(BetaGrowthModel.rewardDelta(3,true)==10,"Weekly delta not full cumulative");
        check(BetaGrowthModel.rewardDelta(7,false)==40,"Monthly silver delta");check(BetaGrowthModel.rewardDelta(14,false)==0,"No duplicate final bonus");
        System.out.println("Beta growth presentation: "+checks+" checks passed");
    }
}
