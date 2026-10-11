package dev.bennett.codexmeter;

import java.util.*;
import org.json.*;

/** Account-owned achievements. Invalid/missing weeks and reset counters are never demotions. */
public final class AccountTier {
    public static final int VERSION=1;
    private static final double[] BOUNDS={10,20,30,45,60,75,85,92,97};
    public static JSONObject empty() throws JSONException {return new JSONObject().put("tier",-1).put("highest",-1).put("low_streak",0).put("end",0).put("protected_until",0).put("score",0).put("count",0).put("evaluations",new JSONArray()).put("rule_version",VERSION);}
    public static int rank(double percent){int r=0;while(r<9&&percent>=BOUNDS[r])r++;return r;}
    public static void promote(JSONObject state,int candidate,long at) throws JSONException {int tier=state.getInt("tier");if(candidate>tier&&candidate<=9){
        state.put("tier",candidate).put("highest",Math.max(candidate,state.getInt("highest"))).put("protected_until",at+2*TierEvolution.WEEK).put("low_streak",0);
        state.getJSONArray("evaluations").put(new JSONObject().put("kind","observed_promotion").put("at",at).put("tier",candidate).put("rule_version",VERSION));}}
    public static void evaluate(JSONObject state,List<FunInsights.Window> windows) throws JSONException {
        List<FunInsights.Window> ordered=new ArrayList<>(windows);ordered.sort(Comparator.comparingLong(w->w.span.end));
        List<FunInsights.Window> selected=new ArrayList<>();
        for(FunInsights.Window w:ordered){
            if(w.span.end-w.span.start!=TierEvolution.WEEK||!w.policy.startsWith("weekly|")||w.observations<3)continue;
            if(!selected.isEmpty()&&selected.get(selected.size()-1).span.end>=w.span.end)continue;selected.add(w);
            long end=w.span.end;if(end<=state.optLong("end"))continue;
            int n=selected.size();double sum=0,weight=0;for(int i=Math.max(0,n-8);i<n;i++){double k=i>=n-4?2:1;sum+=selected.get(i).used.doubleValue()*k;weight+=k;}
            double score=sum/weight;int tier=state.getInt("tier");long previous=state.optLong("end");
            boolean continuous=previous>0&&end-previous==TierEvolution.WEEK&&w.policy.equals(state.optString("policy"));
            boolean low=tier>0&&continuous&&end>state.optLong("protected_until")&&w.used.doubleValue()<BOUNDS[tier-1]-3;
            int streak=low?state.optInt("low_streak")+1:0;
            if(tier<0&&n>=2)promote(state,rank(score),end);
            else if(rank(score)>tier&&n>=4)promote(state,Math.min(tier+1,rank(score)),end);
            else if(streak>=3&&n>=4&&score<BOUNDS[tier-1]-3){state.put("tier",tier-1);streak=0;}
            if(state.getInt("tier")>tier)streak=0;
            state.put("low_streak",streak).put("warning",streak>=2).put("end",end).put("policy",w.policy).put("score",score).put("count",Math.min(8,n));
            state.getJSONArray("evaluations").put(new JSONObject().put("kind","valid_week").put("end",end).put("policy",w.policy).put("tier",state.getInt("tier")).put("highest",state.getInt("highest")).put("score",score).put("low_streak",streak).put("rule_version",VERSION));
        }
    }
}
