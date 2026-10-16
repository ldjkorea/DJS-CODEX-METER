package dev.bennett.codexmeter;

import java.time.LocalDate;
import java.util.*;

/** A read-only projection of the current server window, never a calendar-week estimate. */
final class BetaWeeklyUsage {
    static final class Day {
        final LocalDate date;
        final boolean future;
        int observations;
        double points;
        boolean measured;
        Day(LocalDate date, boolean future) { this.date=date; this.future=future; }
    }
    static final class Model {
        final long start,end,until;
        final List<Day> days;
        final int measuredDays,recordedDays;
        Model(long start,long end,long until,List<Day> days) {
            this.start=start;this.end=end;this.until=until;this.days=Collections.unmodifiableList(days);
            int measured=0,recorded=0;
            for(Day d:days){if(d.measured)measured++;if(d.observations>0)recorded++;}
            measuredDays=measured;recordedDays=recorded;
        }
    }
    static Model project(UsageSnapshot snapshot,List<LedgerRecord> records,long now) {
        UsageWindow window=snapshot==null?null:snapshot.weekly;
        LedgerPeriods.Span span=window==null?null:LedgerPeriods.span(
                window.effectiveResetAtMillis(snapshot.fetchedAtMillis),window.windowSeconds,snapshot.fetchedAtMillis);
        if(span==null)return new Model(0,0,0,Collections.emptyList());
        long until=Math.min(now,Math.min(snapshot.fetchedAtMillis,span.end-1));
        LocalDate first=LedgerAggregation.day(span.start),last=LedgerAggregation.day(span.end-1),today=LedgerAggregation.day(now);
        if(last.toEpochDay()-first.toEpochDay()>31)return new Model(span.start,span.end,until,Collections.emptyList());
        Map<LocalDate,Day> days=new LinkedHashMap<>();
        for(LocalDate d=first;!d.isAfter(last);d=d.plusDays(1))days.put(d,new Day(d,d.isAfter(today)));
        List<LedgerRecord> sorted=new ArrayList<>(records);sorted.sort(Comparator.comparingLong(r->r.at));
        String policy="weekly|"+LedgerRecord.cleanPlan(snapshot.planType)+"|"+window.windowSeconds;
        LedgerRecord before=null;
        for(LedgerRecord row:sorted){
            if(!row.meter.equals("weekly")||row.at<span.start||row.at>until)continue;
            boolean current=row.policy().equals(policy)&&row.source.startsWith("api_")
                    &&UsageWindow.sameResetWindow(row.reset,row.seconds,span.end,window.windowSeconds);
            Day day=days.get(LedgerAggregation.day(row.at));
            if(current&&day!=null)day.observations++;
            if(current&&before!=null&&before.source.startsWith("api_")&&before.policy().equals(policy)
                    &&UsageWindow.sameResetWindow(before.reset,before.seconds,span.end,window.windowSeconds)
                    &&LedgerAggregation.day(before.at).equals(LedgerAggregation.day(row.at))
                    &&LedgerAggregation.classify(before,row,Collections.emptyList())==LedgerAggregation.Change.NORMAL){
                day.points+=row.used-before.used;day.measured=true;
            }
            before=row; // A plan change or legacy observation breaks continuity.
        }
        return new Model(span.start,span.end,until,new ArrayList<>(days.values()));
    }
}
