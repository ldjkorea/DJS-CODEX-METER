package dev.bennett.codexmeter;
import java.util.*;
import org.json.*;

/** Synthetic facts only. UI projection may never mutate the earned journal. */
public final class RpgGrowthUxSelfTest {
    static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        JSONObject state=new JSONObject().put("achievements",new JSONObject());JSONObject facts=state.getJSONObject("achievements");
        String[] earned={"days_1","days_7","tokens_1000000","tier_4"};
        for(int i=0;i<earned.length;i++)facts.put(earned[i],new JSONObject().put("unlocked",true).put("first_earned_at",100+i));
        facts.put("tier_9",new JSONObject().put("unlocked",false));String original=state.toString();
        check(RpgGrowthFacts.earned(state).size()==4,"Only actual unlocks are eligible");
        List<String> defaultIds=RpgGrowthFacts.showcase(state,null,false);check(defaultIds.size()==3,"Default is at most three earned badges");
        JSONArray invalid=new JSONArray(List.of("tier_9","days_1","days_1","unknown","days_7","tier_4","tokens_1000000"));
        check(RpgGrowthFacts.showcase(state,invalid,true).equals(List.of("days_1","days_7","tier_4")),"Locked, missing and duplicate IDs filtered before cap");
        check(RpgGrowthFacts.showcase(state,new JSONArray(),true).isEmpty(),"Explicit empty showcase is preserved");
        check(RpgGrowthFacts.showcase(null,invalid,true).isEmpty(),"No account facts do not produce a showcase");
        check(RpgGrowthFacts.recentAchievements(state).equals(List.of("tier_4","tokens_1000000","days_7")),"Only recorded first-confirmed times drive recent list");
        check(original.equals(state.toString()),"View projections never change earned data");
        JSONObject compact=new JSONObject().put("unlocked_ids",new JSONArray(earned));check(RpgGrowthFacts.showcase(compact,null,false).equals(defaultIds),"Home and growth use identical default badges");
        compact.put("unlocked_ids",new JSONArray(List.of("unknown","tier_9","tier_9")));check(RpgGrowthFacts.earned(compact).equals(List.of("tier_9")),"Compact cache ignores unknown IDs and duplicates");
        state.put("awards",new JSONObject().put("activity-day-count",60).put("token-day:2026-10-01",25).put("quota-day:2026-10-01",10).put("valid-week:1",50).put("achievement:days_1",30));
        check(Arrays.equals(RpgGrowthFacts.xpSources(state),new long[]{60,25,60,30}),"XP breakdown keeps original award scopes");
        JSONObject d=AccountProgress.empty();long now=System.currentTimeMillis();RpgEngine.reconcile(d,List.of(),List.of(),List.of(),List.of(),now,false);long xp=RpgEngine.state(d).getLong("xp");
        check(RpgGrowthFacts.earned(RpgEngine.summary(d)).isEmpty(),"No activity or cached score does not manufacture unlocks");
        check(RpgEngine.summary(d).has("unlocked_ids"),"Compact home cache includes earned IDs");
        RpgEngine.state(d).put("input_stamp","old-catalog");RpgEngine.reconcile(d,List.of(),List.of(),List.of(),List.of(),now,false);check(RpgEngine.state(d).getString("input_stamp").endsWith(RpgCatalog.REVISION),"Catalog update reconciles once without clearing history");
        RpgEngine.reconcile(d,List.of(),List.of(),List.of(),List.of(),now,false);check(RpgEngine.state(d).getLong("xp")==xp,"Empty catalog update and replay grant no XP");
        Set<String> ids=new HashSet<>(),titles=new HashSet<>();for(RpgCatalog.Entry e:RpgCatalog.ALL){check(ids.add(e.id),"Unique stable achievement ID");check(titles.add(e.title)&&!e.titleEn.isEmpty(),"Distinct bilingual title");check(e.target>0&&e.days>=0&&e.weeks>=0,"Measurable positive condition");}
        check(ids.size()==53&&titles.size()==53,"53 distinct achievements and titles");
        System.out.println("Growth UX: "+checks+" assertions passed; synthetic facts, unchanged earned-data contract.");
    }
}
