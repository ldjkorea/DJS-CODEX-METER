package dev.bennett.codexmeter;
import java.time.*;import java.util.*;
/** Synthetic quota projections, independent of UI and Android stores. */
public final class Beta999SelfTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static long at(String s){return LocalDateTime.parse(s).atZone(LedgerAggregation.ZONE).toInstant().toEpochMilli();}
 static final long SECONDS=604800, RESET=at("2026-10-21T12:00:00");
 static UsageSnapshot snapshot(long fetched){return new UsageSnapshot("plus",true,false,new UsageWindow(30,18000,0,(fetched+9000000)/1000),new UsageWindow(45,SECONDS,0,RESET/1000),fetched);}
 static LedgerRecord row(String date,double value){return row(date,value,"plus","api_precise",RESET);}
 static LedgerRecord row(String date,double value,String plan,String source,long reset){return new LedgerRecord("weekly",plan,at(date),value,Double.toString(value),reset,SECONDS,source,false);}
 static BetaWeeklyUsage.Model project(List<LedgerRecord> rows){return BetaWeeklyUsage.project(snapshot(at("2026-10-17T18:00:00")),rows,at("2026-10-17T18:30:00"));}
 public static void main(String[] args)throws Exception{
  List<LedgerRecord> rows=new ArrayList<>(List.of(row("2026-10-14T13:00:00",10),row("2026-10-14T13:15:00",12.5),row("2026-10-16T11:00:00",40),row("2026-10-16T11:15:00",40)));
  BetaWeeklyUsage.Model m=project(rows);
  check(m.start==at("2026-10-14T12:00:00")&&m.end==RESET,"Server reset cycle, not Monday week");check(m.days.size()==8,"Midday seven-day cycle spans eight dates");
  check(m.days.get(0).points==2.5&&m.days.get(0).measured,"Observed increase uses decimal precision");check(!m.days.get(1).measured&&m.days.get(1).observations==0,"Missing date is not zero");check(m.days.get(2).measured&&m.days.get(2).points==0,"Measured zero is explicit");check(m.measuredDays==2&&m.recordedDays==2,"Coverage counts");check(m.days.get(4).future&&!m.days.get(3).future,"Upcoming uses real current date");
  check(project(List.of(row("2026-10-15T23:55:00",10),row("2026-10-16T00:05:00",15))).measuredDays==0,"No midnight interpolation");
  check(project(List.of(row("2026-10-15T01:00:00",10),row("2026-10-15T23:00:00",20))).measuredDays==0,"Gap excluded");
  check(project(List.of(row("2026-10-15T10:00:00",10),row("2026-10-15T10:05:00",8))).measuredDays==0,"Corrections excluded");
  for(String fence:new String[]{"legacy","plan","reset"}){
   LedgerRecord middle=row("2026-10-15T10:05:00",11,fence.equals("plan")?"pro":"plus",fence.equals("legacy")?"legacy":"api_precise",fence.equals("reset")?RESET+86400000:RESET);
   BetaWeeklyUsage.Model f=project(List.of(row("2026-10-15T10:00:00",10),middle,row("2026-10-15T10:10:00",12)));
   check(f.measuredDays==0,"Continuity fence "+fence);
  }
  check(project(List.of(row("2026-10-14T11:59:00",10),row("2026-10-14T12:01:00",11))).measuredDays==0,"Previous cycle excluded");
  check(project(List.of(row("2026-10-17T17:55:00",44),row("2026-10-17T18:05:00",45))).measuredDays==0,"Records newer than snapshot excluded");
  UsageSnapshot expired=snapshot(at("2026-10-17T18:00:00"));BetaWeeklyUsage.Model old=BetaWeeklyUsage.project(expired,rows,RESET+86400000);
  check(old.days.stream().noneMatch(d->d.future),"Stale snapshot never marks past dates upcoming");check(old.until==expired.fetchedAtMillis,"Expired snapshot not extended or reset to full quota");
  check(BetaWeeklyUsage.project(null,rows,RESET).days.isEmpty(),"No snapshot");check(BetaWeeklyUsage.project(new UsageSnapshot("free",true,false,null,null,RESET),rows,RESET).days.isEmpty(),"No invented weekly window for monthly or missing source");
  String immutable=rows.get(0).toJson().toString();project(rows);check(immutable.equals(rows.get(0).toJson().toString()),"No mutation");
  ArrayList<LedgerRecord> many=new ArrayList<>();long first=at("2026-10-16T01:00:00");for(int i=0;i<20000;i++)many.add(new LedgerRecord("weekly","plus",first+i*1000L,40+i/1000d,null,RESET,SECONDS,"api_precise",false));
  long begin=System.nanoTime();BetaWeeklyUsage.Model large=project(many);long ms=(System.nanoTime()-begin)/1000000;
  check(large.recordedDays==1&&large.measuredDays==1,"20k bounded observations");System.out.println("Beta999: "+checks+" checks passed;20k projection "+ms+"ms; reset/missing/zero/boundaries/read-only");
 }
}
