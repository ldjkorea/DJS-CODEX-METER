package dev.bennett.codexmeter;

/** Local presentation deadlines only; never refreshes usage or changes a captured observation. */
final class HomeInsightClock {
    static final long MAX_DELAY_MILLIS=60_000L;
    private HomeInsightClock(){}
    static long nextDelay(UsageSnapshot snapshot,long now){
        long delay=MAX_DELAY_MILLIS;
        if(snapshot==null)return delay;
        delay=sooner(delay,snapshot.nextResetMillis(now),now);
        // A future capture can become usable when the wall clock catches up.
        delay=sooner(delay,snapshot.fetchedAtMillis,now);
        if(snapshot.fetchedAtMillis>0&&snapshot.fetchedAtMillis<=Long.MAX_VALUE-900_001L)
            delay=sooner(delay,snapshot.fetchedAtMillis+900_001L,now);
        return delay;
    }
    private static long sooner(long delay,long deadline,long now){
        if(deadline<=now||deadline<=0)return delay;
        // Compare without subtraction overflow for an abnormal wall clock.
        if(now<=Long.MAX_VALUE-delay&&deadline>=now+delay)return delay;
        return Math.max(1L,deadline-now);
    }
}
