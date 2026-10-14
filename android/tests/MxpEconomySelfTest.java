package dev.bennett.codexmeter;

import java.time.*;
import java.util.*;
import org.json.*;

/** Exercises the same production economy used inside RpgProgressStore, with synthetic accounts only. */
public final class MxpEconomySelfTest {
    static final String A=String.join("",Collections.nCopies(64,"a")),B=String.join("",Collections.nCopies(64,"b"));static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static long at(LocalDate d){return d.atStartOfDay(MxpEconomy.ZONE).toInstant().toEpochMilli();}
    static JSONObject fresh(LocalDate start,long xp,boolean core,boolean titles)throws Exception {
        JSONObject doc=AccountProgress.empty(),r=RpgEngine.state(doc);r.put("xp",xp).put("level",RpgEngine.level(xp));long now=at(start);
        AchievementJourney.prepare(doc,now);TitleJourney.prepare(doc,now);if(core)AchievementJourney.setEnabled(doc,true,now);if(titles)TitleJourney.setEnabled(doc,true,now);
        MxpEconomy.prepare(r,A,now);return r;
    }
    static boolean day(JSONObject m,LocalDate d,long observed)throws Exception {long t=at(d)+3600000;return MxpEconomy.accept(m,A,"synthetic:"+d,"codex",MxpEconomy.EvidenceStatus.VERIFIED,t,t+1000,observed,observed);}
    static Map<Integer,Integer> simulate(LocalDate start,Set<DayOfWeek> active,int horizon)throws Exception {
        JSONObject m=MxpEconomy.state(fresh(start,0,true,true));Map<Integer,Integer> result=new TreeMap<>();
        for(int i=0;i<horizon;i++){LocalDate d=start.plusDays(i);long now=at(d)+7200000;MxpEconomy.settle(m,now);if(active.contains(d.getDayOfWeek()))day(m,d,now);
            for(int level:new int[]{10,20,30,40,50,100})if(m.getInt("level")>=level&&!result.containsKey(level))result.put(level,i+1);if(result.containsKey(100))break;
        }return result;
    }
    static void rejected(RunnableThrowing action,String label)throws Exception {boolean failed=false;try{action.run();}catch(IllegalArgumentException|JSONException e){failed=true;}check(failed,label);}
    interface RunnableThrowing{void run()throws Exception;}
    public static void main(String[] args)throws Exception {
        long[] costs={5,10,10,15,15,40,130,200,650,700};int[] boundaries={9,10,19,20,29,30,39,40,49,50};
        for(int i=0;i<costs.length;i++)check(MxpEconomy.cost(boundaries[i])==costs[i],"cost "+boundaries[i]);
        int[] targets={10,20,30,40,50,100};long[] sums={45,145,295,1145,5395,40395};for(int i=0;i<targets.length;i++)check(MxpEconomy.threshold(targets[i])==sums[i],"cumulative");
        LocalDate start=LocalDate.of(2026,10,14);long now=at(start);JSONObject r=fresh(start,6050,true,true),m=MxpEconomy.state(r);
        check(m.getInt("level")==11&&MxpEconomy.progress(m)==5,"Lv11 half stays half");check(m.getJSONObject("legacy").getLong("xp")==6050,"original preserved");
        check(!MxpEconomy.prepare(r,A,now+10),"one migration");
        JSONObject fraction=MxpEconomy.state(fresh(start,399,true,true));check(Math.abs(MxpEconomy.progress(fraction)-1.65)<1e-12,"33/20 credit");
        JSONObject high=MxpEconomy.state(fresh(start,RpgEngine.threshold(150)+123,true,true));check(high.getInt("level")==150,"no cap/downgrade");
        long observed=now+7200000;day(m,start,observed);long total=m.getLong("total");day(m,start,observed+1);check(m.getLong("total")==total,"duplicate event");
        check(!MxpEconomy.accept(m,A,"click","chatgpt",MxpEconomy.EvidenceStatus.SELF_REPORTED,now,now+1,observed,observed),"self report no XP");
        check(!MxpEconomy.accept(m,A,"future","codex",MxpEconomy.EvidenceStatus.VERIFIED,now,observed+1,observed,observed),"future evidence");
        check(!MxpEconomy.accept(m,A,"old","codex",MxpEconomy.EvidenceStatus.VERIFIED,now-1,now+1,observed,observed),"pre-cutover");
        rejected(()->MxpEconomy.accept(m,B,"wrong","codex",MxpEconomy.EvidenceStatus.VERIFIED,now,now+1,observed,observed),"account isolation");
        rejected(()->MxpEconomy.accept(m,A,"synthetic:"+start,"codex",MxpEconomy.EvidenceStatus.VERIFIED,at(start.plusDays(1)),at(start.plusDays(1))+1,at(start.plusDays(2)),at(start.plusDays(2))),"same ID changed date");
        for(boolean core:new boolean[]{false,true})for(boolean title:new boolean[]{false,true}){JSONObject x=MxpEconomy.state(fresh(start,0,core,title));day(x,start,observed);check(x.getLong("total")== (core?6:2),"four switches XP");check(x.optInt("title_days")== (title?1:0),"four switches title");}
        JSONObject off=MxpEconomy.state(fresh(start,0,true,true));MxpEconomy.switches(off,false,false,now+5000);long late=at(start.plusDays(3));
        check(MxpEconomy.accept(off,A,"late","codex",MxpEconomy.EvidenceStatus.VERIFIED,now+1000,now+2000,late,late),"late ON evidence");check(off.getLong("total")==6,"late period credit");
        JSONObject boundary=MxpEconomy.state(fresh(start,0,true,true));MxpEconomy.switches(boundary,false,false,now+1500);MxpEconomy.accept(boundary,A,"cross","codex",MxpEconomy.EvidenceStatus.VERIFIED,now+1000,now+2000,late,late);check(boundary.getLong("total")==2,"cannot interpolate ON interval");
        check(MxpEconomy.week(LocalDate.of(2027,1,1)).equals("2026-12-28"),"ISO year boundary");
        Set<DayOfWeek> weekdays=EnumSet.range(DayOfWeek.MONDAY,DayOfWeek.FRIDAY),daily=EnumSet.allOf(DayOfWeek.class);
        for(int year:new int[]{2027,2028}){int min=100,max=0;for(LocalDate d=LocalDate.of(year,1,1);d.getYear()==year;d=d.plusDays(1)){Map<Integer,Integer> sim=simulate(d,weekdays,30);check(sim.containsKey(30),"Lv30 by day30 "+d);min=Math.min(min,sim.get(30));max=Math.max(max,sim.get(30));}System.out.println(year+" every start weekday Lv30="+min+".."+max);}
        for(int year:new int[]{2027,2028}){int min=100,max=0;for(LocalDate d=LocalDate.of(year,1,1);d.getYear()==year;d=d.plusDays(1)){Map<Integer,Integer> sim=simulate(d,daily,30);check(sim.containsKey(30),"Daily Lv30 by day30 "+d);min=Math.min(min,sim.get(30));max=Math.max(max,sim.get(30));}System.out.println(year+" every start daily Lv30="+min+".."+max);}
        List<Set<DayOfWeek>> cases=Arrays.asList(EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.THURSDAY),EnumSet.of(DayOfWeek.MONDAY,DayOfWeek.WEDNESDAY,DayOfWeek.FRIDAY),weekdays,daily);
        int[][] expected={{105,378,1480,-1},{41,150,605,4355},{24,92,384,2892},{16,76,372,2576}};
        for(int i=0;i<cases.size();i++){Map<Integer,Integer> result=simulate(start,cases.get(i),5000);for(int j=0;j<4;j++)check(result.getOrDefault(new int[]{30,40,50,100}[j],-1)==expected[i][j],"Python crosscheck "+i+" "+result);System.out.println(cases.get(i).size()+" days/week milestones "+result);}
        JSONObject left=fresh(start,0,true,true),right=new JSONObject(left.toString());for(int i=0;i<14;i++)day(MxpEconomy.state(left),start.plusDays(i),at(start.plusDays(20)));
        for(int i=13;i>=0;i--)day(MxpEconomy.state(right),start.plusDays(i),at(start.plusDays(20)));
        check(MxpEconomy.state(left).getLong("total")==MxpEconomy.state(right).getLong("total"),"reverse delivery invariant");
        long restoreAt=at(start.plusDays(21));MxpEconomy.validate(MxpEconomy.state(left),A,restoreAt);MxpEconomy.merge(left,right,A,restoreAt);long once=MxpEconomy.state(left).getLong("total");MxpEconomy.merge(left,right,A,restoreAt);check(MxpEconomy.state(left).getLong("total")==once,"restore twice no sum");
        JSONObject corrupt=new JSONObject(right.toString());MxpEconomy.state(corrupt).put("total",99999);rejected(()->MxpEconomy.merge(left,corrupt,A,restoreAt),"corrupt total");
        JSONObject conflict=fresh(start.plusDays(1),0,true,true);rejected(()->MxpEconomy.merge(left,conflict,A,restoreAt),"checkpoint conflict");
        JSONObject newer=new JSONObject(right.toString());MxpEconomy.state(newer).put("version",4);rejected(()->MxpEconomy.merge(left,newer,A,restoreAt),"newer schema");
        JSONObject fractional=new JSONObject(right.toString());MxpEconomy.state(fractional).put("total",MxpEconomy.state(right).getLong("total")+0.25);rejected(()->MxpEconomy.merge(left,fractional,A,restoreAt),"fractional corrupted integer");
        JSONObject rollback=new JSONObject(left.toString());rollback.put("xp",rollback.getLong("xp")+1);rejected(()->MxpEconomy.prepare(rollback,A,restoreAt),"Old binary XP write after cutover held, not converted twice");
        JSONObject gap=MxpEconomy.state(fresh(start,0,true,true));day(gap,start,observed);MxpEconomy.switches(gap,false,false,at(start.plusDays(1)));for(int i=1;i<8;i++)day(gap,start.plusDays(i),at(start.plusDays(10)));check(gap.getLong("total")==20&&gap.getInt("verified_days")==1,"OFF keeps base only and does not invent core progress");MxpEconomy.switches(gap,true,true,at(start.plusDays(8)));day(gap,start.plusDays(8),at(start.plusDays(10)));check(gap.getInt("verified_days")==2&&gap.getString("season_start").equals("2026-11-01"),"Resume does not reanchor season or backfill OFF");
        JSONObject leap=MxpEconomy.state(fresh(LocalDate.of(2028,2,28),0,true,true));day(leap,LocalDate.of(2028,2,29),at(LocalDate.of(2028,3,1)));check(leap.getJSONObject("months").getInt("2028-02")==1,"Leap day stays in February");
        JSONObject titleOnly=MxpEconomy.state(fresh(LocalDate.of(2027,1,1),0,false,true));for(int month=0;month<36;month++)for(int d=0;d<14;d++){LocalDate date=LocalDate.of(2027,1,1).plusMonths(month).plusDays(d);day(titleOnly,date,at(date.plusDays(1)));}MxpEconomy.settle(titleOnly,at(LocalDate.of(2030,1,1)));
        check(titleOnly.getJSONObject("titles").has("rpg3.title_chronicler")&&titleOnly.getJSONObject("titles").has("rpg3.title_orbit"),"Legendary and mythic title proof independent of achievements");check(titleOnly.getLong("total")==36*14*2&&titleOnly.getJSONObject("honors").length()==0,"Title decorations create no extra XP or achievement");MxpEconomy.validate(titleOnly,A,at(LocalDate.of(2030,1,1)));
        JSONObject noOn=fresh(start,0,false,false),moreOn=new JSONObject(noOn.toString());MxpEconomy.switches(MxpEconomy.state(moreOn),true,true,now+1);day(MxpEconomy.state(moreOn),start,observed);MxpEconomy.merge(noOn,moreOn,A,restoreAt);MxpEconomy.validate(MxpEconomy.state(noOn),A,restoreAt);check(MxpEconomy.state(noOn).getLong("started_at")==now+1,"Restore longer switch history also restores immutable anchors");
        check(MxpEconomy.annualTarget(12,0,0)==200&&MxpEconomy.annualTarget(12,8,0)==400&&MxpEconomy.annualTarget(12,12,10)==600&&MxpEconomy.annualTarget(0,0,0)==0,"Annual highest qualified tier only");
        JSONObject oldSeason=fresh(LocalDate.of(2027,1,1),0,true,true);for(int month=0;month<10;month++)for(int d=0;d<14;d++){LocalDate date=LocalDate.of(2027,1,1).plusMonths(month).plusDays(d);day(MxpEconomy.state(oldSeason),date,at(date.plusDays(1)));}
        long oldTotal=MxpEconomy.state(oldSeason).getLong("total"),nextYear=at(LocalDate.of(2028,1,1));MxpEconomy.validate(MxpEconomy.state(oldSeason),A,nextYear);JSONObject later=new JSONObject(oldSeason.toString());MxpEconomy.merge(later,oldSeason,A,nextYear);check(MxpEconomy.state(later).getLong("total")==oldTotal+600,"Old valid snapshot restored after annual boundary settles once");MxpEconomy.merge(later,oldSeason,A,nextYear);check(MxpEconomy.state(later).getLong("total")==oldTotal+600,"Old annual backup retry does not repay");
        long performanceStart=System.nanoTime();for(int i=0;i<10000;i++)MxpEconomy.summary(MxpEconomy.state(left),restoreAt);long ms=(System.nanoTime()-performanceStart)/1000000;System.out.println("MXP 10,000 small snapshots: "+ms+" ms; no evidence/history scan");
        System.out.println("Production MXP: "+checks+" assertions passed; synthetic only, not live migration proof.");
    }
}
