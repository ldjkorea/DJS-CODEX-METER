package dev.bennett.codexmeter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.time.*;
import java.util.*;
import org.json.*;

/** Explicit synthetic data, restricted to the separate trial application's sandbox. */
final class TrialDemo {
    static final String ACCOUNT="gpt-hud-trial-demo";
    static boolean active(Context c){AuthTokens t=SecureTokenStore.load(c);return t!=null&&ACCOUNT.equals(t.accountId);}
    static UsageSnapshot snapshot(Context c)throws Exception {
        long now=System.currentTimeMillis();UsageSnapshot sample=new UsageSnapshot("free",true,false,new UsageWindow(10,18000,0,(now+18000000)/1000),new UsageWindow(15,604800,0,(now+6*86400000L)/1000),now);
        AppPreferences.saveSnapshot(c,sample);return sample;
    }
    static void prepare(Context c)throws Exception {
        if(!"dev.bennett.codexmeter.trial".equals(c.getPackageName()))throw new IllegalStateException("Trial package required");
        AuthTokens existing=SecureTokenStore.load(c);if(existing!=null&&!ACCOUNT.equals(existing.accountId))throw new IllegalStateException("내 계정으로 사용 중입니다. 먼저 체험판에서 로그아웃한 뒤 데모를 열어 주세요.");
        AccountSession.cancelSignIn();
        long now=System.currentTimeMillis();synchronized(UsageApi.NETWORK_LOCK){SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_TRIAL_NOT_A_REAL_TOKEN","SYNTHETIC_TRIAL_REFRESH","",now+3650L*86400000,ACCOUNT,""));}
        RefreshScheduler.cancelAll(c);
        AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);AppPreferences.completeOnboarding(c);
        UsageSnapshot sample=snapshot(c);String account=SubscriptionStore.accountKey(c);
        if(!c.getSharedPreferences("trial_welcome",0).getBoolean("demo_seeded",false)){
            FunStore.tone(c,"spicy");
            JSONObject doc=AccountProgress.empty();JSONObject r=RpgEngine.state(doc);LocalDate first=LedgerAggregation.day(now).minusDays(29);long start=first.atStartOfDay(MxpEconomy.ZONE).toInstant().toEpochMilli();
            AchievementJourney.prepare(doc,start);TitleJourney.prepare(doc,start);AchievementJourney.setEnabled(doc,true,start);TitleJourney.setEnabled(doc,true,start);r.put("collection_revision",CollectionRestart.REVISION);MxpEconomy.prepare(r,account,start);
            for(int i=0;i<28;i++){LocalDate d=first.plusDays(i);if(d.getDayOfWeek().getValue()>5)continue;long at=d.atStartOfDay(MxpEconomy.ZONE).toInstant().toEpochMilli()+3600000;MxpEconomy.accept(MxpEconomy.state(r),account,"trial:"+d,"codex",MxpEconomy.EvidenceStatus.VERIFIED,at,at+1000,now,now);}
            if(MxpEconomy.state(r).getJSONObject("titles").has("rpg3.workbench"))r.put("equipped_title","rpg3.workbench");
            c.getDatabasePath(RpgProgressStore.NAME).getParentFile().mkdirs();
            try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(c.getDatabasePath(RpgProgressStore.NAME),null)){
                db.beginTransaction();try{db.execSQL("CREATE TABLE IF NOT EXISTS profiles(account TEXT PRIMARY KEY,document TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS recovery(account TEXT PRIMARY KEY,captured_at INTEGER NOT NULL,document TEXT NOT NULL)");db.setVersion(1);db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{account,doc.toString()});db.setTransactionSuccessful();}finally{db.endTransaction();}
            }
            c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(account).commit();
            JSONObject catalog=new JSONObject(read(c.getAssets().open("coach-ko-v2.json")));JSONArray hidden=new JSONArray();for(int i=0;i<catalog.getJSONArray("quotes").length();i++){JSONObject q=catalog.getJSONArray("quotes").getJSONObject(i);if(q.optString("tipId").isEmpty())hidden.put(q.getString("id"));}
            c.getSharedPreferences("coach_v2",0).edit().putString(account,new JSONObject().put("schemaVersion",2).put("recent",new JSONArray()).put("tips",new JSONArray()).put("hidden",hidden).toString()).commit();
            c.getSharedPreferences("trial_welcome",0).edit().putBoolean("demo_seeded",true).commit();
        }
        RpgProgressStore.reconcile(c,sample,new UsageLedgerDatabase.Data(),account);SpicyQuotes.manual();SpicyQuotes.select(c,account,sample,true,false,0,now);
    }
    static void leaveDemo(Context c){if(active(c)){SecureTokenStore.clear(c);AppPreferences.setAutomaticRefresh(c,true);AppPreferences.setRefreshOnLaunch(c,true);}}
    private static String read(java.io.InputStream in)throws Exception{try(in;java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))>=0;)out.write(buffer,0,n);return out.toString("UTF-8");}}
}
