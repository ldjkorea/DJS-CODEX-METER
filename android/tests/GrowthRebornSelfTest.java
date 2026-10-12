package dev.bennett.codexmeter;
import java.util.*;import org.json.*;
public final class GrowthRebornSelfTest {
 static int n;static void check(boolean b,String why){n++;if(!b)throw new AssertionError(why);}static final String ACCOUNT="a".repeat(64);
 static void token(JSONObject d,long value,long at)throws Exception{RpgEngine.ingestOfficial(d,ACCOUNT,new JSONObject().put("account",ACCOUNT).put("source","codex_account_usage_v1").put("at",at).put("summary",new JSONObject().put("lifetimeTokens",value)),at);}
 public static void main(String[] args)throws Exception{long t=1791800000000L;
  for(boolean medal:new boolean[]{false,true})for(boolean title:new boolean[]{false,true}){JSONObject d=AccountProgress.empty();token(d,1_000_000_000,t-1000);JSONObject legacyBackup=RpgEngine.export(d,ACCOUNT);JSONObject s=RpgEngine.state(d);long xp=s.getLong("xp");List<String> old=new ArrayList<>(RpgTitleCatalog.owned(s));AchievementJourney.prepare(d,t);TitleJourney.prepare(d,t);check(!AchievementJourney.enabled(s)&&!TitleJourney.enabled(s),"Both default OFF");check(s.getLong("xp")==xp,"Migration preserves XP");check(RpgGrowthFacts.earned(s).isEmpty(),"New medals empty");check(RpgTitleCatalog.owned(s).containsAll(old),"Previous titles retained");JSONObject fresh=AccountProgress.empty();AchievementJourney.prepare(fresh,t);TitleJourney.prepare(fresh,t);RpgEngine.merge(fresh,ACCOUNT,legacyBackup,t+1);check(RpgGrowthFacts.earned(RpgEngine.state(fresh)).isEmpty(),"Old backup cannot unlock new medals");check(TitleJourney.owned(RpgEngine.state(fresh)).containsAll(old),"Old backup restores historical title ownership");check(AchievementJourney.journey(RpgEngine.state(fresh)).getJSONObject("legacy").getJSONObject("achievements").getJSONObject("tokens_1000000000").getBoolean("unlocked"),"Old backup medals preserved read-only");
   JSONObject offArchive=RpgEngine.export(d,ACCOUNT),offDevice=AccountProgress.empty();AchievementJourney.prepare(offDevice,t);TitleJourney.prepare(offDevice,t);RpgEngine.merge(offDevice,ACCOUNT,offArchive,t+2);check(TitleJourney.owned(RpgEngine.state(offDevice)).containsAll(old),"Not-started backup retains archived titles on another device");check(AchievementJourney.journey(RpgEngine.state(offDevice)).getJSONObject("legacy").getJSONObject("achievements").getJSONObject("tokens_1000000000").optBoolean("unlocked"),"Not-started backup retains read-only medals");
   if(medal)AchievementJourney.setEnabled(d,true,t+10);if(title)TitleJourney.setEnabled(d,true,t+10);token(d,1_001_000_000,t+20);check(RpgGrowthFacts.earned(s).contains("tokens_1000000")==medal,"Four combinations: medals independent");JSONObject tf=TitleJourney.journey(s).getJSONObject("facts").optJSONObject("tokens_1000000");check((tf!=null&&tf.optBoolean("unlocked"))==title,"Four combinations: independent title counting");
   check(TitleJourney.journey(s).getJSONObject("facts").optJSONObject("tokens_1000000000")==null||!TitleJourney.journey(s).getJSONObject("facts").getJSONObject("tokens_1000000000").optBoolean("unlocked"),"No historical billion-token re-unlock");
   List<String> earned=new ArrayList<>(RpgGrowthFacts.earned(s));JSONObject backup=RpgEngine.export(d,ACCOUNT);JSONObject restored=AccountProgress.empty();check(RpgEngine.merge(restored,ACCOUNT,backup,t+30),"Same account restores both journeys");check(RpgEngine.merge(restored,ACCOUNT,backup,t+30),"Repeated restore accepted idempotently");check(RpgEngine.state(restored).getLong("xp")==s.getLong("xp"),"No duplicate XP");check(!RpgEngine.merge(restored,"b".repeat(64),backup,t+30),"Different account rejected");
   AchievementJourney.setEnabled(d,false,t+40);TitleJourney.setEnabled(d,false,t+40);token(d,2_000_000_000,t+50);check(RpgGrowthFacts.earned(s).equals(earned),"Pausing never resets medals");check(TitleJourney.owned(s).containsAll(old),"Pausing retains old title ownership");
   if(title){String id=TitleJourney.journey(s).getString("id");TitleJourney.setEnabled(d,true,t+60);token(d,2_001_000_000,t+70);check(TitleJourney.journey(s).getLong("tokens")==2_000_000,"Resume excludes paused delta and uses fresh baseline");check(id.equals(TitleJourney.journey(s).getString("id")),"Title journey survives toggles");}
  }
  JSONObject d=AccountProgress.empty();token(d,99_000_000,t-600000);AchievementJourney.prepare(d,t);TitleJourney.prepare(d,t);TitleJourney.setEnabled(d,true,t);check(!TitleJourney.journey(RpgEngine.state(d)).optBoolean("official_armed"),"Stale baseline held");token(d,100_000_000,t+1);check(TitleJourney.journey(RpgEngine.state(d)).getLong("tokens")==0,"First verified baseline never credits gap");
  JSONObject paused=AccountProgress.empty();AchievementJourney.prepare(paused,t);TitleJourney.prepare(paused,t);AchievementJourney.setEnabled(paused,true,t+1);TitleJourney.setEnabled(paused,true,t+1);JSONObject ps=RpgEngine.state(paused),pj=TitleJourney.journey(ps);
  for(int i=1;i<=30;i++)pj.getJSONObject("activity").put(java.time.LocalDate.of(2026,10,1).plusDays(i).toString(),t+i+1);
  for(int i=1;i<=4;i++)pj.getJSONObject("weeks").put(Long.toString(t+i+1),true);
  ps.getJSONObject("achievements").put("days_30",new JSONObject().put("unlocked",true).put("first_earned_at",t+40));
  TitleJourney.reconcile(ps,t+41);TitleJourney.setEnabled(paused,false,t+50);
  ps.getJSONObject("achievements").put("weeks_4",new JSONObject().put("unlocked",true).put("first_earned_at",t+60));
  TitleJourney.setEnabled(paused,true,t+70);TitleJourney.reconcile(ps,t+80);
  check(!pj.getJSONObject("facts").getJSONObject("title_v25_log_keeper").optBoolean("unlocked"),"Paused medal cannot unlock compound title on resume");
  check(pj.getJSONObject("linked_medals").has("days_30"),"Prior enabled medal retained on resume");
  ps.getJSONObject("achievements").getJSONObject("weeks_4").put("first_earned_at",t+81);TitleJourney.reconcile(ps,t+82);
  check(pj.getJSONObject("facts").getJSONObject("title_v25_log_keeper").optBoolean("unlocked"),"Verified resumed medal unlocks compound title");
  JSONObject titleBackup=RpgEngine.export(paused,ACCOUNT),titleRestored=AccountProgress.empty();
  check(RpgEngine.merge(titleRestored,ACCOUNT,titleBackup,t+100),"Compound evidence backup restores");
  long savedXp=RpgEngine.state(titleRestored).getLong("xp");RpgEngine.merge(titleRestored,ACCOUNT,titleBackup,t+100);
  check(RpgEngine.state(titleRestored).getLong("xp")==savedXp,"Repeated compound restore awards no XP");
  JSONObject broken=new JSONObject(titleBackup.toString());broken.getJSONObject("rpg").getJSONObject("title_journey").getJSONObject("linked_medals").put("days_30",t+1000);
  String restoredBefore=titleRestored.toString();boolean badTitle=false;try{RpgEngine.merge(titleRestored,ACCOUNT,broken,t+100);}catch(IllegalArgumentException expected){badTitle=true;}
  check(badTitle&&titleRestored.toString().equals(restoredBefore),"Invalid linked medal restore rejects atomically");
  System.out.println("Growth Reborn: "+n+" checks passed; synthetic opt-in/restore evidence only.");
 }
}
