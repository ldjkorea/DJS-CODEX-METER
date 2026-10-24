package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground-only release notice. Opening the updater is an explicit user action. */
final class ForegroundUpdateNotice {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean PENDING = new AtomicBoolean();
    private static final ForegroundUpdatePolicy GATE = new ForegroundUpdatePolicy();
    // These weak UI references are used only on the main thread.
    private static WeakReference<AppCompatActivity> active = new WeakReference<>(null);
    private static WeakReference<AppCompatActivity> owner = new WeakReference<>(null);
    private static AlertDialog dialog;

    private ForegroundUpdateNotice() {}

    static void resume(AppCompatActivity activity) {
        active = new WeakReference<>(activity);
        schedule(activity.getApplicationContext());
    }

    static void metadataChanged(AppCompatActivity activity) {
        if (active.get() == activity) schedule(activity.getApplicationContext());
    }

    static void pause(AppCompatActivity activity) {
        if (active.get() == activity) active.clear();
        if (owner.get() == activity && dialog != null) dialog.dismiss();
    }

    private static void schedule(Context app) {
        if (!UpdatePreferences.automaticChecks(app) || !PENDING.compareAndSet(false, true)) return;
        WORKER.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                if (GATE.begin(UpdatePreferences.automaticChecks(app),
                        UpdatePreferences.lastCheckMillis(app), now, SystemClock.elapsedRealtime())) {
                    try {
                        ReleaseUpdateClient.checkIfStale(app);
                    } catch (Exception ignored) {
                        return; // Existing diagnostic/error storage is sufficient; no toast/dialog.
                    } finally {
                        GATE.finish();
                    }
                }
                if (!ForegroundUpdatePolicy.recent(UpdatePreferences.lastCheckMillis(app),
                        System.currentTimeMillis()) || !UpdatePreferences.lastError(app).isEmpty()) return;
                GitHubRelease release = UpdatePreferences.availableUpdate(app);
                String channel = UpdatePreferences.channel(app);
                String installed = UpdatePreferences.installedVersion(app);
                MAIN.post(() -> offer(app, release, channel, installed));
            } finally {
                PENDING.set(false);
            }
        });
    }

    private static void offer(Context app, GitHubRelease release, String channel, String installed) {
        AppCompatActivity activity = active.get();
        boolean foreground = activity != null && !activity.isFinishing() && !activity.isDestroyed();
        if (!ForegroundUpdatePolicy.mayPrompt(UpdatePreferences.automaticChecks(app),
                ForegroundUpdatePolicy.recent(UpdatePreferences.lastCheckMillis(app), System.currentTimeMillis()),
                !UpdatePreferences.lastError(app).isEmpty(), foreground,
                release != null && UpdatePreferences.foregroundVersionShown(app, release.version), release != null)) return;
        if (dialog != null || !channel.equals(UpdatePreferences.channel(app))
                || !installed.equals(UpdatePreferences.installedVersion(app))) return;
        if (AppPreferences.isOAuthPending(app) || AccountSession.signInActive()) return;
        // A manual/background check could replace the cache before this main-thread callback.
        GitHubRelease current = UpdatePreferences.availableUpdate(app);
        if (current == null || !current.version.equals(release.version)) return;
        AlertDialog notice = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.foreground_update_title, release.version))
                .setMessage(R.string.foreground_update_message)
                .setPositiveButton(R.string.foreground_update_open, (d, which) -> {
                    Intent intent = new Intent(activity, UpdateActivity.class)
                            .putExtra(UpdateActivity.EXTRA_VERSION, release.version);
                    activity.startActivity(intent);
                })
                .setNegativeButton(R.string.foreground_update_later, null)
                .create();
        notice.setOnDismissListener(d -> {
            if (dialog == notice) { dialog = null; owner.clear(); }
        });
        try {
            notice.show();
            dialog = notice;
            owner = new WeakReference<>(activity);
            // Mark after successful display; rotation, Back and dismissal all retain suppression.
            UpdatePreferences.markForegroundVersionShown(app, release.version);
        } catch (RuntimeException ignored) {
            // Activity/window disappeared; leave the version eligible for the next real resume.
            notice.dismiss();
        }
    }
}
