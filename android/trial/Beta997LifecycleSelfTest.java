package dev.bennett.codexmeter;

import org.json.JSONObject;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class Beta997LifecycleSelfTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        OAuthAttempt attempts=new OAuthAttempt();long old=attempts.begin();attempts.cancel();int[] saved={0};check(!attempts.commit(old,()->saved[0]++),"Cancelled callback cannot commit");
        long fresh=attempts.begin();check(!attempts.cancel(old)&&attempts.current(fresh),"Old cleanup cannot cancel retry");check(!attempts.commit(old,()->saved[0]++),"Old callback cannot replace retry");
        check(attempts.commit(fresh,()->saved[0]++),"Current callback commits");check(!attempts.commit(fresh,()->saved[0]++),"Current callback commits once");check(saved[0]==1&&attempts.epoch(fresh)&&!attempts.active(),"Committed session retains epoch without pending login");attempts.cancel();check(!attempts.epoch(fresh),"Account boundary invalidates post-auth callbacks");
        long concurrent=attempts.begin();ExecutorService pool=Executors.newFixedThreadPool(4);try{List<Future<Boolean>> results=new ArrayList<>();for(int i=0;i<24;i++)results.add(pool.submit(()->attempts.commit(concurrent,()->saved[0]++)));int successes=0;for(Future<Boolean> result:results)if(result.get())successes++;check(successes==1&&saved[0]==2,"Concurrent callbacks commit once");}finally{pool.shutdownNow();}
        long failed=attempts.begin();check(attempts.finish(failed)&&attempts.epoch(failed),"Failure result remains deliverable for its epoch");check(!attempts.current(failed),"Finished failure cannot accept a later token");check(!attempts.commit(failed,()->saved[0]++),"Finished callback cannot commit after failure");
        OAuthAttempt restartedProcess=new OAuthAttempt();check(!restartedProcess.current(fresh)&&!restartedProcess.active(),"Process recreation never resumes an old in-memory PKCE flow");
        String account="a".repeat(64);long now=LocalDate.of(2028,2,29).atTime(12,0).atZone(BetaV4.ZONE).toInstant().toEpochMilli();JSONObject state=BetaV4.create(account,now-86400000L);
        String before=state.toString();JSONObject goal=BetaNextGoal.choose(state,now);check(goal.getString("id").equals("rpg3.first_day"),"New profile recommends first verified day");check(before.equals(state.toString()),"Goal selection cannot mutate economy");
        check(BetaV4.confirm(state,account,now,now,true),"Synthetic verification recorded");before=state.toString();goal=BetaNextGoal.choose(state,now);check(goal.getBoolean("today_done"),"Today completed stops extra-check encouragement");check(before.equals(state.toString()),"Recommendation never awards XP");
        JSONObject synthetic=new JSONObject(state.toString());synthetic.put("checked_days",2).put("completed_weeks",0).put("level",99).put("total",BetaV4.threshold(99));synthetic.getJSONObject("honors").put("rpg3.first_day",now);synthetic.getJSONObject("weeks").put(BetaV4.week(BetaV4.day(now)),2);
        goal=BetaNextGoal.choose(synthetic,now);check(!goal.getString("id").equals("rpg4.level_100"),"One-check goal beats a nearly complete long level goal");
        for(String[] entry:BetaCatalog.ALL)if(!BetaCatalog.title(entry[0]))synthetic.getJSONObject("honors").put(entry[0],now);check(BetaNextGoal.choose(synthetic,now).getString("id").isEmpty(),"Completed collection has no fabricated next goal");
        for(LocalDate d:Arrays.asList(LocalDate.of(2027,1,1),LocalDate.of(2027,1,31),LocalDate.of(2027,12,31),LocalDate.of(2028,2,29),LocalDate.of(2028,12,31))){long at=d.atTime(12,0).atZone(BetaV4.ZONE).toInstant().toEpochMilli();JSONObject sample=BetaV4.create(account,at-86400000L);String raw=sample.toString();check(BetaNextGoal.choose(sample,at).getInt("target")>0,"Boundary recommendation valid "+d);check(raw.equals(sample.toString()),"Boundary recommendation read only "+d);}
        System.out.println("Beta997 lifecycle/goal: "+checks+" checks passed; no real OAuth/API calls");
    }
}
