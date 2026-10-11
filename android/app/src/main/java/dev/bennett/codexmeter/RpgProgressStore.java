package dev.bennett.codexmeter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Separate account journal; never updates the usage ledger, auth store or old plan journals. */
final class RpgProgressStore extends SQLiteOpenHelper {
    static final String NAME="gpt_hud_account_progress.db";
    private RpgProgressStore(Context c){super(c,NAME,null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE profiles(account TEXT PRIMARY KEY, document TEXT NOT NULL)");
        db.execSQL("CREATE TABLE recovery(account TEXT PRIMARY KEY, captured_at INTEGER NOT NULL, document TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int a,int b){throw new IllegalStateException("Preserve unsupported progress database");}
    static void backup(Context c) throws Exception {
        // Called before recovery reads/rollups. Never automatically restores these originals.
        synchronized(UsageApi.NETWORK_LOCK){
        File root=new File(c.getFilesDir(),"rpg-step1-recovery");root.mkdirs();File marker=new File(root,"complete");if(marker.exists())return;
        File folder=new File(root,"snapshot-"+UUID.randomUUID());if(!folder.mkdirs())throw new IOException("Recovery directory unavailable");
        UsageLedgerDatabase.recoveryCopy(c,folder);
        JSONObject prefs=new JSONObject();for(String name:new String[]{"codex_subscription_cost","codex_fun_v1","codex_tier_evolution_v2"})prefs.put(name,new JSONObject(c.getSharedPreferences(name,0).getAll()));
        File data=new File(folder,"legacy-preferences.json");if(!data.exists())Files.write(data.toPath(),prefs.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JSONObject manifest=new JSONObject();for(File file:Objects.requireNonNull(folder.listFiles())){
            java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
            try(InputStream in=new FileInputStream(file)){byte[] bytes=new byte[8192];for(int n;(n=in.read(bytes))>=0;)digest.update(bytes,0,n);}
            StringBuilder hex=new StringBuilder();for(byte v:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",v&255));manifest.put(file.getName(),hex.toString());
        }
        Files.write(new File(folder,"manifest.json").toPath(),manifest.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.write(marker.toPath(),folder.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
    static JSONObject peek(Context c){try{String account=SubscriptionStore.accountKey(c);if(account==null)return null;
        String raw=c.getSharedPreferences("codex_rpg_display_v1",0).getString(account,"");
        if(!raw.isEmpty()){try{JSONObject cached=new JSONObject(raw);if(cached.has("tier")&&cached.has("settlements"))return cached;}catch(JSONException invalid){/* Recover from durable journal below. */}}
        synchronized(UsageApi.NETWORK_LOCK){
        if(!account.equals(SubscriptionStore.accountKey(c)))return null;
        raw=c.getSharedPreferences("codex_rpg_display_v1",0).getString(account,"");
        if(!raw.isEmpty()){try{JSONObject cached=new JSONObject(raw);if(cached.has("tier")&&cached.has("settlements"))return cached;}catch(JSONException invalid){/* Recover below. */}}
        if(!c.getDatabasePath(NAME).exists())return null;
        try(RpgProgressStore helper=new RpgProgressStore(c);Cursor cursor=helper.getReadableDatabase().rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{account})){
            if(cursor.moveToFirst()){
                JSONObject restored=display(new JSONObject(cursor.getString(0)));
                // Rebuild only the small display cache after restart/cache loss.
                if(!account.equals(SubscriptionStore.accountKey(c)))return null;
                c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(account,restored.toString()).commit();
                return restored;
            }
        }return null;}
    }catch(Exception ignored){return null;}}
    static int tier(Context c){JSONObject p=peek(c),t=p==null?null:p.optJSONObject("tier");int tier=t==null?-1:t.optInt("tier",-1);return tier>=0&&tier<=9?tier:-1;}
    static void reconcile(Context c,UsageSnapshot captured,UsageLedgerDatabase.Data data) throws Exception {
        reconcile(c,captured,data,SubscriptionStore.accountKey(c));
    }
    static void reconcile(Context c,UsageSnapshot captured,UsageLedgerDatabase.Data data,String expectedAccount) throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){
            String account=SubscriptionStore.accountKey(c);UsageSnapshot current=AppPreferences.loadSnapshot(c);
            if(account==null||!account.equals(expectedAccount)||current==null||captured==null||current.fetchedAtMillis!=captured.fetchedAtMillis||!current.planType.equals(captured.planType))return;
            backupStep2(c);
            try(RpgProgressStore helper=new RpgProgressStore(c)){
                SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();JSONObject doc;
                try{
                    String old="";try(Cursor cursor=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{account})){if(cursor.moveToFirst())old=cursor.getString(0);}
                    doc=old.isEmpty()?AccountProgress.empty():new JSONObject(old);
                    db.execSQL("INSERT OR IGNORE INTO recovery(account,captured_at,document) VALUES(?,?,?)",new Object[]{account,System.currentTimeMillis(),old.isEmpty()?doc.toString():old});
                    Set<String> plans=new LinkedHashSet<>(Arrays.asList("free","go","plus","pro","prolite","pro_lite","pro_light","pro_100","team","business","enterprise"));plans.add(LedgerRecord.cleanPlan(current.planType));
                    for(LedgerRecord r:data.records)plans.add(r.plan);
                    JSONArray bills=new JSONArray();
                    for(String plan:plans){String key=SubscriptionStore.planKey(c,plan);if(key==null)continue;
                        JSONObject fun=legacy(c,"codex_fun_v1",key),tier=legacy(c,"codex_tier_evolution_v2",key);AccountProgress.importLegacy(doc,key,fun,tier);
                        String text=c.getSharedPreferences("codex_subscription_cost",0).getString(key+"_history","");JSONArray history=text.isEmpty()?new JSONArray():new JSONArray(text);
                        JSONObject bill=legacy(c,"codex_subscription_cost",key);if(bill!=null)history.put(bill);
                        for(int i=0;i<history.length();i++)bills.put(new JSONObject(history.getJSONObject(i).toString()).put("plan",plan));
                    }
                    List<Long> credits=new ArrayList<>();for(UsageLedgerDatabase.Event e:data.events)if(e.type.equals("credit_used")&&e.origin.equals("confirmed"))credits.add(e.at);
                    long now=System.currentTimeMillis();boolean evaluate=current.fetchedAtMillis<=now&&now-current.fetchedAtMillis<900000L&&AppPreferences.getLastError(c).isEmpty();
                    AccountProgress.reconcile(doc,data.records,data.days,credits,bills,UsageLedgerDatabase.lanFloor(c),now,evaluate);
                    db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{account,doc.toString()});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                JSONObject display=display(doc);
                if(!c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(account,display.toString()).commit())throw new IllegalStateException("Progress display write failed; durable journal preserved");
            }
        }
    }
    private static JSONObject display(JSONObject doc)throws JSONException {
        JSONObject result=new JSONObject().put("tier",doc.getJSONObject("tier")).put("settlements",doc.getJSONObject("settlements"));
        if(doc.getJSONObject("future").has("rpg"))result.put("rpg",RpgEngine.summary(doc));return result;
    }
    static JSONObject progress(Context c){JSONObject p=peek(c);return p==null?null:p.optJSONObject("rpg");}
    private static void backupStep2(Context c)throws IOException {
        File root=new File(c.getFilesDir(),"rpg-step2-recovery");if(new File(root,"complete").exists())return;
        if(!root.exists()&&!root.mkdirs())throw new IOException("Progress recovery unavailable");
        File folder=new File(root,"snapshot-"+UUID.randomUUID());if(!folder.mkdirs())throw new IOException("Recovery snapshot unavailable");
        File db=c.getDatabasePath(NAME);for(String suffix:new String[]{"","-wal","-shm"}){File f=new File(db.getPath()+suffix);if(f.exists())Files.copy(f.toPath(),new File(folder,f.getName()).toPath());}
        Files.write(new File(root,"complete").toPath(),folder.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    static void tokens(Context c,String expected,JSONObject envelope)throws Exception {mutate(c,expected,envelope,null,null);}
    static boolean equip(Context c,String expected,String title)throws Exception {return mutate(c,expected,null,title,null);}
    static boolean restore(Context c,String expected,JSONObject journal)throws Exception {return mutate(c,expected,null,null,journal);}
    static JSONObject export(Context c,String expected)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){String account=SubscriptionStore.accountKey(c);if(account==null||!account.equals(expected)||!c.getDatabasePath(NAME).exists())return null;
            try(RpgProgressStore helper=new RpgProgressStore(c);Cursor cursor=helper.getReadableDatabase().rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{account})){
                return cursor.moveToFirst()?RpgEngine.export(new JSONObject(cursor.getString(0)),account):null;
            }
        }
    }
    private static boolean mutate(Context c,String expected,JSONObject envelope,String title,JSONObject journal)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){String account=SubscriptionStore.accountKey(c);if(account==null||!account.equals(expected))return false;
            if(envelope==null&&title==null&&journal==null)return false;
            if(envelope!=null){JSONObject cached=progress(c);if(cached!=null&&envelope.optLong("at")<=cached.optLong("token_observed_at"))return false;}
            backupStep2(c);JSONObject document;
            try(RpgProgressStore helper=new RpgProgressStore(c)){SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{String raw="";try(Cursor cursor=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{account})){if(cursor.moveToFirst())raw=cursor.getString(0);}
                    document=raw.isEmpty()?AccountProgress.empty():new JSONObject(raw);
                    boolean changed=journal!=null?RpgEngine.merge(document,account,journal,System.currentTimeMillis()):envelope!=null?RpgEngine.ingestOfficial(document,account,envelope,System.currentTimeMillis()):RpgEngine.equip(document,title);
                    if(!changed)return false;
                    db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{account,document.toString()});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
            }
            if(!c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(account,display(document).toString()).commit())throw new IllegalStateException("RPG display failed; journal preserved");return true;
        }
    }
    private static JSONObject legacy(Context c,String name,String key) throws JSONException {String raw=c.getSharedPreferences(name,0).getString(key,"");return raw.isEmpty()?null:new JSONObject(raw);}
    static JSONObject monthly(Context c,UsageSnapshot s){try{
        JSONObject p=peek(c);SubscriptionCost cost=SubscriptionStore.load(c,s);if(p==null||cost==null)return null;
        JSONObject settlements=p.getJSONObject("settlements");for(String id:AccountProgress.keys(settlements)){
            JSONObject recap=settlements.getJSONObject(id),bill=recap.getJSONObject("bill");
            if(bill.getString("plan").equals(LedgerRecord.cleanPlan(s.planType))&&bill.getString("start").equals(cost.start.toString())&&bill.getString("end").equals(cost.end.toString()))return recap;
        }return null;
    }catch(Exception ignored){return null;}}
}
