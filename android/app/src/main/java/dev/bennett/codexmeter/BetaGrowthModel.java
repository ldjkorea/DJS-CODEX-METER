package dev.bennett.codexmeter;

import org.json.*;

/** Detached beta presentation, constructed on GrowthActivity's worker. No writes or network. */
final class BetaGrowthModel {
    enum Status { ERROR, DEMO, OFF, BASELINE, STALE, PENDING, COMPLETE }
    final JSONObject summary;
    final Status status;
    final long lastObserved;
    final boolean collectionEnabled;
    private BetaGrowthModel(JSONObject summary,Status status,long lastObserved,boolean enabled){
        this.summary=summary;this.status=status;this.lastObserved=lastObserved;collectionEnabled=enabled;
    }
    static BetaGrowthModel create(JSONObject journal,String account,long now,boolean automatic,boolean fetchFailed,boolean demo){
        JSONObject r=journal==null?null:journal.optJSONObject("rpg"),m=MxpEconomy.state(r);
        boolean enabled=AchievementJourney.enabled(r);
        JSONObject summary=null;
        try {
            if(m==null||account==null||!account.equals(m.getString("account")))throw new IllegalArgumentException("Account scope");
            summary=MxpEconomy.summary(m,now);
            for(String key:new String[]{"day","week","month","level","next_cost"})if(!summary.has(key))throw new JSONException("Missing presentation field");
        }catch(Exception ignored){return new BetaGrowthModel(null,Status.ERROR,0,enabled);}
        // RpgEngine.export contains rpg, not AccountProgress's raw evidence dictionary.
        // Use only observation timestamps actually exported for this account, never the global snapshot.
        long last=r.optLong("quota_observed_at");if(last<0||last>now)last=0;
        JSONObject days=m.optJSONObject("days");
        if(days!=null){java.util.Iterator<String> keys=days.keys();while(keys.hasNext()){JSONObject day=days.optJSONObject(keys.next());if(day==null)continue;for(String key:new String[]{"base","core","title"}){JSONObject fact=day.optJSONObject(key);long at=fact==null?0:fact.optLong("observed_at");if(at>0&&at<=now)last=Math.max(last,at);}}}
        Status status=demo?Status.DEMO:fetchFailed?Status.ERROR:!enabled?Status.OFF:last==0?Status.BASELINE:
            !automatic||now-last>24L*60*60*1000?Status.STALE:summary.optBoolean("day_verified")?Status.COMPLETE:Status.PENDING;
        return new BetaGrowthModel(summary,status,last,enabled);
    }
    boolean known(){return summary!=null;}
    String label(){switch(status){
        case ERROR:return "확인 오류 · 저장된 기록은 유지됩니다";
        case DEMO:return "가상 데모 · 실제 계정 활동이 아닙니다";
        case OFF:return "명예 수집 꺼짐 · 기존 획득 기록 유지";
        case BASELINE:return "활동 기준 확인 전";
        case STALE:return "저장된 기록 · 최신 활동 확인 필요";
        case COMPLETE:return "오늘 활동 확인 완료";
        default:return "오늘 확인된 활동 없음";
    }}
    String daily(){return !known()?"확인 불가":summary.optBoolean("day_verified")?"확인 완료":
        status==Status.BASELINE?"기준 확인 전":status==Status.ERROR||status==Status.STALE?"최신 확인 필요":status==Status.OFF?"명예 수집 꺼짐":"아직 확인 전";}
    static int next(int count,int... stages){for(int stage:stages)if(count<stage)return stage;return -1;}
    static int rewardDelta(int count,boolean weekly){int next=weekly?next(count,3,5):next(count,7,10,14);
        return next<0?0:weekly?MxpEconomy.weeklyTarget(next)-MxpEconomy.weeklyTarget(count):MxpEconomy.monthlyTarget(next)-MxpEconomy.monthlyTarget(count);}
}
