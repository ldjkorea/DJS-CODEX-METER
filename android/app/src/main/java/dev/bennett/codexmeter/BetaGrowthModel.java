package dev.bennett.codexmeter;
import org.json.*;
final class BetaGrowthModel {
    enum Status { ERROR, DEMO, OFF, BASELINE, STALE, PENDING, COMPLETE }
    final JSONObject summary;final Status status;final long lastObserved;final boolean collectionEnabled;
    BetaGrowthModel(JSONObject summary,Status status){this.summary=summary;this.status=status;lastObserved=summary==null?0:summary.optLong("last_response");collectionEnabled=summary!=null&&summary.optBoolean("enabled");}
    static BetaGrowthModel create(JSONObject journal,String account,long now,boolean automatic,boolean failed,boolean demo){
        JSONObject state=journal==null?null:journal.optJSONObject("beta_v4");if(state==null)return new BetaGrowthModel(null,Status.BASELINE);
        try{BetaV4.validateIdentity(state,account);JSONObject summary=BetaV4.summary(state,now);JSONObject goal=BetaNextGoal.choose(state,now);String id=goal.optString("id");summary.put("next_goal",goal).put("next_id",id);if(!id.isEmpty()){int[] progress=BetaCatalog.progress(state,id,now);summary.put("goal_count",progress[0]).put("goal_target",progress[1]);}
            return new BetaGrowthModel(summary,demo?Status.DEMO:failed||summary.optLong("last_response")>now?Status.ERROR:!summary.getBoolean("enabled")?Status.OFF:summary.optLong("last_response")==0?Status.BASELINE:!automatic||now-summary.getLong("last_response")>86400000?Status.STALE:summary.getInt("day")>0?Status.COMPLETE:Status.PENDING);
        }catch(Exception ignored){return new BetaGrowthModel(null,Status.ERROR);}
    }
    boolean known(){return summary!=null;}
    String key(){switch(status){case ERROR:return "error";case DEMO:return "demo";case OFF:return "paused";case BASELINE:return "empty";case STALE:return "offline";case COMPLETE:return "checked_today";default:return "not_today";}}
}
