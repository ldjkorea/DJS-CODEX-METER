package dev.bennett.codexmeter;

import android.content.res.ColorStateList;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.List;

/** Primary home content in every preset. Reads the cached observation only. */
final class BetaQuotaUi {
    private BetaQuotaUi() {}

    static void add(MainActivity activity, LinearLayout parent, UsageSnapshot snapshot,
            boolean dark, List<Runnable> clockUpdates) {
        LinearLayout card = BetaUi.group(parent,
                BetaExperienceUi.t(activity, "남은 사용 한도", "Remaining limits"), dark);
        card.setTag("beta998:quota");
        if (TrialDemo.active(activity)) card.addView(BetaUi.note(activity,
                BetaExperienceUi.t(activity, "가상 데모 · 실제 계정 한도 아님",
                        "Synthetic demo · not real account limits"), dark));
        boolean stacked = activity.getResources().getConfiguration().fontScale > 1.3f
                || activity.getResources().getConfiguration().screenWidthDp < 360;
        LinearLayout meters = new LinearLayout(activity);
        meters.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        card.addView(meters, new LinearLayout.LayoutParams(-1, -2));
        addWindow(activity, meters, snapshot, snapshot == null ? null : snapshot.fiveHour,
                true, dark, stacked, clockUpdates);
        addWindow(activity, meters, snapshot, snapshot == null ? null : snapshot.weekly,
                false, dark, stacked, clockUpdates);
        if (snapshot != null) {
            Ui.addSpacer(card, 8);
            card.addView(BetaUi.note(activity, activity.getString(R.string.next_observed_at,
                    V3Display.time(snapshot.fetchedAtMillis)), dark));
        }
        Ui.addSpacer(parent, 16);
    }

    private static void addWindow(MainActivity activity, LinearLayout parent,
            UsageSnapshot snapshot, UsageWindow window, boolean fiveHour, boolean dark,
            boolean stacked, List<Runnable> clockUpdates) {
        String key = fiveHour ? "five-hour" : "weekly";
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(stacked ? -1 : 0, -2,
                stacked ? 0 : 1);
        if (!fiveHour) {
            if (stacked) params.topMargin = Ui.dp(activity, 16);
            else params.setMarginStart(Ui.dp(activity, 16));
        }
        parent.addView(column, params);
        String label = BetaExperienceUi.t(activity,
                fiveHour ? "5시간 남음" : "주간 남음",
                fiveHour ? "5-hour remaining" : "Weekly remaining");
        column.addView(BetaUi.body(activity, label, dark));
        TextView value = BetaUi.text(activity, window == null ? "—"
                : UsagePrecision.remaining(window, AppPreferences.usageDecimalPlaces(activity)),
                30, Ui.accent(activity, dark), true);
        value.setTag("beta998:quota:" + key + ":value");
        value.setContentDescription(label + ": " + value.getText());
        column.addView(value);
        ProgressBar progress = Ui.progress(activity, dark);
        progress.setMax(100000);
        progress.setProgress(window == null ? 0 : (int) Math.round((100 - window.preciseUsedPercent) * 1000));
        progress.setProgressTintList(ColorStateList.valueOf(Ui.accent(activity, dark)));
        progress.setVisibility(window == null ? android.view.View.GONE : android.view.View.VISIBLE);
        progress.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        column.addView(progress, new LinearLayout.LayoutParams(-1, Ui.dp(activity, 6)));
        TextView status = BetaUi.note(activity, "", dark);
        status.setTag("beta998:quota:" + key + ":status");
        column.addView(status);
        TextView reset = BetaUi.note(activity, "", dark);
        reset.setTag("beta998:quota:" + key + ":reset");
        column.addView(reset);
        Runnable update = () -> {
            if (window == null) {
                status.setText(BetaExperienceUi.t(activity,
                        snapshot == null ? "사용량 확인 필요" : "제공된 한도 없음",
                        snapshot == null ? "Check usage first" : "Limit not provided"));
                reset.setText("");
                return;
            }
            long now = System.currentTimeMillis();
            UsageEvidence.State state = UsageEvidence.state(snapshot, window,
                    !AppPreferences.getLastError(activity).isEmpty(), now,
                    RefreshScheduler.effectiveRefreshMinutes(activity));
            int resource = state == UsageEvidence.State.CACHED_AFTER_FAILURE ? R.string.phase_a_cached_failed
                    : state == UsageEvidence.State.RESET_PASSED ? R.string.phase_a_reset_passed
                    : state == UsageEvidence.State.STALE ? R.string.phase_a_stale
                    : state == UsageEvidence.State.UNAVAILABLE ? R.string.phase_a_unavailable
                    : R.string.phase_a_confirmed;
            status.setText(resource);
            long resetAt = window.effectiveResetAtMillis(snapshot.fetchedAtMillis);
            reset.setText(fiveHour ? FiveHourResetDisplay.text(activity, window,
                    snapshot.fetchedAtMillis, now, WidgetOptions.RESET_RELATIVE, false)
                    : resetAt <= 0 ? activity.getString(R.string.ui_reset_time_unavailable_5c3030)
                    : resetAt <= now ? activity.getString(R.string.phase_a_reset_passed)
                    : UsageFormat.reset(activity, window, WidgetOptions.RESET_RELATIVE,
                            snapshot.fetchedAtMillis, now));
        };
        clockUpdates.add(update);
        update.run();
    }
}
