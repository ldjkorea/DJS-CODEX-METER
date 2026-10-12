package dev.bennett.codexmeter;
import java.util.*;
import org.json.*;

public final class CollectionRestartSelfTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static JSONObject envelope(String account,long at,long n)throws Exception{return new JSONObject().put("account",account).put("at",at).put("source","codex_account_usage_v1").put("summary",new JSONObject().put("lifetimeTokens",n));}
    public static void main(String[] args)throws Exception{
        long now=1791774000000L;String account="a".repeat(64);
        JSONObject old=AccountProgress.empty();RpgEngine.ingestOfficial(old,account,envelope(account,now,100_000_000),now);
        AchievementJourney.prepare(old,now);TitleJourney.prepare(old,now);AchievementJourney.setEnabled(old,true,now+1);TitleJourney.setEnabled(old,true,now+1);
        JSONObject doc=new JSONObject(old.toString()),s=RpgEngine.state(doc);long xp=s.getLong("xp");String tokens=s.getJSONObject("tokens").toString(),tier=s.getJSONObject("KR").toString();
        check(CollectionRestart.prepare(doc,now+2),"Old enabled journeys migrate once");
        check(!AchievementJourney.enabled(s)&&!TitleJourney.enabled(s),"Both OFF after update");
        check(RpgGrowthFacts.earned(s).isEmpty()&&TitleJourney.owned(s).isEmpty(),"Both collections zero, including legacy titles");
        check(s.getLong("xp")==xp&&s.getJSONObject("tokens").toString().equals(tokens)&&s.getJSONObject("KR").toString().equals(tier),"Lifetime XP/token/tier unchanged");
        String migrated=doc.toString();check(!CollectionRestart.prepare(doc,now+3)&&migrated.equals(doc.toString()),"No reset on restart or upgrade rerun");
        JSONObject stale=RpgEngine.export(old,account);RpgEngine.merge(doc,account,stale,now+4);s=RpgEngine.state(doc);
        check(RpgGrowthFacts.earned(s).isEmpty()&&TitleJourney.owned(s).isEmpty(),"Old backup cannot revive collections");
        for(boolean medals:new boolean[]{false,true})for(boolean titles:new boolean[]{false,true}){
            JSONObject d=new JSONObject(doc.toString());JSONObject state=RpgEngine.state(d);
            AchievementJourney.setEnabled(d,medals,now+10);TitleJourney.setEnabled(d,titles,now+10);
            for(String key:new String[]{AchievementJourney.KEY,TitleJourney.KEY}){JSONObject j=state.getJSONObject(key);if(j.getBoolean("enabled"))j.getJSONObject("activity").put("2026-10-12",now+11);}
            RpgEngine.ingestOfficial(d,account,envelope(account,now+12,101_000_000),now+12);
            RpgEngine.ingestOfficial(d,account,envelope(account,now+13,102_000_000),now+13);
            check(AchievementJourney.journey(state).getLong("tokens")== (medals?2_000_000:0),"Medal tokens only while ON");
            check(TitleJourney.journey(state).getLong("tokens")== (titles?2_000_000:0),"Title tokens independently ON");
            check(RpgGrowthFacts.earned(state).contains("tokens_1000000")==medals,"Medal unlock gated");
            check(TitleJourney.owned(state).contains("tokens_1000000")==titles,"Title unlock independently gated");
            JSONObject backup=RpgEngine.export(d,account),archive=RpgArchive.create(d,account,new JSONArray(),false,now+14);long earnedXp=state.getLong("xp");
            CollectionRestart.reset(d,false,now+20,true);CollectionRestart.reset(d,true,now+20,true);
            RpgEngine.merge(d,account,backup,now+21);state=RpgEngine.state(d);
            check(!AchievementJourney.enabled(state)&&!TitleJourney.enabled(state)&&RpgGrowthFacts.earned(state).isEmpty()&&TitleJourney.owned(state).isEmpty(),"Manual reset survives old backup restore");
            check(state.getLong("xp")==earnedXp,"Reset/restore cannot duplicate lifetime XP");
            d=RpgArchive.merge(d,archive,account,now+21);state=RpgEngine.state(d);check(RpgGrowthFacts.earned(state).isEmpty()&&TitleJourney.owned(state).isEmpty(),"Actual archive restore honors reset tombstone");
            AchievementJourney.setEnabled(d,true,now+22);TitleJourney.setEnabled(d,true,now+22);
            check(AchievementJourney.journey(state).getLong("tokens")==0&&TitleJourney.journey(state).getLong("tokens")==0,"New run starts zero");
        }
        for(RpgCatalog.Entry e:RpgCatalog.ALL){int n=CollectionTierGoals.required(e);check(n>=0&&n<=9,"Stable tier goal "+e.id);}
        check(UsagePrecision.remaining(new UsageWindow(41.125,604800,0,now/1000+604800),0).equals("59%"),"Display whole quota, retain raw precision");
        UsageWindow half=new UsageWindow(58.5,604800,0,now/1000+604800);check(UsagePrecision.used(half,0).equals("59%")&&UsagePrecision.remaining(half,0).equals("41%"),"Rounded used and remaining stay complementary across views");
        check(RpgCatalog.ALL.size()==53&&RpgTitleCatalog.displayEntries().size()==63,"IDs preserved");
        System.out.println("Collection restart: "+checks+" checks passed (synthetic records)");
    }
}
