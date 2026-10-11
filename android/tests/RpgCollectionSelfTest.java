package dev.bennett.codexmeter;
import java.util.*;import org.json.*;
public final class RpgCollectionSelfTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        JSONObject s=new JSONObject().put("activity_days",30).put("token_status","observed").put("quota_score_basis","actual_billing_period_utilization").put("achievements",new JSONObject());JSONObject facts=s.getJSONObject("achievements");
        for(int i=0;i<53;i++){RpgCatalog.Entry e=RpgCatalog.ALL.get(i);facts.put(e.id,new JSONObject().put("unlocked",i<5).put("progress",i<5?e.target:e.target/2).put("progress_ratio",i<5?1:.5).put("progress_days",10).put("progress_weeks",2).put("first_earned_at",i<5?100+i:0));}
        String before=s.toString();Set<String> patterns=new HashSet<>();int[] categories=new int[5];
        for(RpgCatalog.Entry e:RpgCatalog.ALL){check(patterns.add(Arrays.toString(RpgCollectionFacts.motif(e))),"Distinct geometry for "+e.id);categories[RpgCollectionFacts.category(e)]++;check(RpgCollectionFacts.index(e.id)>=0,"Stable item ID");}
        check(Arrays.equals(categories,new int[]{0,11,22,10,10}),"Metric-based categories cover exactly 53");
        check(RpgCollectionFacts.select(s,0,0,0,"",false,List.of()).size()==53,"All 53 visible");
        check(RpgCollectionFacts.select(s,1,0,0,"",false,List.of()).size()==5,"Earned only");
        check(RpgCollectionFacts.select(s,3,0,0,"",false,List.of()).size()==48,"Locked includes partially completed");
        check(RpgCollectionFacts.initial(s)==2,"In-progress default");
        for(int status=0;status<4;status++)for(int category=0;category<5;category++)for(int sort=0;sort<4;sort++){
            List<RpgCatalog.Entry> result=RpgCollectionFacts.select(s,status,category,sort,"",false,List.of("tokens_1000000"));
            check(result.equals(RpgCollectionFacts.select(s,status,category,sort,"",false,List.of("tokens_1000000"))),"Stable sorting");
            for(RpgCatalog.Entry e:result){check(category==0||RpgCollectionFacts.category(e)==category,"Intersected category");check(status!=1||RpgCollectionFacts.earned(s,e),"No locked in owned");check(status!=2||RpgCollectionFacts.progressing(s,e),"In-progress classification");check(status!=3||!RpgCollectionFacts.earned(s,e),"No earned in locked");}
            if(sort==1)for(int i=1;i<result.size();i++)check(RpgCollectionFacts.ratio(s,result.get(i-1))>=RpgCollectionFacts.ratio(s,result.get(i)),"Nearest sort");
        }
        check(RpgCollectionFacts.select(s,1,0,0,"TOKEN",true,List.of()).stream().allMatch(e->e.en.toLowerCase().contains("token")||e.titleEn.toLowerCase().contains("token")),"Title search supports English");
        List<String> draft=new ArrayList<>();for(int i=0;i<3;i++)check(RpgCollectionFacts.toggle(draft,RpgCatalog.ALL.get(i).id,s),"Three choices accepted");List<String> saved=new ArrayList<>(draft);
        check(!RpgCollectionFacts.toggle(draft,RpgCatalog.ALL.get(3).id,s)&&draft.equals(saved),"Fourth rejected without replacing");check(!RpgCollectionFacts.toggle(draft,RpgCatalog.ALL.get(10).id,s),"Locked cannot equip");
        check(RpgCollectionFacts.toggle(draft,saved.get(1),s)&&draft.size()==2,"Click removes");check(saved.size()==3,"Cancel does not mutate persisted selection");
        for(int i=0;i<100;i++)check(RpgCollectionFacts.earned(s,RpgGrowthFacts.entry(RpgCollectionFacts.recommend(s,new Random(i)))),"Random previews owned only");
        check(before.equals(s.toString()),"All view projections preserve original facts/XP/title");
        JSONObject unknown=new JSONObject().put("achievements",new JSONObject().put("tokens_1000000",new JSONObject().put("progress",0).put("progress_ratio",0)));RpgCatalog.Entry token=RpgCatalog.ALL.get(0);
        check(!RpgCollectionFacts.known(unknown,token)&&RpgCollectionFacts.ratio(unknown,token)==-1,"Unknown is not measured zero");unknown.put("token_status","observed");check(RpgCollectionFacts.ratio(unknown,token)==0,"Confirmed zero distinct");unknown.put("token_status","unknown");unknown.getJSONObject("achievements").getJSONObject(token.id).put("progress",10);check(RpgCollectionFacts.known(unknown,token),"Confirmed historical lower bound survives outage");
        check(RpgCollectionFacts.initial(null)==0&&RpgCollectionFacts.recommend(null,new Random()).isEmpty(),"Empty account safe");
        System.out.println("Medal codex/wardrobe: "+checks+" assertions passed; detached synthetic facts only.");
    }
}
