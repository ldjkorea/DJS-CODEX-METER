package dev.bennett.codexmeter;
import org.json.*;import java.time.*;import java.util.*;
public final class Beta995SelfTest {
    static int checks;static final String ACCOUNT="a".repeat(64);
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    interface Throwing{void run()throws Exception;}
    static void rejected(Throwing run,String label){boolean failed=false;try{run.run();}catch(Exception expected){failed=true;}check(failed,label);}
    static long at(LocalDate date){return date.atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli()+3600000;}
    static JSONObject simulate(LocalDate start,int days,int kind)throws Exception{
        JSONObject state=BetaV4.create(ACCOUNT,at(start)-1000);for(int i=0;i<days;i++){LocalDate date=start.plusDays(i);boolean collect=kind==0||kind==1&&date.getDayOfWeek().getValue()<=3||kind==2&&date.getDayOfMonth()%4==0&&date.getDayOfMonth()<=28||kind==3&&date.getDayOfWeek().getValue()<=5;if(collect)BetaV4.confirm(state,ACCOUNT,at(date),at(date),true);}return state;
    }
    public static void main(String[] args)throws Exception{
        LocalDate start=LocalDate.of(2027,1,4);long begin=at(start)-1000;JSONObject state=BetaV4.create(ACCOUNT,begin);
        check(state.getInt("level")==1&&state.getLong("total")==0,"Independent fresh record");check(BetaV4.threshold(30)==232,"First 29 levels cost 8 each");
        rejected(()->BetaV4.cost(0),"Negative/zero level");rejected(()->BetaV4.create(ACCOUNT,0),"Invalid cutover");
        String empty=state.toString();check(!BetaV4.confirm(state,ACCOUNT,at(start),at(start),false),"Cache/non-server does not count");check(empty.equals(state.toString()),"Cache unchanged");
        check(!BetaV4.confirm(state,ACCOUNT,begin,at(start),true),"Old event excluded");check(!BetaV4.confirm(state,ACCOUNT,at(start)+1,at(start),true),"Future response excluded");check(!BetaV4.confirm(state,ACCOUNT,at(start),at(start)+900001,true),"Stale response excluded");
        rejected(()->BetaV4.confirm(state,"b".repeat(64),at(start),at(start),true),"Wrong account excluded");
        check(BetaV4.confirm(state,ACCOUNT,at(start),at(start),true),"First fresh server response");check(state.getLong("total")==1,"Daily exactly +1");String one=state.toString();
        for(int i=0;i<30;i++)check(!BetaV4.confirm(state,ACCOUNT,at(start)+i+1,at(start)+i+1,true),"Same day retry");check(one.equals(state.toString()),"Retries no extra reward");
        for(int i=1;i<5;i++)BetaV4.confirm(state,ACCOUNT,at(start.plusDays(i)),at(start.plusDays(i)),true);
        check(state.getLong("total")==35,"5 daily +20 +10");check(state.getJSONObject("rewards").length()==7,"Unique period/stage IDs");check(state.getJSONObject("titles").has("rpg3.starter"),"First personalized title");
        state.put("equipped","rpg3.starter");JSONObject saved=new JSONObject(state.toString());long now=at(start.plusDays(7));JSONObject restored=BetaV4.restore(state,saved,ACCOUNT,now);
        check(restored.getLong("total")==35,"Restore idempotent");check(restored.getString("equipped").equals("rpg3.starter"),"Equipment preserved");
        JSONObject tampered=new JSONObject(saved.toString()).put("total",99);rejected(()->BetaV4.restore(state,tampered,ACCOUNT,now),"Tampered XP rejected");
        JSONObject tokenBackup=new JSONObject(saved.toString()).put("access_token","SYNTHETIC_ONLY");rejected(()->BetaV4.restore(state,tokenBackup,ACCOUNT,now),"Authentication fields refused in imported state");
        JSONObject fractional=new JSONObject(saved.toString()).put("total",35.5);rejected(()->BetaV4.restore(state,fractional,ACCOUNT,now),"Fractional claimed XP rejected");
        JSONObject badTime=new JSONObject(saved.toString());badTime.getJSONObject("honors").put("rpg3.first_day",-1);rejected(()->BetaV4.restore(state,badTime,ACCOUNT,now),"Invalid earned date rejected");
        JSONObject fakeTitle=new JSONObject(saved.toString());fakeTitle.getJSONObject("titles").put("rpg3.title_chronicler",now);rejected(()->BetaV4.restore(state,fakeTitle,ACCOUNT,now),"Unproved title rejected");
        JSONObject different=BetaV4.create(ACCOUNT,begin+1);rejected(()->BetaV4.restore(different,saved,ACCOUNT,now),"Different growth era rejected");
        JSONObject paused=new JSONObject(saved.toString()).put("enabled",false);check(!BetaV4.confirm(paused,ACCOUNT,at(start.plusDays(5)),now,true),"Paused recording");
        JSONObject seven=simulate(start,7,0);check(seven.getLong("total")==77,"7 daily +week30 +month40");
        check(BetaV4.summary(seven,at(start.plusDays(7))).getInt("day")==0,"New day has no cache certification");check(BetaV4.summary(seven,at(start.plusDays(30))).getInt("month")==0,"Month boundary independent");
        check(BetaV4.week(LocalDate.of(2027,1,1)).equals("2026-12-28"),"Week crosses calendar year");
        long midnight=LocalDate.of(2027,2,1).atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli();check(BetaV4.day(midnight-1).toString().equals("2027-01-31"),"Seoul midnight before");check(BetaV4.day(midnight).toString().equals("2027-02-01"),"Seoul midnight after");
        JSONObject merged=BetaV4.restore(seven,saved,ACCOUNT,now);check(merged.getLong("total")==77,"Old backup cannot erase new days");
        JSONObject modelDoc=new JSONObject().put("beta_v4",new JSONObject(saved.toString()));
        check(BetaGrowthModel.create(null,ACCOUNT,now,true,false,false).status==BetaGrowthModel.Status.BASELINE,"No baseline never pretends verified");
        check(BetaGrowthModel.create(modelDoc,"b".repeat(64),now,true,false,false).summary==null,"Cross-account summary never displayed");
        check(BetaGrowthModel.create(modelDoc,ACCOUNT,now,true,true,false).status==BetaGrowthModel.Status.ERROR,"Failed confirmation keeps saved balance with error");
        check(BetaGrowthModel.create(modelDoc,ACCOUNT,now,false,false,false).status==BetaGrowthModel.Status.STALE,"Automatic checking disabled is stale");
        check(BetaGrowthModel.create(modelDoc,ACCOUNT,at(start.plusDays(4)),true,false,false).status==BetaGrowthModel.Status.COMPLETE,"Actual checked day shows complete");
        check(BetaGrowthModel.create(modelDoc,ACCOUNT,at(start.plusDays(4))+1000,true,false,true).status==BetaGrowthModel.Status.DEMO,"Synthetic evidence marked demo");
        modelDoc.getJSONObject("beta_v4").put("enabled",false);check(BetaGrowthModel.create(modelDoc,ACCOUNT,now,true,false,false).status==BetaGrowthModel.Status.OFF,"Paused recording displayed");
        long performanceStart=System.nanoTime();for(int i=0;i<10000;i++)BetaV4.summary(seven,now);System.out.println("V4 10000 cached summaries: "+((System.nanoTime()-performanceStart)/1000000)+" ms; no network or DB");
        HashSet<String> ids=new HashSet<>();int honors=0,titles=0;for(String[] e:BetaCatalog.ALL){check(ids.add(e[0]),"Stable unique catalog ID");if(BetaCatalog.title(e[0])){titles++;check(BetaCatalog.name(e[0],"민수",true).contains("민수"),"Korean name template");check(BetaCatalog.name(e[0],"Alice",false).contains("Alice"),"English name template");}else honors++;}check(honors==11&&titles==5,"No padding collectibles");
        JSONObject before=new JSONObject().put("xp",77).put("mxp_v3",new JSONObject().put("total",77)).put("KR",new JSONObject().put("tier",3));JSONObject after=new JSONObject().put("xp",100).put("mxp_v3",new JSONObject().put("total",100)).put("KR",new JSONObject().put("tier",4)).put("token_observed_at",123);BetaLegacy.freezeGrowth(before,after);check(after.getLong("xp")==77&&after.getJSONObject("mxp_v3").getLong("total")==77,"Legacy growth frozen");check(after.getJSONObject("KR").getInt("tier")==4&&after.getLong("token_observed_at")==123,"Grade and raw observations continue");
        int earliest=99,latest=0;for(int year:new int[]{2027,2028})for(int day=1;day<=Year.of(year).length();day++){LocalDate date=LocalDate.ofYearDay(year,day);JSONObject sample=BetaV4.create(ACCOUNT,at(date)-1000);int reached=0;for(int i=0;i<30;i++){LocalDate d=date.plusDays(i);if(d.getDayOfWeek().getValue()<=5)BetaV4.confirm(sample,ACCOUNT,at(d),at(d),true);if(reached==0&&sample.getInt("level")>=30)reached=i+1;}check(reached>0,"Five days/week Lv30 within first30: "+date);earliest=Math.min(earliest,reached);latest=Math.max(latest,reached);}
        System.out.println("Every 2027/2028 start, five days/week Lv30="+earliest+".."+latest+" days");
        for(int kind=0;kind<3;kind++){System.out.println("Scenario "+(kind==0?"daily":kind==1?"three days/week":"seven days/month"));for(int days:new int[]{1,7,30,90,365}){JSONObject s=simulate(LocalDate.of(2027,1,1),days,kind);System.out.println(days+" days: XP="+s.getLong("total")+", level="+s.getInt("level")+", inside="+s.getLong("progress")+"/"+s.getLong("next_cost"));}}
        System.out.println("Beta 9.9.5 economy/catalog: "+checks+" checks passed; synthetic only");
    }
}
