package dev.bennett.codexmeter;

import java.time.*;
import java.util.*;
import org.json.*;

/** Metadata only. Server aggregate and local processed-token counters have different semantics. */
public final class RpgTokens {
    private RpgTokens() {}
    public static JSONObject empty() throws JSONException {
        return new JSONObject().put("official",new JSONObject()).put("sessions",new JSONObject()).put("turns",new JSONObject());
    }
    public static boolean official(JSONObject state,String account,JSONObject envelope,long now) throws JSONException {
        if(envelope==null||!account.equals(envelope.optString("account"))||!"codex_account_usage_v1".equals(envelope.optString("source")))return false;
        long at=envelope.optLong("at");if(at<=0||at>now||now-at>86400000L)return false;
        JSONObject summary=envelope.optJSONObject("summary");if(summary==null)return false;
        JSONObject old=state.getJSONObject("official");if(at<old.optLong("at"))return false;
        JSONObject saved=new JSONObject().put("at",at).put("source","codex_account_usage_v1").put("date_zone","service_unknown")
            .put("basis","backend_token_activity; not raw input/output or billable tokens");
        for(String key:new String[]{"lifetimeTokens","peakDailyTokens","longestRunningTurnSec","currentStreakDays","longestStreakDays"}) {
            if(!summary.isNull(key)&&summary.has(key)){long value=summary.getLong(key);if(value<0)throw new IllegalArgumentException("Negative token metric");
                // A service correction cannot retract previously earned milestones.
                saved.put(key,key.equals("lifetimeTokens")?Math.max(value,old.optLong(key)):value);
            }else if(key.equals("lifetimeTokens")&&old.has(key))saved.put(key,old.getLong(key));
        }
        JSONObject days=old.optJSONObject("days");days=days==null?new JSONObject():new JSONObject(days.toString());
        JSONArray incoming=envelope.optJSONArray("dailyUsageBuckets");if(incoming!=null){if(incoming.length()>4000)throw new IllegalArgumentException("Token daily bound");
            for(int i=0;i<incoming.length();i++){JSONObject d=incoming.getJSONObject(i);String date=LocalDate.parse(d.getString("startDate")).toString();long value=d.getLong("tokens");
                if(value<0||LocalDate.parse(date).isAfter(LedgerAggregation.day(now).plusDays(1)))throw new IllegalArgumentException("Invalid token day");
                days.put(date,Math.max(value,days.optLong(date)));
            }
        }
        saved.put("days",days);state.put("official",saved);return true;
    }
    /** Caller must prove creator_account_id against the authenticated account before invoking this. */
    public static boolean local(JSONObject state,String account,JSONObject fact,long now) throws JSONException {
        if(!account.equals(fact.optString("account"))||!fact.optBoolean("identity_verified")||!"codex_thread_cumulative_v1".equals(fact.optString("source")))return false;
        String session=fact.getString("session"),turn=fact.getString("turn");
        if(!session.matches("[a-zA-Z0-9_-]{1,120}")||!turn.matches("[a-zA-Z0-9_-]{1,120}"))throw new IllegalArgumentException("Token identity");
        long at=fact.getLong("at");if(at<=0||at>now||fact.optBoolean("inherited_history"))return false;
        JSONObject total=fact.getJSONObject("total"),last=fact.getJSONObject("last"),sessions=state.getJSONObject("sessions");
        validate(total);validate(last);JSONObject previous=sessions.optJSONObject(session);
        if(previous!=null&&(at<previous.getLong("at")||total.getLong("totalTokens")<previous.getJSONObject("total").getLong("totalTokens")))return false;
        JSONObject clean=new JSONObject().put("at",at).put("total",clean(total)).put("turn",turn).put("source",fact.getString("source"));
        sessions.put(session,clean);
        // 'last' is the latest model response, not the sum of all responses in this turn.
        String key=session+":"+turn;JSONObject prior=state.getJSONObject("turns").optJSONObject(key);
        if(prior==null||at>=prior.getLong("at"))state.getJSONObject("turns").put(key,new JSONObject().put("at",at).put("last_response",clean(last)).put("source",fact.getString("source")));
        return true;
    }
    private static void validate(JSONObject v)throws JSONException {
        for(String k:new String[]{"inputTokens","cachedInputTokens","outputTokens","reasoningOutputTokens","totalTokens"})if(v.getLong(k)<0)throw new IllegalArgumentException("Negative token counter");
        if(v.getLong("cachedInputTokens")>v.getLong("inputTokens")||v.getLong("reasoningOutputTokens")>v.getLong("outputTokens")||Math.addExact(v.getLong("inputTokens"),v.getLong("outputTokens"))!=v.getLong("totalTokens"))throw new IllegalArgumentException("Token subset/total mismatch");
    }
    private static JSONObject clean(JSONObject v)throws JSONException {JSONObject out=new JSONObject();for(String k:new String[]{"inputTokens","cachedInputTokens","outputTokens","reasoningOutputTokens","totalTokens"})out.put(k,v.getLong(k));if(v.has("cacheWriteInputTokens")){long n=v.getLong("cacheWriteInputTokens");if(n<0)throw new IllegalArgumentException("Cache write counter");out.put("cacheWriteInputTokens",n);}return out;}
    public static long localTotal(JSONObject state)throws JSONException {long n=0;for(String key:AccountProgress.keys(state.getJSONObject("sessions")))n=Math.addExact(n,state.getJSONObject("sessions").getJSONObject(key).getJSONObject("total").getLong("totalTokens"));return n;}
    public static long milestoneTotal(JSONObject state)throws JSONException {JSONObject official=state.getJSONObject("official");return official.has("lifetimeTokens")?official.getLong("lifetimeTokens"):localTotal(state);}
    public static boolean known(JSONObject state)throws JSONException {return state.getJSONObject("official").has("lifetimeTokens")||state.getJSONObject("sessions").length()>0;}
    /** Metadata from the user's account-bound journal, not an unauthenticated server response. */
    public static void mergeJournal(JSONObject state,JSONObject other,String account,long now)throws JSONException {
        JSONObject imported=other.getJSONObject("official"),old=state.getJSONObject("official");long at=imported.optLong("at");
        if(at>0&&at<=now){JSONObject projected=new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",at).put("summary",new JSONObject());
            for(String k:new String[]{"lifetimeTokens","peakDailyTokens","longestRunningTurnSec","currentStreakDays","longestStreakDays"})if(imported.has(k))projected.getJSONObject("summary").put(k,imported.getLong(k));
            // Preserve the real capture time; accepting an old journal does not make its values fresh.
            JSONObject clean=empty();official(clean,account,projected,at);JSONObject verified=clean.getJSONObject("official");
            if(at>=old.optLong("at")){JSONObject previous=old;state.put("official",verified);old=verified;if(previous.has("lifetimeTokens"))old.put("lifetimeTokens",Math.max(old.optLong("lifetimeTokens"),previous.getLong("lifetimeTokens")));old.put("days",previous.optJSONObject("days")==null?new JSONObject():new JSONObject(previous.getJSONObject("days").toString()));}
            else if(verified.has("lifetimeTokens"))old.put("lifetimeTokens",Math.max(old.optLong("lifetimeTokens"),verified.getLong("lifetimeTokens")));
            JSONObject dates=old.optJSONObject("days");if(dates==null){dates=new JSONObject();old.put("days",dates);}JSONObject incoming=imported.optJSONObject("days");
            if(incoming!=null)for(String date:AccountProgress.keys(incoming)){LocalDate.parse(date);long n=incoming.getLong(date);if(n<0||LocalDate.parse(date).isAfter(LedgerAggregation.day(at).plusDays(1)))throw new IllegalArgumentException("Journal token date");dates.put(date,Math.max(n,dates.optLong(date)));}
        }
        JSONObject originalTurns=new JSONObject(state.getJSONObject("turns").toString());
        JSONObject sessions=other.getJSONObject("sessions");for(String session:AccountProgress.keys(sessions)){JSONObject row=sessions.getJSONObject(session);JSONObject fact=new JSONObject().put("account",account).put("identity_verified",true).put("source","codex_thread_cumulative_v1").put("session",session).put("turn",row.optString("turn","unknown")).put("at",row.getLong("at")).put("total",row.getJSONObject("total")).put("last",row.getJSONObject("total"));local(state,account,fact,now);}
        // Do not reinterpret a session total as an actual per-turn response during restoration.
        state.put("turns",originalTurns);JSONObject turns=originalTurns;
        JSONObject incomingTurns=other.getJSONObject("turns");for(String key:AccountProgress.keys(incomingTurns)){JSONObject row=incomingTurns.getJSONObject(key),prior=turns.optJSONObject(key);validate(row.getJSONObject("last_response"));long stamp=row.getLong("at");if(stamp>0&&stamp<=now&&(prior==null||stamp>=prior.getLong("at")))turns.put(key,new JSONObject().put("at",stamp).put("source","codex_thread_cumulative_v1").put("last_response",clean(row.getJSONObject("last_response"))));}
    }
}
