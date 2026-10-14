package dev.bennett.codexmeter;

import android.content.Context;

/** One-time upgrade from the older beta which forcibly disabled checks in demo mode. */
final class BetaUpdatePolicy {
    static void initialize(Context c) {
        android.content.SharedPreferences p=c.getSharedPreferences("trial_update_policy",0);
        if(!p.getBoolean("enabled_v1",false)) {
            UpdatePreferences.setAutomaticChecks(c,true);
            UpdatePreferences.setNotifyUpdatesEnabled(c,true);
            p.edit().putBoolean("enabled_v1",true).apply();
        }
        ReleaseUpdateScheduler.ensureScheduled(c);
    }
}
