package dev.bennett.codexmeter;
import android.app.Instrumentation;import android.content.Context;import android.os.Bundle;
import java.time.*;import java.util.*;import org.json.*;

/** Standalone synthetic test package. Uses native Android SQLite and JSON, never the real app. */
public final class RpgNativeTest extends Instrumentation {
    int checks;void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{verify();result.putString("stream","Native RPG: "+checks+" persistence checks + "+RpgStep1SelfTest.checks+" calculation checks passed. Synthetic standalone emulator package.\n");finish(-1,result);}catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
    void verify()throws Exception{
        RpgStep1SelfTest.main(new String[0]);Context c=getTargetContext();long now=System.currentTimeMillis();LocalDate start=LedgerAggregation.day(now).minusDays(2);long reset=start.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli()+TierEvolution.WEEK;
        SecureTokenStore.current=new AuthTokens("NATIVE_FAKE_TOKEN","fixture-refresh","",0,"native-fixture-A","");String account=SubscriptionStore.accountKey(c);
        AppPreferences.current=new UsageSnapshot("pro",true,false,null,new UsageWindow(40,604800,0,reset/1000),now);
        check(SubscriptionStore.save(c,AppPreferences.current,SubscriptionStore.key(c,AppPreferences.current),new SubscriptionCost("100","USD",start,start.plusDays(30))),"Payment save");
        String legacyKey=SubscriptionStore.key(c,AppPreferences.current);JSONObject fun=new JSONObject().put("rating",new JSONObject().put("tier",7).put("end",now-1000));c.getSharedPreferences("codex_fun_v1",0).edit().putString(legacyKey,fun.toString()).commit();
        RpgProgressStore.backup(c);UsageLedgerDatabase.Data data=new UsageLedgerDatabase.Data();data.records.add(new LedgerRecord("weekly","pro",now-2000,80,"80",reset,604800,"api_precise",false));data.records.add(new LedgerRecord("weekly","pro",now-1000,0,"0",reset+86400000,604800,"api_precise",false));data.records.add(new LedgerRecord("weekly","pro",now,40,"40",reset+86400000,604800,"api_precise",false));
        RpgProgressStore.reconcile(c,AppPreferences.current,data,account);check(points(c)==120,"Reset recovery persisted");check(RpgProgressStore.tier(c)==7,"Legacy tier retained");
        RpgProgressStore.reconcile(c,AppPreferences.current,data,account);check(points(c)==120,"No duplicate growth");
        c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(account).commit();check(points(c)==120&&RpgProgressStore.tier(c)==7,"Native SQLite close/reopen with no display cache");
        check(c.getSharedPreferences("codex_fun_v1",0).getString(legacyKey,"").equals(fun.toString()),"Legacy journal unchanged");
        UsageSnapshot pro=AppPreferences.current;AppPreferences.current=new UsageSnapshot("free",true,false,null,pro.weekly,now);RpgProgressStore.reconcile(c,AppPreferences.current,new UsageLedgerDatabase.Data(),account);check(RpgProgressStore.tier(c)==7,"Free keeps account tier");check(RpgProgressStore.monthly(c,AppPreferences.current)==null,"Free cannot borrow paid Pro price");AppPreferences.current=pro;
        check(points(c)==120,"Returning Pro preserves recap");SecureTokenStore.current=null;check(RpgProgressStore.peek(c)==null,"Logout hides tier");
        SecureTokenStore.current=new AuthTokens("FAKE_B","fixture-refresh","",0,"native-fixture-B","");check(RpgProgressStore.peek(c)==null,"Other account isolation");RpgProgressStore.reconcile(c,AppPreferences.current,data,account);check(RpgProgressStore.peek(c)==null,"Stale worker blocked");
    }
    double points(Context c)throws Exception{return RpgProgressStore.monthly(c,AppPreferences.current).getJSONObject("buckets").getJSONObject("weekly|pro|604800").getDouble("points");}
}
