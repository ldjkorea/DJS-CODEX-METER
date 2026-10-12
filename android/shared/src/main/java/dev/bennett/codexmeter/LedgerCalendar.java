package dev.bennett.codexmeter;

/** Read-only calendar styling from measured daily deltas, never cumulative quota or gaps. */
public final class LedgerCalendar {
    private LedgerCalendar() { }

    public static boolean emerald(LedgerAggregation.Day day) {
        return LedgerPresentation.measured(day) && day.policy.startsWith("weekly|")
                // Ignore sub-nanopoint floating subtraction noise at the exact boundary.
                && Double.isFinite(day.points) && day.points > 13d + 1e-9;
    }

    public static int band(LedgerAggregation.Day day) {
        if(!LedgerPresentation.measured(day)||!Double.isFinite(day.points)||day.points<0)return -1;
        return day.points>=25?4:day.points>=20?3:day.points>13+1e-9?2:day.points>=5?1:0;
    }
    public static int color(LedgerAggregation.Day day) {
        int band=band(day);return band<0?0:new int[]{0xffef8f94,0xffe6ad76,0xff69d6ae,0xffc8d5df,0xffe9cf8f}[band];
    }
    public static int intensity(LedgerAggregation.Day day) {
        if (!LedgerPresentation.measured(day) || !Double.isFinite(day.points) || day.points < 0) return 0;
        return (int) (35 + 90 * Math.min(1, day.points / 20d));
    }
}
