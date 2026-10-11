package dev.bennett.codexmeter;

import android.content.Context;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.*;import org.json.*;

/** Actual production account DAO on a real SQLite bridge, synthetic account and ledger input. */
public final class RpgStoreSelfTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void account(String id){SecureTokenStore.current=new AuthTokens("SYNTHETIC_TOKEN_DO_NOT_STORE","fixture-refresh","",0,id,"");}
    static JSONObject document(String key)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(RpgProgressStore.NAME);Cursor c=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{key})){return c.moveToFirst()?new JSONObject(c.getString(0)):null;}}
    static double points(Context c){return RpgProgressStore.monthly(c,AppPreferences.current).getJSONObject("buckets").getJSONObject("weekly|pro|604800").getDouble("points");}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("rpg-store-synthetic-");System.setProperty("ledger.dbdir",dir.toString());Context c=new Context(dir);
        account("fixture-account-A");String a=SubscriptionStore.accountKey(c);long now=System.currentTimeMillis();LocalDate start=LedgerAggregation.day(now).minusDays(2);long zero=start.atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();long reset=zero+TierEvolution.WEEK;
        AppPreferences.current=new UsageSnapshot("pro",true,false,null,new UsageWindow(40,604800,0,reset/1000),now);
        SubscriptionCost bill=new SubscriptionCost("100","USD",start,start.plusDays(30));check(SubscriptionStore.save(c,AppPreferences.current,SubscriptionStore.key(c,AppPreferences.current),bill),"Entered billing retained");
        Path original=dir.resolve("original-fixture.db");Files.writeString(original,"immutable synthetic old DB");byte[] before=Files.readAllBytes(original);
        RpgProgressStore.backup(c);Path root=dir.resolve("rpg-step1-recovery");Path copy=root.resolve(Files.readString(root.resolve("complete")));
        check(Arrays.equals(before,Files.readAllBytes(copy.resolve("original-fixture.db"))),"Pre-recovery copy preserved");check(new JSONObject(Files.readString(copy.resolve("manifest.json"))).has("original-fixture.db"),"Snapshot checksum manifest");
        UsageLedgerDatabase.Data data=new UsageLedgerDatabase.Data();data.records.add(new LedgerRecord("weekly","pro",now-2000,80,"80",reset,604800,"api_precise",false));data.records.add(new LedgerRecord("weekly","pro",now-1000,0,"0",reset+86400000L,604800,"api_precise",false));data.records.add(new LedgerRecord("weekly","pro",now,40,"40",reset+86400000L,604800,"api_precise",false));
        RpgProgressStore.reconcile(c,AppPreferences.current,data,a);check(points(c)==120,"Production SQLite saves pre/post-reset total");String stored=document(a).toString();
        RpgProgressStore.reconcile(c,AppPreferences.current,data,a);check(stored.equals(document(a).toString()),"Repeated recovery is idempotent on disk");
        c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(a,"").commit();Context restarted=new Context(dir);check(points(restarted)==120,"Process/cache reopen reads durable SQLite recap");
        c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(a,"broken-json").commit();check(points(c)==120,"Corrupt derived cache falls back to SQLite");
        SQLiteDatabase.failCommit=true;boolean failed=false;try{RpgProgressStore.reconcile(c,AppPreferences.current,data,a);}catch(Exception expected){failed=true;}check(failed&&stored.equals(document(a).toString()),"Commit failure rolls back without wiping recap");
        Context.failWrites=true;failed=false;try{RpgProgressStore.reconcile(c,AppPreferences.current,data,a);}catch(Exception expected){failed=true;}Context.failWrites=false;check(failed&&stored.equals(document(a).toString()),"Preference display failure preserves durable source");
        account("fixture-account-B");String b=SubscriptionStore.accountKey(c);check(RpgProgressStore.peek(c)==null,"Different account cannot see A tier/billing");RpgProgressStore.reconcile(c,AppPreferences.current,data,a);check(document(b)==null,"Stale A worker cannot write to B even with same snapshot timestamp");
        RpgProgressStore.reconcile(c,AppPreferences.current,new UsageLedgerDatabase.Data(),b);check(document(b)!=null&&RpgProgressStore.monthly(c,AppPreferences.current)==null,"B starts independent billing and progress");
        account("fixture-account-A");check(points(c)==120,"Returning account keeps original recap");
        UsageSnapshot pro=AppPreferences.current;AppPreferences.current=new UsageSnapshot("free",true,false,null,pro.weekly,now);RpgProgressStore.reconcile(c,AppPreferences.current,new UsageLedgerDatabase.Data(),a);check(RpgProgressStore.tier(c)==document(a).getJSONObject("tier").getInt("tier"),"Plan change retains account tier");check(SubscriptionStore.load(c,AppPreferences.current)==null,"Pro price is not applied to Free");AppPreferences.current=pro;
        ExecutorService pool=Executors.newFixedThreadPool(4);try{List<Future<?>> futures=new ArrayList<>();for(int i=0;i<8;i++)futures.add(pool.submit(()->{try{RpgProgressStore.reconcile(c,AppPreferences.current,data,a);}catch(Exception e){throw new RuntimeException(e);}}));for(Future<?> f:futures)f.get();}finally{pool.shutdown();}check(points(c)==120,"Concurrent writers cannot double-count");
        check(Arrays.equals(before,Files.readAllBytes(original)),"Recovery never rewrites original usage DB");check(!document(a).toString().contains("SYNTHETIC_TOKEN_DO_NOT_STORE")&&!document(a).toString().contains("fixture-account-A"),"Account journal excludes auth token/raw identity");
        try(SQLiteDatabase db=new SQLiteDatabase(RpgProgressStore.NAME);Cursor cursor=db.rawQuery("SELECT COUNT(*) FROM recovery",null)){cursor.moveToFirst();check(cursor.getInt(0)==2,"One pre-recovery state per account");}
        SecureTokenStore.current=null;check(RpgProgressStore.peek(c)==null,"Signed-out display hides all account data");
        System.out.println("RPG production account DAO: "+checks+" assertions passed (real local SQLite, synthetic inputs; no phone proof).");
    }
}
