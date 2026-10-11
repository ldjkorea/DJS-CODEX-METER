package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/** Billing-period lower bounds from actual counters. Resets never erase earlier evidence. */
public final class MonthlyLedger {
    public static final int VERSION=1;
    public static final class Bucket {
        public final String policy;
        public final BigDecimal allocation;
        public BigDecimal points=BigDecimal.ZERO;
        public boolean partial=true;
        public final List<String> unknown=new ArrayList<>();
        public final List<long[]> covered=new ArrayList<>();
        Bucket(String policy,long start,long end,long seconds){this.policy=policy;
            allocation=BigDecimal.valueOf(end-start).divide(BigDecimal.valueOf(seconds*1000L),MathContext.DECIMAL128).multiply(BigDecimal.valueOf(100));}
        public BigDecimal percent(){return allocation.signum()==0?BigDecimal.ZERO:points.multiply(BigDecimal.valueOf(100)).divide(allocation,MathContext.DECIMAL128);}
    }
    public static Map<String,Bucket> calculate(List<LedgerRecord> input,List<LedgerAggregation.Day> days,
            List<Long> credits,long start,long end,long floor,long now){
        if(start<0||end<=start)throw new IllegalArgumentException("Invalid billing period");
        Map<String,Bucket> result=new LinkedHashMap<>();
        List<LedgerRecord> rows=new ArrayList<>(input);rows.sort(Comparator.comparing((LedgerRecord r)->r.meter).thenComparingLong(r->r.at));
        Map<String,LedgerRecord> latest=new HashMap<>();
        for(LedgerRecord row:rows){
            if(!(row.meter.equals("weekly")||row.meter.equals("monthly"))||row.plan.equals("unknown")||row.at<=floor||row.at>now)continue;
            LedgerRecord before=latest.get(row.meter);if(before!=null&&row.at<=before.at)continue;latest.put(row.meter,row);
            if(row.at<start||row.at>=end)continue;
            Bucket bucket=result.computeIfAbsent(row.policy(),k->new Bucket(k,start,end,row.seconds));
            LedgerAggregation.Change change=LedgerAggregation.classify(before,row,credits);
            // A confirmed credit near a scheduled rollover cannot turn the new period into old usage.
            if(before!=null&&row.policy().equals(before.policy())&&row.at>=before.reset&&row.reset>before.reset)
                change=LedgerAggregation.Change.RESET;
            long from=before==null?Math.max(0,row.reset-row.seconds*1000L):before.at;
            BigDecimal amount=null;
            if(before==null||change==LedgerAggregation.Change.RESET){
                long windowStart=row.reset-row.seconds*1000L;
                if(row.reset>row.at&&windowStart>=Math.max(start,floor)&&windowStart<=row.at){amount=new BigDecimal(row.decimal);from=windowStart;}
                else bucket.unknown.add("billing_boundary_or_missing_baseline:"+from+":"+row.at);
            }else if(change==LedgerAggregation.Change.NORMAL||change==LedgerAggregation.Change.GAP){
                if(before.at>=Math.max(start,floor))amount=new BigDecimal(row.decimal).subtract(new BigDecimal(before.decimal));
                else bucket.unknown.add("billing_boundary:"+before.at+":"+row.at);
                if(change==LedgerAggregation.Change.GAP)bucket.unknown.add("measurement_gap:"+before.at+":"+row.at);
            }else {
                // Credit confirmation describes the operation, not a measured post-reset spend.
                // A counter drop/new schedule provides a new baseline; only later increases count.
                bucket.unknown.add(change.name()+":"+from+":"+row.at);
            }
            if(amount!=null&&amount.signum()>=0){bucket.points=bucket.points.add(amount);bucket.covered.add(new long[]{from,row.at});}
        }
        for(LedgerAggregation.Day day:days){
            String[] policy=day.policy.split("\\|");if(policy.length!=3||!(policy[0].equals("weekly")||policy[0].equals("monthly"))||policy[1].equals("unknown"))continue;
            long from=java.time.LocalDate.parse(day.date).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),to=from+86400000L;
            if(from<Math.max(start,floor)||to>end||day.last>now||day.points<0||day.legacy)continue;
            // Daily sealed totals are a fallback, never added on top of raw-supported intervals.
            boolean overlap=false;for(LedgerRecord row:rows)if(row.policy().equals(day.policy)&&row.at>=from&&row.at<to)overlap=true;
            for(Bucket b:result.values())if(b.policy.equals(day.policy))for(long[] range:b.covered)if(range[0]<to&&range[1]>=from)overlap=true;
            if(overlap)continue;
            Bucket bucket=result.computeIfAbsent(day.policy,k->new Bucket(k,start,end,Long.parseLong(policy[2])));
            bucket.points=bucket.points.add(BigDecimal.valueOf(day.points));bucket.covered.add(new long[]{from,to});
            bucket.unknown.add("sealed_daily_lower_bound:"+from+":"+to);
        }
        return result;
    }
}
