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
        // Painting must never wait for quota HTTP, journal migration or database recovery.
        // Workers retain the durable recovery path below and refresh this display cache.
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()){
            String raw=c.getSharedPreferences("codex_rpg_display_v1",0).getString(account,"");
            if(raw.isEmpty())return null;
            JSONObject cached=new JSONObject(raw);
            if(!cached.has("tier")||!cached.has("settlements")||!account.equals(SubscriptionStore.accountKey(c)))return null;
            return safeDisplay(cached);
        }
        synchronized(UsageApi.NETWORK_LOCK){if(c.getDatabasePath(NAME).exists()&&!Integer.toString(CollectionRestart.REVISION).equals(String.valueOf(c.getSharedPreferences("codex_rpg_display_v1",0).getAll().get(account+":collection_revision"))))ensureJourney(c,account);}
        String raw=c.getSharedPreferences("codex_rpg_display_v1",0).getString(account,"");
        if(!raw.isEmpty()){try{JSONObject cached=new JSONObject(raw);if(cached.has("tier")&&cached.has("settlements"))return safeDisplay(cached);}catch(JSONException invalid){/* Recover from durable journal below. */}}
        synchronized(UsageApi.NETWORK_LOCK){
        if(!account.equals(SubscriptionStore.accountKey(c)))return null;
        raw=c.getSharedPreferences("codex_rpg_display_v1",0).getString(account,"");
        if(!raw.isEmpty()){try{JSONObject cached=new JSONObject(raw);if(cached.has("tier")&&cached.has("settlements"))return safeDisplay(cached);}catch(JSONException invalid){/* Recover below. */}}
        if(!c.getDatabasePath(NAME).exists())return null;
        try(RpgProgressStore helper=new RpgProgressStore(c);Cursor cursor=helper.getReadableDatabase().rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{account})){
            if(cursor.moveToFirst()){
                JSONObject restored=safeDisplay(display(new JSONObject(cursor.getString(0))));
                // Rebuild only the small display cache after restart/cache loss.
                if(!account.equals(SubscriptionStore.accountKey(c)))return null;
                c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(account,restored.toString()).commit();
                return restored;
            }
        }return null;}
    }catch(Exception ignored){return null;}}
    static int tier(Context c){JSONObject p=peek(c),r=p==null?null:p.optJSONObject("rpg"),t=r==null?null:r.optJSONObject("KR");if(t==null||t.optInt("tier",-1)<0)t=p==null?null:p.optJSONObject("tier");int tier=t==null?-1:t.optInt("tier",-1);return tier>=0&&tier<=9?tier:-1;}
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
                    prepareJourneys(doc,System.currentTimeMillis());
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
                    boolean promotedActive=BetaGrowthStore.activeWorker(c,account);JSONObject promotedPrior=promotedActive?new JSONObject(RpgEngine.state(doc).toString()):null;
                    AccountProgress.reconcile(doc,data.records,data.days,credits,bills,UsageLedgerDatabase.lanFloor(c),now,evaluate);
                    if(promotedActive)BetaLegacy.freezeGrowth(promotedPrior,RpgEngine.state(doc));
                    db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{account,doc.toString()});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                JSONObject display=display(doc);
                if(!c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(account,display.toString()).commit())throw new IllegalStateException("Progress display write failed; durable journal preserved");
            }
        }
    }
    private static JSONObject display(JSONObject doc)throws JSONException {
        JSONObject result=new JSONObject().put("tier",doc.getJSONObject("tier")).put("settlements",doc.getJSONObject("settlements"));
        result.put("billing",doc.getJSONObject("billing"));
        if(doc.getJSONObject("future").has("showcase"))result.put("showcase",doc.getJSONObject("future").getJSONObject("showcase"));
        if(doc.getJSONObject("future").has("rpg"))result.put("rpg",RpgEngine.summary(doc));return result;
    }
    static JSONObject progress(Context c){JSONObject p=peek(c);return p==null?null:p.optJSONObject("rpg");}
    static JSONObject archive(Context c,String expected)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){if(expected==null||!expected.equals(SubscriptionStore.accountKey(c)))throw new IllegalStateException("Account changed");ensureJourney(c,expected);
            try(RpgProgressStore h=new RpgProgressStore(c);Cursor row=h.getReadableDatabase().rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{expected})){
                if(!row.moveToFirst())throw new IllegalStateException("No achievement journal");JSONObject doc=new JSONObject(row.getString(0));
                JSONObject show=doc.getJSONObject("future").optJSONObject("showcase");if(show==null)show=new JSONObject().put("ids",new JSONArray(RpgShowcase.selected(c,RpgEngine.state(doc)))).put("custom",c.getSharedPreferences("codex_rpg_showcase_v1",0).getBoolean(expected+":custom",false));
                return RpgArchive.create(doc,expected,show.getJSONArray("ids"),show.getBoolean("custom"),System.currentTimeMillis());
            }
        }
    }
    // Worker-side acknowledgement is required before reporting a completed restore.
    @android.annotation.SuppressLint("ApplySharedPref")
    static boolean restoreArchive(Context c,String expected,JSONObject file)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){if(expected==null||!expected.equals(SubscriptionStore.accountKey(c)))return false;backupStep2(c);JSONObject document;
            try(RpgProgressStore h=new RpgProgressStore(c)){SQLiteDatabase db=h.getWritableDatabase();db.beginTransaction();try{
                String raw="";try(Cursor row=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{expected})){if(row.moveToFirst())raw=row.getString(0);}
                document=raw.isEmpty()?AccountProgress.empty():new JSONObject(raw);prepareJourneys(document,System.currentTimeMillis());document=RpgArchive.merge(document,file,expected,System.currentTimeMillis());
                prepareJourneys(document,System.currentTimeMillis());
                JSONObject ownShow=document.getJSONObject("future").optJSONObject("showcase"),fileShow=file.getJSONObject("showcase");
                if((ownShow==null||!ownShow.optBoolean("custom"))&&!c.getSharedPreferences("codex_rpg_showcase_v1",0).getBoolean(expected+":custom",false)){
                    JSONArray ids=fileShow.getJSONArray("ids"),kept=new JSONArray();List<String> eligible=RpgGrowthFacts.earned(RpgEngine.state(document));for(int i=0;i<ids.length();i++)if(eligible.contains(ids.getString(i)))kept.put(ids.getString(i));
                    if(kept.length()>0||ids.length()==0)document.getJSONObject("future").put("showcase",new JSONObject(fileShow.toString()).put("ids",kept));
                }
                db.execSQL("INSERT OR REPLACE INTO recovery(account,captured_at,document) VALUES(?,?,?)",new Object[]{expected,System.currentTimeMillis(),raw.isEmpty()?AccountProgress.empty().toString():raw});
                db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{expected,document.toString()});db.setTransactionSuccessful();
            }finally{db.endTransaction();}}
            // Invalidate the disposable cache; a failure to cache cannot undo a committed restore.
            c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(expected).commit();
            c.getSharedPreferences("codex_rpg_showcase_v1",0).edit().putString(RpgShowcase.seenKey(expected,RpgEngine.state(document)),new JSONArray(RpgGrowthFacts.earned(RpgEngine.state(document))).toString()).putString(expected+":titles_seen",new JSONArray(RpgEngine.eligibleTitles(document)).toString()).commit();
            return true;
        }
    }
    // Durable SQLite is authoritative; invalidate its derived cache before completion.
    @android.annotation.SuppressLint("ApplySharedPref")
    static boolean saveShowcase(Context c,String expected,List<String> ids)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){if(expected==null||!expected.equals(SubscriptionStore.accountKey(c)))return false;ensureJourney(c,expected);JSONObject document;
            try(RpgProgressStore h=new RpgProgressStore(c)){SQLiteDatabase db=h.getWritableDatabase();db.beginTransaction();try{
                try(Cursor row=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{expected})){if(!row.moveToFirst())return false;document=new JSONObject(row.getString(0));}
                if(ids.size()>3||new HashSet<>(ids).size()!=ids.size()||!RpgGrowthFacts.earned(RpgEngine.state(document)).containsAll(ids))return false;
                document.getJSONObject("future").put("showcase",new JSONObject().put("ids",new JSONArray(ids)).put("custom",true));
                db.execSQL("UPDATE profiles SET document=? WHERE account=?",new Object[]{document.toString(),expected});db.setTransactionSuccessful();
            }finally{db.endTransaction();}}
            c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(expected).commit();return true;
        }
    }
    private static void backupStep2(Context c)throws IOException {
        File root=new File(c.getFilesDir(),"rpg-step2-recovery");if(new File(root,"complete").exists())return;
        if(!root.exists()&&!root.mkdirs())throw new IOException("Progress recovery unavailable");
        File folder=new File(root,"snapshot-"+UUID.randomUUID());if(!folder.mkdirs())throw new IOException("Recovery snapshot unavailable");
        File db=c.getDatabasePath(NAME);for(String suffix:new String[]{"","-wal","-shm"}){File f=new File(db.getPath()+suffix);if(f.exists())Files.copy(f.toPath(),new File(folder,f.getName()).toPath());}
        Files.write(new File(root,"complete").toPath(),folder.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    static void tokens(Context c,String expected,JSONObject envelope)throws Exception {mutate(c,expected,envelope,null,null);}
    static boolean equip(Context c,String expected,String title)throws Exception {return mutate(c,expected,null,title,null);}
    static boolean recommend(Context c,String expected)throws Exception {return mutate(c,expected,null,"@recommend",null);}
    static boolean restore(Context c,String expected,JSONObject journal)throws Exception {return mutate(c,expected,null,null,journal);}
    static JSONObject export(Context c,String expected)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){String account=SubscriptionStore.accountKey(c);if(account==null||!account.equals(expected)||!c.getDatabasePath(NAME).exists())return null;ensureJourney(c,expected);
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
                    prepareJourneys(document,System.currentTimeMillis());
                    boolean changed=journal!=null?RpgEngine.merge(document,account,journal,System.currentTimeMillis()):envelope!=null?RpgEngine.ingestOfficial(document,account,envelope,System.currentTimeMillis()):title.equals("@recommend")?!RpgEngine.recommend(document,new Random()).isEmpty():RpgEngine.equip(document,title);
                    if(!changed)return false;
                    db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{account,document.toString()});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
            }
            if(!c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(account,display(document).toString()).commit())throw new IllegalStateException("RPG display failed; journal preserved");return true;
        }
    }
    private static JSONObject safeDisplay(JSONObject p)throws JSONException {
        JSONObject s=p.optJSONObject("rpg");if(s!=null&&s.optInt("collection_revision")<CollectionRestart.REVISION){s.put("achievement_enabled",false).put("title_enabled",false).put("achievement_started_at",0).put("title_started_at",0).put("unlocked_achievements",0).put("unlocked_ids",new JSONArray()).put("unlocked_titles",new JSONArray());s.remove("equipped_title");s.remove("recommended_title");p.put("showcase",new JSONObject().put("ids",new JSONArray()).put("custom",false));}
        if(s!=null&&!s.has("achievement_enabled")){s.put("unlocked_achievements",0).put("unlocked_ids",new JSONArray());JSONArray old=s.optJSONArray("unlocked_titles"),keep=new JSONArray();if(old!=null)for(int i=0;i<old.length();i++)if(RpgGrowthFacts.entry(old.optString(i))==null)keep.put(old.optString(i));s.put("unlocked_titles",keep);if(RpgGrowthFacts.entry(s.optString("equipped_title"))!=null)s.remove("equipped_title");p.put("showcase",new JSONObject().put("ids",new JSONArray()).put("custom",true));}return p;
    }
    private static void ensureJourney(Context c,String expected)throws Exception {
        try(RpgProgressStore h=new RpgProgressStore(c)){SQLiteDatabase db=h.getWritableDatabase();db.beginTransaction();try{JSONObject doc;try(Cursor row=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{expected})){if(!row.moveToFirst())return;doc=new JSONObject(row.getString(0));}if(prepareJourneys(doc,System.currentTimeMillis())){db.execSQL("UPDATE profiles SET document=? WHERE account=?",new Object[]{doc.toString(),expected});c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(expected).commit();}db.setTransactionSuccessful();}finally{db.endTransaction();}}
        c.getSharedPreferences("codex_rpg_display_v1",0).edit().putString(expected+":collection_revision",Integer.toString(CollectionRestart.REVISION)).commit();
    }
    private static boolean prepareJourneys(JSONObject d,long now)throws Exception{if(TitleJourney.journey(RpgEngine.state(d))==null&&!d.getJSONObject("future").has("reborn_before_migration"))d.getJSONObject("future").put("reborn_before_migration",new JSONObject(RpgEngine.state(d).toString()));boolean changed=AchievementJourney.prepare(d,now);changed=TitleJourney.prepare(d,now)||changed;return CollectionRestart.prepare(d,now)||changed;}
    static boolean resetCollection(Context c,String expected,boolean titles)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){if(expected==null||!expected.equals(SubscriptionStore.accountKey(c)))return false;backupStep2(c);
            try(RpgProgressStore h=new RpgProgressStore(c)){SQLiteDatabase db=h.getWritableDatabase();db.beginTransaction();try{
                JSONObject doc;try(Cursor row=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{expected})){if(!row.moveToFirst())return false;doc=new JSONObject(row.getString(0));}
                prepareJourneys(doc,System.currentTimeMillis());CollectionRestart.reset(doc,titles,System.currentTimeMillis(),true);
                db.execSQL("UPDATE profiles SET document=? WHERE account=?",new Object[]{doc.toString(),expected});db.setTransactionSuccessful();
            }finally{db.endTransaction();}}
            c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(expected).commit();return true;
        }
    }
    static boolean setAchievementsEnabled(Context c,String expected,boolean enabled)throws Exception{return setCollectionEnabled(c,expected,enabled,false);}
    static boolean setTitlesEnabled(Context c,String expected,boolean enabled)throws Exception{return setCollectionEnabled(c,expected,enabled,true);}
    private static boolean setCollectionEnabled(Context c,String expected,boolean enabled,boolean titles)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){if(expected==null||!expected.equals(SubscriptionStore.accountKey(c)))return false;JSONObject doc;
            try(RpgProgressStore h=new RpgProgressStore(c)){SQLiteDatabase db=h.getWritableDatabase();db.beginTransaction();try{String raw="";try(Cursor row=db.rawQuery("SELECT document FROM profiles WHERE account=?",new String[]{expected})){if(row.moveToFirst())raw=row.getString(0);}doc=raw.isEmpty()?AccountProgress.empty():new JSONObject(raw);prepareJourneys(doc,System.currentTimeMillis());if(titles)TitleJourney.setEnabled(doc,enabled,System.currentTimeMillis());else AchievementJourney.setEnabled(doc,enabled,System.currentTimeMillis());db.execSQL("INSERT OR REPLACE INTO profiles(account,document) VALUES(?,?)",new Object[]{expected,doc.toString()});db.setTransactionSuccessful();}finally{db.endTransaction();}}
            c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(expected).commit();
            if(enabled)c.getSharedPreferences("codex_rpg_showcase_v1",0).edit().putString(RpgShowcase.seenKey(expected,RpgEngine.state(doc)),new JSONArray(RpgGrowthFacts.earned(RpgEngine.state(doc))).toString()).commit();return true;
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
