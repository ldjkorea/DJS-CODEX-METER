package dev.bennett.codexmeter;

import java.util.*;
import org.json.*;

/** Presentation only. Never edits the catalog, journal, unlocks or XP. */
public final class RpgCollectionFacts {
    public static final int ALL=0, EARNED=1, PROGRESS=2, LOCKED=3;
    private RpgCollectionFacts(){}
    public static JSONObject fact(JSONObject state,RpgCatalog.Entry e){JSONObject all=state==null?null:state.optJSONObject("achievements");return all==null?null:all.optJSONObject(e.id);}
    public static boolean earned(JSONObject s,RpgCatalog.Entry e){JSONObject f=fact(s,e);return f!=null&&f.optBoolean("unlocked");}
    public static int category(RpgCatalog.Entry e){switch(e.metric){case "tokens":return 1;case "days":case "streak":case "weeks":case "weekend":return 2;case "tier":return 3;default:return 4;}}
    public static int rarity(RpgCatalog.Entry e){switch(e.rarity){case "rare":return 1;case "epic":return 2;case "legendary":return 3;case "mythic":return 4;default:return 0;}}
    public static int index(String id){for(int i=0;i<RpgCatalog.ALL.size();i++)if(RpgCatalog.ALL.get(i).id.equals(id))return i;return -1;}
    public static boolean known(JSONObject state,RpgCatalog.Entry e){
        JSONObject f=fact(state,e);if(f==null||!f.has("progress"))return false;
        if(earned(state,e)||f.optLong("progress")>0)return true; // Previously confirmed lower bounds survive missing live data.
        if(e.metric.equals("tokens"))return state.optString("token_status").equals("observed");
        if(e.metric.equals("utilization"))return state.has("quota_score_basis")&&!state.optString("quota_score_basis").equals("unavailable");
        // A freshly created empty profile is not evidence of zero historical activity.
        return state.optInt("activity_days")>0||state.optInt("valid_weeks")>0;
    }
    public static double ratio(JSONObject s,RpgCatalog.Entry e){JSONObject f=fact(s,e);return earned(s,e)?1:known(s,e)?Math.max(0,Math.min(1,f.optDouble("progress_ratio"))):-1;}
    public static boolean progressing(JSONObject s,RpgCatalog.Entry e){JSONObject f=fact(s,e);return !earned(s,e)&&f!=null&&(ratio(s,e)>0||f.optLong("progress")>0||(e.days>0&&f.optInt("progress_days")>0)||(e.weeks>0&&f.optInt("progress_weeks")>0));}
    public static long time(JSONObject s,RpgCatalog.Entry e){JSONObject f=fact(s,e);return f==null?0:Math.max(0,f.optLong("first_earned_at"));}
    public static int initial(JSONObject s){for(RpgCatalog.Entry e:RpgCatalog.ALL)if(progressing(s,e))return PROGRESS;return RpgGrowthFacts.earned(s).isEmpty()?ALL:EARNED;}
    public static List<RpgCatalog.Entry> select(JSONObject s,int status,int category,int sort,String query,boolean titles,List<String> equipped){
        List<RpgCatalog.Entry> result=new ArrayList<>();String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        for(RpgCatalog.Entry e:RpgCatalog.ALL){boolean unlocked=earned(s,e);
            if(status==EARNED&&!unlocked||status==LOCKED&&unlocked||status==PROGRESS&&!progressing(s,e))continue;
            if(category>0&&category(e)!=category)continue;
            if(!q.isEmpty()&&!(e.ko+" "+e.en+" "+e.title+" "+e.titleEn).toLowerCase(Locale.ROOT).contains(q))continue;
            result.add(e);
        }
        result.sort((a,b)->{
            int c=0;
            if(sort==1)c=Double.compare(ratio(s,b),ratio(s,a));
            else if(sort==2)c=Long.compare(time(s,b),time(s,a));
            else if(sort==3)c=Integer.compare(rarity(b),rarity(a));
            else {
                if(status==ALL)c=Boolean.compare(earned(s,a),earned(s,b)); // Upcoming goals first.
                if(c==0&&earned(s,a)&&earned(s,b)){c=Boolean.compare(equipped.contains(b.id),equipped.contains(a.id));if(c==0)c=Long.compare(time(s,b),time(s,a));if(c==0)c=Integer.compare(rarity(b),rarity(a));}
                if(c==0&&!earned(s,a)&&!earned(s,b)){c=Double.compare(ratio(s,b),ratio(s,a));if(c==0)c=Boolean.compare(b.days>0||b.weeks>0,a.days>0||a.weeks>0);}
            }
            return c!=0?c:Integer.compare(index(a.id),index(b.id));
        });return result;
    }
    public static boolean toggle(List<String> draft,String id,JSONObject s){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);if(e==null||!earned(s,e))return false;if(draft.contains(id)){draft.remove(id);return true;}if(draft.size()>=3)return false;draft.add(id);return true;}
    public static String recommend(JSONObject s,Random random){List<String> ids=RpgGrowthFacts.earned(s);return ids.isEmpty()?"":ids.get(random.nextInt(ids.size()));}
    /** Explicit catalog ordinal -> unique frame, central emblem and ornament. Stable across draws. */
    public static int[] motif(RpgCatalog.Entry e){int i=index(e.id);if(i<0)throw new IllegalArgumentException("Unknown achievement");return new int[]{i%4,(i/4)%7,i/28};}
}
