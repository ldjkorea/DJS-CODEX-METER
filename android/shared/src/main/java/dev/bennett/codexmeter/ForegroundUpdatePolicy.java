package dev.bennett.codexmeter;

/** Small foreground discovery gate. No timers, network, Android or account state. */
public final class ForegroundUpdatePolicy {
    public static final long CACHE_MILLIS = 5 * 60 * 1000L;
    private boolean checking;
    private long lastAttemptElapsed = -1;

    public static boolean recent(long checkedAt, long now) {
        return checkedAt > 0 && now >= checkedAt && now - checkedAt < CACHE_MILLIS;
    }

    public synchronized boolean begin(boolean enabled, long checkedAt, long now, long elapsed) {
        if (!enabled || checking || recent(checkedAt, now)) return false;
        if (lastAttemptElapsed >= 0 && elapsed >= lastAttemptElapsed
                && elapsed - lastAttemptElapsed < CACHE_MILLIS) return false;
        checking = true;
        lastAttemptElapsed = elapsed;
        return true;
    }

    public synchronized void finish() {
        checking = false;
    }

    public static boolean mayPrompt(boolean enabled, boolean fresh, boolean failed,
            boolean foreground, boolean alreadyShown, boolean available) {
        return enabled && fresh && !failed && foreground && !alreadyShown && available;
    }
}
