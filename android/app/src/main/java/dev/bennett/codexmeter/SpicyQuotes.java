package dev.bennett.codexmeter;

import android.content.Context;
import android.os.SystemClock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Local worker-only parsing and selection; the home thread reads immutable selections. */
final class SpicyQuotes {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Random RANDOM=new Random();
    private static final Map<String,Selection> CURRENT=new ConcurrentHashMap<>();
    private static final Object SELECTION_LOCK=new Object();
    private static CoachCatalog catalog;
    private static boolean loaded;
    private static volatile long generation=System.nanoTime();private static long away;
    static final class Selection {
        final String id,text,identity,group; final CoachCatalog.Tip tip;
        Selection(CoachCatalog.Quote q,CoachCatalog.Tip tip,String identity,String group){this.id=q.id;this.text=q.text;this.tip=tip;this.identity=identity;this.group=group;}
    }
    static synchronized void foreground(){if(away>0&&SystemClock.elapsedRealtime()-away>=30000)generation++;away=0;}
    static synchronized void background(boolean rotation){if(!rotation)away=SystemClock.elapsedRealtime();}
    static synchronized void manual(){generation++;}
    static String identity(Context c,String account){return account+":"+CoachPolicy.tone(c)+":"+interest(c)+":"+c.getResources().getConfiguration().getLocales().toLanguageTags()+":20261017-medals";}
    static String cacheKey(Context c,String account){return identity(c,account)+":"+generation+":"+medalStamp(c);}
    static String medalStamp(Context c){org.json.JSONObject summary=BetaMedalStore.peek(c);return summary==null?"":summary.toString();}
    static String interest(Context c){return c.getSharedPreferences("codex_fun_settings",0).getString("interest","");}
    static Selection peek(Context c,String account){Selection s=account==null?null:CURRENT.get(account);return s!=null&&s.identity.equals(identity(c,account))?s:null;}
    private static CoachCatalog load(Context c)throws Exception {
        if(!loaded){try(BufferedReader in=new BufferedReader(new InputStreamReader(c.getAssets().open("coach-ko-v2.json"),StandardCharsets.UTF_8))){StringBuilder s=new StringBuilder();for(String line;(line=in.readLine())!=null;)s.append(line);JSONObject combined=new JSONObject(s.toString());try(InputStream extra=c.getAssets().open("coach-ko-medals-v1.json")){JSONObject medal=new JSONObject(readExtra(extra));JSONArray rows=medal.getJSONArray("quotes");for(int i=0;i<rows.length();i++)combined.getJSONArray("quotes").put(rows.getJSONObject(i));}catalog=new CoachCatalog(combined);loaded=true;}}return catalog;
    }
    private static String readExtra(InputStream stream)throws Exception{BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));StringBuilder value=new StringBuilder();for(String line;(line=reader.readLine())!=null;)value.append(line);return value.toString();}
    static String select(Context c,String account,UsageSnapshot snapshot,boolean fresh,boolean bonus,double daily,long now){synchronized(SELECTION_LOCK){try{
        if(account==null)return null;
        if(!"spicy".equals(CoachPolicy.tone(c))||!c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ko")){CURRENT.remove(account);return null;}
        long epoch=generation;CoachCatalog content=load(c);String identity=identity(c,account);
        android.content.SharedPreferences prefs=c.getSharedPreferences("coach_v2",0);JSONObject state;
        try{state=new JSONObject(prefs.getString(account,""));}catch(Exception missing){String legacyKey=SubscriptionStore.key(c,snapshot);JSONObject old;try{old=new JSONObject(c.getSharedPreferences("spicy_quote_ids",0).getString(legacyKey,""));}catch(Exception ignored){old=new JSONObject();}state=content.migrate(old);}
        String group=BetaMedalCoach.group(BetaMedalStore.peek(c),CoachCatalog.group(snapshot,fresh,!AppPreferences.getLastError(c).isEmpty(),bonus,daily,now),now);
        CoachCatalog.Quote q=content.select(state,group,CoachPolicy.tone(c),interest(c),identity+":"+epoch+":"+group,false,RANDOM);
        if(epoch!=generation||!identity.equals(identity(c,account))||!account.equals(SubscriptionStore.accountKey(c)))return null;
        prefs.edit().putString(account,state.toString()).apply();
        if(q==null){CURRENT.remove(account);return null;}CURRENT.put(account,new Selection(q,content.tips.get(q.tipId),identity,group));return q.text;
    }catch(Exception ignored){if(account!=null)CURRENT.remove(account);return null;}}}
    static void rotate(Context c,String account,UsageSnapshot snapshot,boolean fresh,boolean bonus,double daily,boolean hide,Runnable done){
        String expected=identity(c,account);Selection shown=peek(c,account);
        WORKER.execute(()->{synchronized(SELECTION_LOCK){if(!expected.equals(identity(c,account))||!account.equals(SubscriptionStore.accountKey(c)))return;
            if(hide&&shown!=null)try{android.content.SharedPreferences p=c.getSharedPreferences("coach_v2",0);JSONObject s=new JSONObject(p.getString(account,"{}"));JSONArray h=s.optJSONArray("hidden");if(h==null)h=new JSONArray();h.put(shown.id);s.put("hidden",h);p.edit().putString(account,s.toString()).apply();}catch(Exception ignored){}
            manual();select(c,account,snapshot,fresh,bonus,daily,System.currentTimeMillis());new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(expected.equals(identity(c,account))&&account.equals(SubscriptionStore.accountKey(c)))done.run();});
        }});
    }
    static void restoreHidden(Context c,String account,Runnable done){WORKER.execute(()->{synchronized(SELECTION_LOCK){try{android.content.SharedPreferences p=c.getSharedPreferences("coach_v2",0);JSONObject s=new JSONObject(p.getString(account,"{}"));s.put("hidden",new JSONArray());p.edit().putString(account,s.toString()).apply();manual();}catch(Exception ignored){}}new android.os.Handler(android.os.Looper.getMainLooper()).post(done);});}
}
