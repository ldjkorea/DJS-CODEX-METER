package dev.bennett.codexmeter;

/** Display evidence only. Never changes a quota, capture time or reset baseline. */
public final class UsageEvidence {
    public enum State { CONFIRMED, STALE, RESET_PASSED, CACHED_AFTER_FAILURE, UNAVAILABLE, NOT_PROVIDED }
    private UsageEvidence() { }
    public static State state(UsageSnapshot snapshot,UsageWindow window,boolean failed,long now,int refreshMinutes) {
        if(snapshot==null)return State.UNAVAILABLE;
        if(window==null)return State.NOT_PROVIDED;
        if(failed)return State.CACHED_AFTER_FAILURE;
        UsageInsights.Freshness freshness=UsageInsights.freshness(snapshot.fetchedAtMillis,now,refreshMinutes);
        if(freshness==UsageInsights.Freshness.UNKNOWN)return State.UNAVAILABLE;
        if(freshness!=UsageInsights.Freshness.FRESH)return State.STALE;
        long reset=window.effectiveResetAtMillis(snapshot.fetchedAtMillis);
        return reset>0&&reset<=now?State.RESET_PASSED:State.CONFIRMED;
    }
    public static boolean hasQuota(UsageSnapshot snapshot) {
        if(snapshot==null)return false;
        if(snapshot.weekly!=null||snapshot.monthly!=null||snapshot.fiveHour!=null)return true;
        for(UsageLimit limit:snapshot.additionalLimits)if(limit.primary!=null||limit.secondary!=null)return true;
        return false;
    }
    public static boolean fractional(UsageWindow window) {
        return window!=null&&window.preciseUsedPercent!=Math.rint(window.preciseUsedPercent);
    }
}
