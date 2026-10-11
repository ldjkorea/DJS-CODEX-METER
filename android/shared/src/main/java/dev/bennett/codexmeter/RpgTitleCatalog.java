package dev.bennett.codexmeter;

import java.util.*;
import org.json.*;

/** Independent profile decorations. No XP, no changes to the 53 achievement definitions. */
public final class RpgTitleCatalog {
    public static final class Title {
        public final RpgCatalog.Entry display;
        public final String[] medals;
        public final int days,weeks;
        Title(String id,String ko,String en,String rarity,int days,int weeks,String... medals){
            display=new RpgCatalog.Entry(id,ko,en,"special",1,ko,en,rarity,0,0,0);
            this.days=days;this.weeks=weeks;this.medals=medals;
        }
        public String description(boolean ko){List<String> labels=new ArrayList<>();for(String id:medals){RpgCatalog.Entry e=RpgGrowthFacts.entry(id);labels.add(e==null?id:ko?e.ko:e.en);}return (ko?"필요 메달: ":"Required medals: ")+String.join(" + ",labels)+(days>0?(ko?" · 확인된 활동 ":" · verified active days ")+days:"")+(weeks>0?(ko?" · 완전 주간 ":" · complete weeks ")+weeks:"");}
        public double ratio(JSONObject s){if(s==null)return 0;double result=1;JSONObject achievements=s.optJSONObject("achievements");for(String id:medals){JSONObject a=achievements==null?null:achievements.optJSONObject(id);result=Math.min(result,a==null?0:a.optBoolean("unlocked")?1:a.optDouble("progress_ratio",0));}if(days>0)result=Math.min(result,s.optInt("activity_days")/(double)days);if(weeks>0)result=Math.min(result,s.optInt("valid_weeks")/(double)weeks);return Math.max(0,Math.min(1,result));}
    }
    public static final List<Title> ALL=Collections.unmodifiableList(Arrays.asList(
        new Title("title_v25_log_keeper","잊혀진 로그의 수호자","Keeper of Forgotten Logs","rare",30,4,"days_30","weeks_4"),
        new Title("title_v25_reset_laugh","초기화를 비웃는 자","Laughing at Resets","epic",30,12,"weeks_12","value_100"),
        new Title("title_v25_sun_return","태양의 귀환","Return of the Sun","epic",100,12,"days_100","tokens_100000000"),
        new Title("title_v25_sky_archivist","천공의 기록관","Celestial Archivist","legendary",180,26,"days_180","weeks_26"),
        new Title("title_v25_third_eye","삼족오의 세 번째 눈","The Raven's Third Eye","epic",30,4,"tier_4","tokens_100000000"),
        new Title("title_v25_demolition_chief","한도 철거반 총책임자","Quota Demolition Chief","legendary",100,12,"value_200","weeks_12"),
        new Title("title_v25_false_phoenix","불사조인 척하는 삼족오","A Raven Playing Phoenix","epic",100,12,"plans_2","days_100"),
        new Title("title_v25_prompt_sovereign","프롬프트의 지배자","Sovereign of Prompts","legendary",180,26,"tokens_1000000000","tier_6"),
        new Title("title_v25_daily_sun","매일 태양을 깨우는 자 II","Daily Sun Waker II","epic",100,12,"streak_30","days_100"),
        new Title("title_v25_orbit_guard","태양 궤도의 수호자","Guardian of the Solar Orbit","mythic",365,52,"weeks_52","tier_7")
    ));
    private RpgTitleCatalog(){}
    public static Title find(String id){for(Title t:ALL)if(t.display.id.equals(id))return t;return null;}
    public static List<RpgCatalog.Entry> displayEntries(){List<RpgCatalog.Entry> result=new ArrayList<>(RpgCatalog.ALL);for(Title t:ALL)result.add(t.display);return result;}
    public static List<String> owned(JSONObject s){List<String> ids=new ArrayList<>(RpgGrowthFacts.earned(s));JSONObject titles=s==null?null:s.optJSONObject("titles");if(titles!=null)for(Title t:ALL)if(titles.optJSONObject(t.display.id)!=null&&titles.optJSONObject(t.display.id).optBoolean("unlocked"))ids.add(t.display.id);return ids;}
    public static void reconcile(JSONObject s,long now)throws JSONException {JSONObject titles=s.optJSONObject("titles");if(titles==null){titles=new JSONObject();s.put("titles",titles);}for(Title t:ALL){JSONObject fact=titles.optJSONObject(t.display.id);if(fact==null){fact=new JSONObject().put("unlocked",false);titles.put(t.display.id,fact);}double ratio=t.ratio(s);fact.put("progress",ratio==1?1:0).put("target",1).put("progress_ratio",Math.max(fact.optDouble("progress_ratio",0),ratio));if(!fact.optBoolean("unlocked")&&ratio>=1)fact.put("unlocked",true).put("first_earned_at",now).put("rule_version",1);}}
}
