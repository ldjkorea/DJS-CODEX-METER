package dev.bennett.codexmeter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Synthetic timing/concurrency/eligibility. Does not certify Android dialogs or persistence. */
public final class ForegroundUpdatePolicySelfTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        long now = 1800000000000L, ttl = ForegroundUpdatePolicy.CACHE_MILLIS;
        ForegroundUpdatePolicy gate = new ForegroundUpdatePolicy();
        check(ttl == 300000, "Short cache is five minutes");
        check(!gate.begin(false, 0, now, 1000), "OFF prevents request");
        check(gate.begin(true, 0, now, 1000), "Initial foreground checks");
        check(!gate.begin(true, 0, now + ttl, 1000 + ttl), "In-flight request deduplicated");
        gate.finish();
        check(!gate.begin(true, 0, now + 1, 1001), "Failed request also has process cooldown");
        check(!gate.begin(true, 0, now - ttl, 1002), "Wall clock rollback cannot burst requests");
        check(!gate.begin(true, 0, now + ttl, 1000 + ttl - 1), "Just before retry boundary");
        check(gate.begin(true, 0, now + ttl, 1000 + ttl), "Exact retry boundary");
        gate.finish();
        for (long age : new long[]{0, 1, ttl - 1}) {
            check(ForegroundUpdatePolicy.recent(now - age, now), "Fresh cache " + age);
            check(!new ForegroundUpdatePolicy().begin(true, now - age, now, 1), "Stored cache survives process gate reset");
        }
        check(!ForegroundUpdatePolicy.recent(now - ttl, now), "Exact cache expiry");
        check(!ForegroundUpdatePolicy.recent(now + 1, now), "Future cache not fresh");
        check(!ForegroundUpdatePolicy.recent(0, now), "No cache");
        check(new ForegroundUpdatePolicy().begin(true, now - ttl, now, 1), "Persisted last error eventually retried");
        check(new ForegroundUpdatePolicy().begin(true, now + 1, now, 1), "Clock rollback eventually recoverable");
        for (int mask = 0; mask < 64; mask++) {
            boolean enabled = (mask & 1) != 0, fresh = (mask & 2) != 0, failed = (mask & 4) != 0;
            boolean foreground = (mask & 8) != 0, shown = (mask & 16) != 0, available = (mask & 32) != 0;
            boolean expected = enabled && fresh && !failed && foreground && !shown && available;
            check(ForegroundUpdatePolicy.mayPrompt(enabled, fresh, failed, foreground, shown, available) == expected, "All prompt gates " + mask);
        }
        // The stored version is an independent durable input, not a per-Activity dismissal flag.
        Set<String> stored = new HashSet<>();
        check(ForegroundUpdatePolicy.mayPrompt(true, true, false, true, stored.contains("3.1.5"), true), "New version eligible");
        stored.add("3.1.5");
        check(!ForegroundUpdatePolicy.mayPrompt(true, true, false, true, stored.contains("3.1.5"), true), "Dismissed version suppressed");
        stored = new HashSet<>(stored);
        check(!ForegroundUpdatePolicy.mayPrompt(true, true, false, true, stored.contains("3.1.5"), true), "Recreated state remains suppressed");
        check(ForegroundUpdatePolicy.mayPrompt(true, true, false, true, stored.contains("3.1.6"), true), "Later version eligible");
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            for (int round = 0; round < 30; round++) {
                ForegroundUpdatePolicy concurrent = new ForegroundUpdatePolicy();
                AtomicInteger started = new AtomicInteger();
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> jobs = new ArrayList<>();
                for (int i = 0; i < 64; i++) jobs.add(pool.submit(() -> {
                    try { start.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                    if (concurrent.begin(true, 0, now, 1000)) started.incrementAndGet();
                }));
                start.countDown();
                for (Future<?> job : jobs) job.get();
                check(started.get() == 1, "Exactly one concurrent automatic request " + round);
                concurrent.finish();
                check(!concurrent.begin(true, 0, now + 1, 1001), "Completion keeps cooldown " + round);
            }
        } finally { pool.shutdownNow(); }
        System.out.println("Foreground update policy: " + checks + " assertions passed; synthetic JVM only.");
    }
}
