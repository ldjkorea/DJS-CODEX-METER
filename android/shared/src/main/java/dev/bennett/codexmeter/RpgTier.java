package dev.bennett.codexmeter;
import java.util.*;
import org.json.*;

/** Independent game scores. Eligibility means a certified full window with comparable inputs. */
public final class RpgTier {
    public static final double[] BOUNDS={10,20,30,45,60,75,85,92,97};
    public static final double KR_ADOPTION=.406,WORLD_ADOPTION=.188;
    public static final String SURVEY="Microsoft AI Diffusion Q2 2026; published 2026-09-28; working-age population 15-64; telemetry-adjusted, not token distribution";
    private RpgTier() {}
    public static JSONObject empty(String region)throws JSONException {return AccountTier.empty().put("model",RpgEngine.MODEL).put("region",region).put("weeks",new JSONObject()).put("label","game_estimate");}
    public static double score(String region,int days,int validWeeks,long monthlyTokens,Double utilization,boolean tokensKnown) {
        // Adoption data supplies context only; these benchmarks are explicitly game assumptions.
        double activityTarget=12+Math.round(30*(region.equals("KR")?KR_ADOPTION:WORLD_ADOPTION));
        double activity=Math.min(1,days/activityTarget)*75+Math.min(1,validWeeks/8d)*25;
        double intensity=Math.min(1,Math.log1p(Math.max(0,monthlyTokens)/1_000_000d)/Math.log1p(region.equals("KR")?3000d:2000d))*100;
        double value=.4*activity,weight=.4;
        if(tokensKnown){value+=.35*intensity;weight+=.35;}
        if(utilization!=null){value+=.25*Math.min(100,Math.max(0,utilization));weight+=.25;}
        return Math.max(0,Math.min(100,value/weight));
    }
    public static void current(JSONObject state,double score,long now,boolean enough,boolean changed)throws JSONException {
        state.put("latest_score",score).put("provisional",!enough).put("evaluated_at",now).put("model",RpgEngine.MODEL);
        if(!changed)return;int rank=AccountTier.rank(score);if(!enough)rank=Math.min(4,rank);
        AccountTier.promote(state,rank,now); // Only new positive facts promote; viewing/reset cannot demote.
    }
    public static void week(JSONObject state,String policy,long end,double score,boolean eligible)throws JSONException {
        if(!eligible||end<=state.optLong("end"))return;
        JSONObject weeks=state.getJSONObject("weeks");if(!policy.equals(state.optString("policy"))&&!state.optString("policy").isEmpty()){weeks=new JSONObject();state.put("weeks",weeks).put("low_streak",0).put("warning",false);}
        weeks.put(Long.toString(end),score);
        List<String> ends=AccountProgress.keys(weeks);ends.sort(Comparator.comparingLong(Long::parseLong));
        while(ends.size()>8)weeks.remove(ends.remove(0));
        double sum=0,weight=0;for(int i=0;i<ends.size();i++){double w=i>=ends.size()-4?2:1;sum+=weeks.getDouble(ends.get(i))*w;weight+=w;}double longScore=sum/weight;
        int tier=state.getInt("tier");boolean continuous=end-state.optLong("end")==TierEvolution.WEEK&&policy.equals(state.optString("policy"));
        boolean low=tier>0&&continuous&&end>state.optLong("protected_until")&&score<BOUNDS[tier-1]-3;
        int streak=low?state.optInt("low_streak")+1:0;
        if(streak>=3&&ends.size()>=4&&longScore<BOUNDS[tier-1]-3){state.put("tier",tier-1);streak=0;}
        state.put("end",end).put("policy",policy).put("score",longScore).put("count",ends.size()).put("low_streak",streak).put("warning",streak>=2);
        state.getJSONArray("evaluations").put(new JSONObject().put("kind","certified_game_week").put("end",end).put("score",score).put("long_score",longScore).put("tier",state.getInt("tier")).put("model",RpgEngine.MODEL));
    }
}
