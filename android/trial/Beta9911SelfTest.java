package dev.bennett.codexmeter;
import java.time.*;import java.util.*;import org.json.*;

public final class Beta9911SelfTest {
    static int checks;static final String ACCOUNT="synthetic-a";
    static void check(boolean b,String text){checks++;if(!b)throw new AssertionError(text);}
    static long time(String date){return LocalDate.parse(date).atTime(10,0).atZone(BetaV4.ZONE).toInstant().toEpochMilli();}
    static LedgerRecord sample(String plan,long at,double used,long reset){return new LedgerRecord("weekly",plan,at,used,Double.toString(used),reset,604800,"api_precise",false);}
    static void day(JSONObject s,String date,double delta)throws Exception {
        long at=time(date),reset=at+86400000L;
        check(BetaMedalModel.observe(s,ACCOUNT,sample("plus",at,10,reset),at),"baseline accepted");
        check(BetaMedalModel.observe(s,ACCOUNT,sample("plus",at+60000,10+delta,reset),at+60000),"increased observation");
    }
    static void rejects(JSONObject s,long now,String text){boolean refused=false;try{BetaMedalModel.validate(s,ACCOUNT,now);}catch(Exception expected){refused=true;}check(refused,text);}
    public static void main(String[] args)throws Exception {
        long start=time("2026-01-01"),now=time("2026-12-31");JSONObject s=BetaMedalModel.empty(ACCOUNT,start,false);
        for(int i=1;i<=7;i++)day(s,String.format("2026-01-%02d",i),10);
        check(s.getJSONObject("days").getJSONObject("2026-01-07").getInt("grade")==0,"Insufficient reference is ungraded, not invented gold");
        day(s,"2026-01-08",6);check(s.getJSONObject("days").getJSONObject("2026-01-08").getInt("grade")==1,"Bronze personal pace");
        long at=time("2026-01-08")+120000,reset=time("2026-01-08")+86400000;
        check(BetaMedalModel.observe(s,ACCOUNT,sample("plus",at,17,reset),at),"increment to silver");
        check(s.getJSONObject("days").getJSONObject("2026-01-08").getInt("grade")==2,"Silver70% reference");
        check(BetaMedalModel.observe(s,ACCOUNT,sample("plus",at+60000,20,reset),at+60000),"increment to gold");
        JSONObject d=s.getJSONObject("days").getJSONObject("2026-01-08");check(d.getInt("grade")==3&&d.getDouble("reference")==10,"Gold at fixed daily reference");
        String before=s.toString();check(!BetaMedalModel.observe(s,ACCOUNT,sample("plus",at+60000,20,reset),at+60000),"Retry ignored");check(before.equals(s.toString()),"Retry leaves all awards unchanged");
        check(!BetaMedalModel.observe(s,ACCOUNT,sample("plus",at,50,reset),at+60000),"Late observation ignored");
        check(BetaMedalModel.observe(s,ACCOUNT,sample("plus",at+120000,5,reset),at+120000),"Downward correction new baseline");check(d.getDouble("points")==10&&d.getInt("grade")==3,"Correction neither retracts nor creates points");
        check(BetaMedalModel.observe(s,ACCOUNT,sample("plus",at+150000,20,reset),at+150000),"Post-correction response accepted without further grade evaluation");check(d.getDouble("points")==10&&d.optBoolean("uncertain"),"Corrected usage cannot be counted again");
        check(BetaMedalModel.observe(s,ACCOUNT,sample("pro",at+180000,80,reset),at+180000),"Policy transition recorded");check(d.optBoolean("mixed")&&d.getDouble("points")==10,"No cross-plan subtraction");
        JSONObject scaled=BetaMedalModel.empty(ACCOUNT,start,false);for(int i=1;i<=7;i++){
            long stamp=time(String.format("2026-01-%02d",i));BetaMedalModel.observe(scaled,ACCOUNT,sample("pro",stamp,10,stamp+86400000),stamp);BetaMedalModel.observe(scaled,ACCOUNT,sample("pro",stamp+60000,12,stamp+86400000),stamp+60000);
        }
        long stamp=time("2026-01-08");BetaMedalModel.observe(scaled,ACCOUNT,sample("pro",stamp,10,stamp+86400000),stamp);BetaMedalModel.observe(scaled,ACCOUNT,sample("pro",stamp+60000,12,stamp+86400000),stamp+60000);
        check(scaled.getJSONObject("days").getJSONObject("2026-01-08").getInt("grade")==3,"Same personal pace at lower allowance percentage still gold; no plan multiplier");
        JSONObject gap=BetaMedalModel.empty(ACCOUNT,start,false);BetaMedalModel.observe(gap,ACCOUNT,sample("plus",start,10,start+86400000),start);
        BetaMedalModel.observe(gap,ACCOUNT,sample("plus",start+LedgerAggregation.MAX_GAP+1,60,start+86400000),start+LedgerAggregation.MAX_GAP+1);
        check(!gap.getJSONObject("days").getJSONObject("2026-01-01").getBoolean("active"),"Gap cannot award an activity day");
        long midnight=LocalDate.parse("2026-01-02").atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli();
        JSONObject cross=BetaMedalModel.empty(ACCOUNT,start,false);BetaMedalModel.observe(cross,ACCOUNT,sample("plus",midnight-60000,10,midnight+86400000),midnight-60000);
        BetaMedalModel.observe(cross,ACCOUNT,sample("plus",midnight+60000,20,midnight+86400000),midnight+60000);
        check(!cross.getJSONObject("days").getJSONObject("2026-01-02").getBoolean("active"),"Midnight delta is not attributed to today");
        JSONObject zero=BetaMedalModel.empty(ACCOUNT,start,false);BetaMedalModel.observe(zero,ACCOUNT,sample("plus",start,10,start+60000),start);BetaMedalModel.observe(zero,ACCOUNT,sample("plus",start+1000,10,start+60000),start+1000);
        check(zero.getJSONObject("days").getJSONObject("2026-01-01").getBoolean("measured")&&!zero.getJSONObject("days").getJSONObject("2026-01-01").getBoolean("active"),"Observed zero remains distinct from no observation");
        BetaMedalModel.observe(zero,ACCOUNT,sample("plus",start+120000,3,start+604800000),start+120000);BetaMedalModel.observe(zero,ACCOUNT,sample("plus",start+180000,4,start+604800000),start+180000);
        check(zero.getJSONObject("days").getJSONObject("2026-01-01").getDouble("points")==1,"Reset neither credits initial3% nor subtracts an old window");
        JSONObject leap=BetaMedalModel.empty(ACCOUNT,time("2028-02-28"),false);day(leap,"2028-02-28",1);day(leap,"2028-02-29",1);day(leap,"2028-03-01",1);
        check(BetaMedalModel.summary(leap,time("2028-03-01")+60000).getInt("month")==1,"Leap-month boundary");
        JSONObject year=BetaMedalModel.empty(ACCOUNT,time("2026-12-31"),false);day(year,"2026-12-31",1);day(year,"2027-01-01",1);
        check(BetaMedalModel.summary(year,time("2027-01-01")+60000).getInt("week")==2&&BetaMedalModel.summary(year,time("2027-01-01")+60000).getInt("month")==1,"Year crossing keeps Monday week and calendar month separate");
        JSONObject months=BetaMedalModel.empty(ACCOUNT,start,false);for(int m=1;m<=3;m++)for(int i=1;i<=14;i++)day(months,String.format("2026-%02d-%02d",m,i),1);
        check(months.getJSONObject("periods").getJSONObject("month:2026-01").getInt("grade")==3,"Monthly14 activity dates");
        check(months.getJSONObject("titles").has("medal.title1")&&months.getJSONObject("titles").has("medal.title3"),"Permanent titles from1/3 gold months");
        check(months.getJSONObject("honors").length()==3,"Three permanent honors");
        check(BetaMedalModel.grade(3,false)==1&&BetaMedalModel.grade(4,false)==2&&BetaMedalModel.grade(5,false)==3,"Weekly3/4/5");
        check(BetaMedalModel.grade(7,true)==1&&BetaMedalModel.grade(10,true)==2&&BetaMedalModel.grade(14,true)==3,"Monthly7/10/14");
        check(BetaMedalModel.personalGrade(.77,1.1)==2&&BetaMedalModel.personalGrade(.769999,1.1)==1&&BetaMedalModel.personalGrade(1.1,1.1)==3,"Decimal70%/100% boundaries");
        JSONObject merged=BetaMedalModel.merge(BetaMedalModel.empty(ACCOUNT,now,false),months,ACCOUNT,now);String once=merged.toString();
        for(int i=0;i<24;i++)merged=BetaMedalModel.merge(merged,months,ACCOUNT,now);
        check(merged.getJSONObject("days").toString().equals(new JSONObject(once).getJSONObject("days").toString()),"24 repeated restores do not duplicate days");
        check(merged.getJSONObject("periods").toString().equals(months.getJSONObject("periods").toString()),"Restores retain exactly one award per period");
        check(!BetaMedalModel.observe(merged,ACCOUNT,sample("plus",time("2026-03-14"),80,now+86400000),now),"Restored watermark rejects stale evidence");
        JSONObject bad=new JSONObject(months.toString()).put("access_token","not-real");rejects(bad,now,"Unknown/authentication field rejected");
        bad=new JSONObject(months.toString());bad.getJSONObject("days").getJSONObject("2026-01-01").put("access_token","not-real");rejects(bad,now,"Nested authentication rejected");
        rejects(new JSONObject(months.toString()).put("account","other"),now,"Account isolation");rejects(new JSONObject(months.toString()).put("version",2),now,"Unknown future schema retained");
        bad=new JSONObject(months.toString());bad.getJSONObject("days").getJSONObject("2026-01-01").put("points",-1);rejects(bad,now,"Negative evidence rejected");
        boolean denied=false;try{BetaMedalModel.merge(months,new JSONObject(months.toString()).put("demo",true),ACCOUNT,now);}catch(IllegalArgumentException expected){denied=true;}check(denied,"Demo/real restore isolation");
        check(!BetaMedalModel.observe(months,ACCOUNT,sample("plus",now+1,20,now+86400000),now),"Future response rejected");
        JSONObject summary=BetaMedalModel.summary(months,time("2026-03-14"));check(summary.getJSONObject("periods").length()<=2,"Home cache contains at most current2 periods, no history");
        JSONObject near=new JSONObject().put("date","2026-03-13").put("week",3).put("month",9).put("today",new JSONObject().put("active",true));
        check(!BetaMedalModel.nextPeriod(near,time("2026-03-13")).getBoolean("month"),"Equal remaining dates prefer this week's reachable medal");
        near.put("date","2026-03-15").put("week",0);check(BetaMedalModel.nextPeriod(near,time("2026-03-15")).getBoolean("month"),"Unreachable Sunday weekly target is not recommended");
        near.put("month",14).put("week",5);check(BetaMedalModel.nextPeriod(near,time("2026-03-15"))==null,"Completed periods fall back to existing lifetime goals");
        near.put("date","2026-03-31").put("month",9).put("week",3).put("today",new JSONObject().put("active",false));check(BetaMedalModel.nextPeriod(near,time("2026-03-31")).getBoolean("month"),"Equal effort prefers the earlier month-end deadline");
        check(!summary.has("total")&&!summary.has("xp")&&!summary.has("level"),"New medals never grant XP or alter levels");
        long coachNow=time("2026-01-08")+60000;JSONObject pace=BetaMedalModel.summary(scaled,coachNow);
        check(BetaMedalCoach.group(pace,"low",coachNow).equals("medal_week"),"Today's weekly medal precedes the daily grade");
        pace.put("periods",new JSONObject());
        check(BetaMedalCoach.group(pace,"low",coachNow).equals("medal_gold"),"Gold reaction from recorded personal grade");
        for(String guard:new String[]{"critical","missing","stale","network"})check(BetaMedalCoach.group(pace,guard,coachNow).equals(guard),"Usage guard precedes medals "+guard);
        check(BetaMedalCoach.group(pace,"low",coachNow+86400000).equals("low"),"Yesterday's medal is not today's reaction");
        if(args.length==2){
            JSONObject catalog=new JSONObject(java.nio.file.Files.readString(java.nio.file.Path.of(args[0]))),extra=new JSONObject(java.nio.file.Files.readString(java.nio.file.Path.of(args[1])));
            check(catalog.getJSONArray("quotes").length()==600&&extra.getJSONArray("quotes").length()==24,"600 original IDs +24 additive reactions");
            for(int i=0;i<extra.getJSONArray("quotes").length();i++)catalog.getJSONArray("quotes").put(extra.getJSONArray("quotes").get(i));
            CoachCatalog content=new CoachCatalog(catalog);check(content.tips.size()==40&&content.quotes.size()==624,"40 unchanged linked tips");
            for(String group:new String[]{"medal_record","medal_bronze","medal_silver","medal_gold","medal_week","medal_month"}){
                JSONObject chosen=content.migrate(new JSONObject());Set<String> ids=new HashSet<>();
                for(int i=0;i<40;i++){CoachCatalog.Quote q=content.select(chosen,group,"spicy","","medal-"+i,false,new Random(i));check(q!=null&&q.group.equals(group)&&content.tips.containsKey(q.tipId),"Medal reaction and real linked prompt");ids.add(q.id);}
                check(ids.size()==4,"Four stable choices per medal group");
                JSONArray hidden=new JSONArray();for(String id:ids)hidden.put(id);chosen.put("hidden",hidden);
                check(content.select(chosen,group,"spicy","","all-hidden",false,new Random(2))==null,"All hidden medal copy stays hidden");
            }
        }
        System.out.println("Beta9911 medals: "+checks+" checks passed; personal pace / plan boundaries / calendar / restore / no XP");
    }
}
