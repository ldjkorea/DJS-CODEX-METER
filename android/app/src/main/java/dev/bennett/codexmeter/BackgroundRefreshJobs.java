package dev.bennett.codexmeter;

import android.content.Context;
import android.content.ComponentName;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;

/** Keep the original due time for an unchanged policy; leave retries/reset/manual jobs alone. */
final class BackgroundRefreshJobs {
    static boolean keepExisting(Context c, JobScheduler jobs, int periodic, int shortA, int shortB) {
        boolean adaptive = AppPreferences.getAutomaticRefresh(c);
        int minutes = RefreshScheduler.effectiveRefreshMinutes(c);
        boolean chained = adaptive || minutes < 15;
        String reason = adaptive ? RefreshScheduler.REASON_ADAPTIVE : RefreshScheduler.REASON_SHORT_PERIODIC;
        ComponentName service = new ComponentName(c, UsageRefreshJobService.class);
        int keep = -1;
        int[] policyIds = {periodic, shortA, shortB};
        for (int id : policyIds) {
            JobInfo job = jobs.getPendingJob(id);
            if (job == null) continue;
            boolean matching = chained
                    ? id != periodic && !job.isPeriodic()
                        && job.getMinLatencyMillis() == minutes * 60000L
                        && reason.equals(job.getExtras().getString("reason", ""))
                    : id == periodic && job.isPeriodic()
                        && job.getIntervalMillis() == minutes * 60000L;
            if (matching && service.equals(job.getService()) && keep < 0) keep = id;
        }
        if (keep < 0) return false;
        for (int id : policyIds) {
            if (id != keep && jobs.getPendingJob(id) != null) jobs.cancel(id);
        }
        AppPreferences.setSchedulerError(c, "");
        return true;
    }

    private BackgroundRefreshJobs() {}
}
