package dev.bennett.codexmeter;

import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Synthetic histories only. Exercises production archive/crypto/reconciliation, never personal data. */
public final class RpgRefinementSelfTest {
    static int checks;
    static final String ACCOUNT="a".repeat(64),OTHER="b".repeat(64),P="weekly|pro|604800";
    static final long D=86400000L,W=7*D,S=LocalDate.of(2026,8,1).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),NOW=S+90*D;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    interface Action {void run()throws Exception;}
    static void rejects(Action f,String why)throws Exception {boolean thrown=false;try{f.run();}catch(Exception expected){thrown=true;}check(thrown,why);}
    static JSONObject copy(JSONObject a)throws Exception{return new JSONObject(a.toString());}
    static JSONObject official(long at,long total,int days,long each)throws Exception {JSONArray daily=new JSONArray();for(int i=0;i<days;i++)daily.put(new JSONObject().put("startDate",LedgerAggregation.day(at).minusDays(i).toString()).put("tokens",each));return new JSONObject().put("account",ACCOUNT).put("source","codex_account_usage_v1").put("at",at).put("summary",new JSONObject().put("lifetimeTokens",total)).put("dailyUsageBuckets",daily);}
    static LedgerRecord row(long at,String used,long reset){return new LedgerRecord("weekly","pro",at,Double.parseDouble(used),used,reset,604800,"api_precise",false);}
    static JSONObject profile()throws Exception {JSONObject doc=AccountProgress.empty();List<LedgerRecord> rows=List.of(row(S+D,"80.123456789",S+W),row(S+2*D,"0",S+W+D),row(S+2*D+1000,"40",S+W+D));JSONObject bill=new JSONObject().put("plan","pro").put("amount","100").put("currency","USD").put("start","2026-08-01").put("end","2026-08-31").put("saved_at",S);AccountProgress.reconcile(doc,rows,List.of(),List.of(),new JSONArray().put(bill),0,NOW);RpgEngine.ingestOfficial(doc,ACCOUNT,official(NOW,2_700_000_000L,30,10_000_000L),NOW);return doc;}
    static double points(JSONObject d)throws Exception {return d.getJSONObject("settlements").getJSONObject("pro:2026-08-01:2026-08-31").getJSONObject("buckets").getJSONObject(P).getDouble("points");}
    public static void main(String[] args)throws Exception {
        JSONObject doc=profile(),s=RpgEngine.state(doc);check(s.getJSONObject("achievements").length()==53,"All unchanged achievements present");
        long xp=s.getLong("xp");check(s.getJSONObject("awards").getLong("milestone-v25:tokens:1000000000")==1000,"Versioned one-time billion-token milestone");
        RpgEngine.ingestOfficial(doc,ACCOUNT,official(NOW,2_700_000_000L,30,10_000_000L),NOW);check(RpgEngine.state(doc).getLong("xp")==xp,"Duplicate fetch cannot duplicate milestones");
        String title=RpgEngine.eligibleTitles(doc).get(0);check(RpgEngine.equip(doc,title),"Existing title equips");JSONArray selected=new JSONArray(RpgGrowthFacts.earned(s).subList(0,3));JSONObject file=RpgArchive.create(doc,ACCOUNT,selected,true,NOW);
        char[] pass="test-only-long-password".toCharArray();byte[] bytes=RpgBackupCodec.encrypt(file,pass),second=RpgBackupCodec.encrypt(file,pass);
        check(!Arrays.equals(bytes,second),"Independent random salt/nonce per export");check(!new String(bytes,StandardCharsets.UTF_8).contains("lifetimeTokens"),"Encrypted file hides token metadata");
        JSONObject decoded=RpgBackupCodec.decrypt(bytes,pass);check(decoded.getString("account").equals(ACCOUNT),"Authenticated encrypted round trip");
        rejects(()->RpgBackupCodec.decrypt(bytes,"another-long-password".toCharArray()),"Wrong password rejects authenticated file");
        JSONObject fractional=new JSONObject(new String(bytes,StandardCharsets.UTF_8)).put("version",1.5);
        rejects(()->RpgBackupCodec.decrypt(fractional.toString().getBytes(StandardCharsets.UTF_8),pass),"Fractional protocol version is rejected rather than truncated");
        fractional.put("version",1).put("iterations",600000.5);
        rejects(()->RpgBackupCodec.decrypt(fractional.toString().getBytes(StandardCharsets.UTF_8),pass),"Fractional KDF work factor is rejected rather than truncated");
        JSONObject outer=new JSONObject(new String(bytes,StandardCharsets.UTF_8));byte[] ciphertext=Base64.getDecoder().decode(outer.getString("data"));ciphertext[ciphertext.length/2]^=1;outer.put("data",Base64.getEncoder().encodeToString(ciphertext));
        rejects(()->RpgBackupCodec.decrypt(outer.toString().getBytes(StandardCharsets.UTF_8),pass),"Changed ciphertext rejects GCM authentication");
        rejects(()->RpgBackupCodec.decrypt(Arrays.copyOf(bytes,bytes.length-30),pass),"Truncation rejected");
        rejects(()->RpgArchive.merge(AccountProgress.empty(),decoded,OTHER,NOW),"Foreign account rejects restore");
        JSONObject fresh=RpgArchive.merge(AccountProgress.empty(),decoded,ACCOUNT,NOW);check(RpgEngine.state(fresh).getLong("xp")==xp,"Same-account reinstall restores all XP");check(Math.abs(points(fresh)-120.123456789)<1e-9,"Pre-global-reset and post-reset usage recap restored precisely");
        check(RpgEngine.state(fresh).getString("equipped_title").equals(title),"Equipped linked title preserved");
        long once=RpgEngine.state(fresh).getLong("xp");for(int i=0;i<4;i++)fresh=RpgArchive.merge(fresh,decoded,ACCOUNT,NOW);check(RpgEngine.state(fresh).getLong("xp")==once&&points(fresh)==points(doc),"Repeated restore never adds XP or recap snapshots");
        RpgEngine.equip(fresh,"");JSONObject older=RpgArchive.merge(fresh,decoded,ACCOUNT,NOW);check(!RpgEngine.state(older).has("equipped_title"),"Explicit current unequip survives an older backup");
        JSONObject newer=copy(fresh);RpgEngine.ingestOfficial(newer,ACCOUNT,official(NOW+1,5_000_000_000L,30,10_000_000L),NOW+1);long newerXp=RpgEngine.state(newer).getLong("xp");JSONObject retained=RpgArchive.merge(newer,decoded,ACCOUNT,NOW+1);check(RpgEngine.state(retained).getLong("xp")==newerXp&&RpgEngine.state(retained).getJSONObject("achievements").getJSONObject("tokens_5b").getBoolean("unlocked"),"Old archive cannot remove newer achievement or XP");
        String source=newer.toString();JSONObject partial=copy(file);partial.getJSONObject("document").getJSONObject("future").getJSONObject("rpg").getJSONObject("achievements").remove("tokens_1000000");rejects(()->RpgArchive.merge(newer,partial,ACCOUNT,NOW+1),"Missing achievement entry rejects whole archive");check(newer.toString().equals(source),"Failed validation leaves source unchanged");
        JSONObject wrong=copy(file);wrong.getJSONObject("document").put("access_token","DO_NOT_EXPORT");rejects(()->RpgArchive.validate(wrong,ACCOUNT,NOW),"Authentication cannot enter archive");
        for(String key:new String[]{"pairing_secret","pairingCode","private_key","prompt","command","conversation","sourceCode","raw_log"}){
            JSONObject secret=copy(file);secret.getJSONObject("document").getJSONObject("future").put(key,"SYNTHETIC_PRIVATE");String preserved=doc.toString();rejects(()->RpgArchive.merge(doc,secret,ACCOUNT,NOW),"Private metadata rejected: "+key);check(doc.toString().equals(preserved),"Rejected archive keeps current journal");
        }
        JSONObject fractionalArchive=copy(file).put("version",1.5);rejects(()->RpgArchive.validate(fractionalArchive,ACCOUNT,NOW),"Fractional archive version rejected");
        for(boolean medals:new boolean[]{false,true})for(boolean titles:new boolean[]{false,true}){
            JSONObject opted=copy(doc);AchievementJourney.prepare(opted,NOW+1);TitleJourney.prepare(opted,NOW+1);
            AchievementJourney.setEnabled(opted,medals,NOW+2);TitleJourney.setEnabled(opted,titles,NOW+2);
            if(medals){AchievementJourney.setEnabled(opted,false,NOW+3);AchievementJourney.setEnabled(opted,true,NOW+4);}
            if(titles){TitleJourney.setEnabled(opted,false,NOW+3);TitleJourney.setEnabled(opted,true,NOW+4);}
            JSONObject portable=RpgArchive.create(opted,ACCOUNT,new JSONArray(),true,NOW+10),decodedJourney=RpgBackupCodec.decrypt(RpgBackupCodec.encrypt(portable,pass),pass);
            JSONObject recovered=RpgArchive.merge(AccountProgress.empty(),decodedJourney,ACCOUNT,NOW+10),rs=RpgEngine.state(recovered),os=RpgEngine.state(opted);
            check(AchievementJourney.journey(rs).toString().equals(AchievementJourney.journey(os).toString()),"Encrypted medal ON/OFF segments and baseline preserved");
            JSONObject originalScope=copy(TitleJourney.journey(os)),restoredScope=copy(TitleJourney.journey(rs));
            // Restore refreshes derived locked-title facts; compare every persisted counting baseline.
            originalScope.remove("facts");restoredScope.remove("facts");originalScope.remove("linked_medals");restoredScope.remove("linked_medals");
            check(originalScope.similar(restoredScope),"Encrypted title ON/OFF segments and counting baselines preserved");
            JSONObject linkedBefore=TitleJourney.journey(os).optJSONObject("linked_medals"),linkedAfter=TitleJourney.journey(rs).optJSONObject("linked_medals");
            check(linkedBefore==null?linkedAfter==null||linkedAfter.length()==0:linkedBefore.similar(linkedAfter),"Title linked medal evidence preserved without invented medals");
            long originalXp=rs.getLong("xp");recovered=RpgArchive.merge(recovered,decodedJourney,ACCOUNT,NOW+11);check(RpgEngine.state(recovered).getLong("xp")==originalXp,"Encrypted repeated restore never awards XP");
        }
        JSONObject tooMany=copy(file);tooMany.getJSONObject("showcase").getJSONArray("ids").put("days_1");rejects(()->RpgArchive.validate(tooMany,ACCOUNT,NOW),"Showcase bounds strictly validated");
        JSONObject fabricated=copy(file);fabricated.getJSONObject("document").getJSONObject("future").getJSONObject("rpg").getJSONObject("awards").put("arbitrary-xp",200);rejects(()->RpgArchive.validate(fabricated,ACCOUNT,NOW),"Unrecognized award rejected");
        JSONObject early=AccountProgress.empty();JSONObject earlyBill=new JSONObject().put("plan","pro").put("amount","100").put("currency","USD").put("start","2026-08-01").put("end","2026-08-31").put("saved_at",S);List<LedgerRecord> earlyRows=List.of(row(S+D,"80",S+W),row(S+2*D,"0",S+W+D),row(S+2*D+1000,"40",S+W+D));AccountProgress.reconcile(early,earlyRows,List.of(),List.of(),new JSONArray().put(earlyBill),0,S+2*D+1000);JSONObject earlyFile=RpgArchive.create(early,ACCOUNT,new JSONArray(),false,S+2*D+1000),deleted=AccountProgress.empty();deleted.put("floor",S+3*D);deleted=RpgArchive.merge(deleted,earlyFile,ACCOUNT,S+4*D);check(deleted.getJSONObject("evidence").length()==0,"Deletion floor prevents old raw reimport");AccountProgress.reconcile(deleted,List.of(row(S+4*D,"10",S+W+D),row(S+4*D+1000,"20",S+W+D)),List.of(),List.of(),new JSONArray(),S+3*D,S+4*D+1000);check(points(deleted)==130,"Recovered pre-deletion recap plus actual new consumption, no double counting");
        JSONObject revised=copy(doc);revised.getJSONObject("settlements").getJSONObject("pro:2026-08-01:2026-08-31").getJSONObject("bill").put("amount","200");JSONObject mergedBill=RpgArchive.merge(revised,file,ACCOUNT,NOW);double expected=200*points(mergedBill)*7/30/100;check(Math.abs(mergedBill.getJSONObject("settlements").getJSONObject("pro:2026-08-01:2026-08-31").getJSONObject("buckets").getJSONObject(P).getDouble("reference_value")-expected)<1e-8,"Current payment edit retained and imported reference value recalculated");
        for(RpgTitleCatalog.Title def:RpgTitleCatalog.ALL){JSONObject independent=AccountProgress.empty(),is=RpgEngine.state(independent);RpgEngine.reconcile(independent,List.of(),List.of(),List.of(),List.of(),NOW,false);is.put("activity_days",def.days).put("valid_weeks",def.weeks);for(String id:def.medals)is.getJSONObject("achievements").getJSONObject(id).put("unlocked",true);RpgTitleCatalog.reconcile(is,NOW);check(RpgTitleCatalog.owned(is).contains(def.display.id),"Independent title requires real combined facts "+def.display.id);long before=is.getLong("xp");check(RpgEngine.equip(independent,def.display.id)&&is.getLong("xp")==before,"Independent equip changes no XP");}
        for(RpgTitleCatalog.Title def:RpgTitleCatalog.ALL){for(String id:def.medals)check(!RpgGrowthFacts.entry(id).metric.equals("plans")&&!RpgGrowthFacts.entry(id).metric.equals("dawn"),"New title does not reward subscription changes or sleep disruption");}
        for(RpgCatalog.Entry e:RpgCatalog.ALL){int[] first=RpgCollectionFacts.motif(e);check(Arrays.equals(first,RpgCollectionFacts.motif(e)),"Stable ID motif "+e.id);}
        JSONObject tier=AccountProgress.empty(),t=RpgEngine.state(tier);for(String region:new String[]{"KR","WORLD"}){RpgTier.current(t.getJSONObject(region),98,S-10*W,true,true);t.getJSONObject(region).put("end",S).put("policy",P).put("protected_until",0).getJSONObject("weeks").put(Long.toString(S),0);}
        for(int i=-4;i<=5;i++)t.getJSONObject("weeks").put(Long.toString(S+i*W),new JSONObject().put("start",S+(i-1)*W).put("end",S+i*W).put("policy",P).put("used","0"));t.put("current_policy",P).put("policy_changed_at",S-10*W);
        List<LedgerRecord> observations=new ArrayList<>();observations.add(row(S-10*W,"0",S-9*W));
        for(int i=1;i<=3;i++){long now=S+i*W+1000;JSONObject envelope=official(now,0,35,0).put("calendar_certification",new JSONObject().put("zone","Asia/Seoul").put("scope","account").put("complete",true).put("start",LedgerAggregation.day(now).minusDays(34).toString()).put("end",LedgerAggregation.day(now).plusDays(1).toString()));observations.add(row(now,"0",S+(i+1)*W));RpgEngine.reconcile(tier,observations,List.of(),List.of(),List.of(),now,true);RpgEngine.ingestOfficial(tier,ACCOUNT,envelope,now);check(t.optString("demotion_status").equals("comparable_window_evaluated"),"Operational ingestion invokes certified evaluator");if(i<=2)check(t.getJSONObject("KR").getInt("tier")==9,"One/two certified low weeks hold rank");}
        check(t.getJSONObject("KR").getInt("tier")==8&&t.getJSONObject("WORLD").getInt("tier")==8,"Three comparable low weeks plus long score demote independently by one");check(t.getJSONObject("KR").getInt("highest")==9,"Highest record remains permanent");
        JSONObject cachedCalendar=RpgTokens.empty();RpgTokens.official(cachedCalendar,ACCOUNT,official(NOW,1,35,0),NOW);JSONObject absent=official(NOW+1,1,0,0).put("calendar_certification",new JSONObject().put("zone","Asia/Seoul").put("scope","account").put("complete",true).put("start",LedgerAggregation.day(NOW).minusDays(30).toString()).put("end",LedgerAggregation.day(NOW).toString()));rejects(()->RpgTokens.official(cachedCalendar,ACCOUNT,absent,NOW+1),"Cached unknown-zone dates cannot certify a new incomplete calendar");
        RpgEngine.ingestOfficial(tier,ACCOUNT,official(S+4*W,0,35,0),S+4*W);check(t.optString("demotion_status").startsWith("held_"),"Unverified/stale operational inputs hold demotion");
        for(int days:new int[]{1,30,90,365}){long daily=days*120L,bonus=1350;int level=RpgEngine.level(daily+bonus);check(level>=RpgEngine.level(daily)&&level<40,"Bounded balance simulation "+days+" days");System.out.println("Synthetic balance "+days+" active days: "+(daily+bonus)+" XP / level "+level);}
        String[] kinds={"new","30-day","90-day","365-day","intensive","returning"};int[] counts={1,30,90,365,30,30};long[] volume={100000,100000,100000,100000,100000000,100000};
        for(int i=0;i<kinds.length;i++){JSONObject sample=AccountProgress.empty();AchievementJourney.prepare(sample,NOW);TitleJourney.prepare(sample,NOW);JSONObject observed=official(NOW,counts[i]*volume[i],counts[i],volume[i]);
            RpgEngine.ingestOfficial(sample,ACCOUNT,observed,NOW);JSONObject game=RpgEngine.state(sample);long earnedXp=game.getLong("xp");int earnedLevel=game.getInt("level");
            check(earnedXp>0&&earnedLevel==RpgEngine.level(earnedXp),"Actual engine monotone XP/level "+kinds[i]);
            RpgEngine.ingestOfficial(sample,ACCOUNT,observed,NOW);check(game.getLong("xp")==earnedXp,"Actual duplicate reward blocked "+kinds[i]);
            if(kinds[i].equals("returning")){RpgEngine.reconcile(sample,List.of(),List.of(),List.of(),List.of(),NOW+90*D,false);check(game.getLong("xp")==earnedXp,"Ninety-day break cannot erase XP");}
            check(RpgGrowthFacts.earned(game).isEmpty()&&TitleJourney.owned(game).isEmpty(),"OFF collections remain off for "+kinds[i]);
            System.out.println("Phase B observed-token simulation "+kinds[i]+": "+earnedXp+" XP / level "+earnedLevel);
        }
        Arrays.fill(pass,'\0');System.out.println("RPG v25 refinement: "+checks+" assertions passed (synthetic histories, no user recovery claim).");
    }
}
