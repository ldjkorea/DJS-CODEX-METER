package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Presentation only: at most three decimals, with no invented trailing precision. */
public final class UsagePrecision {
    private UsagePrecision() {}
    public static String number(double value) {
        if (!Double.isFinite(value)) return "—";
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
        DecimalFormat format = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(Locale.getDefault()));
        format.setRoundingMode(RoundingMode.HALF_UP);
        format.setGroupingUsed(false);
        return format.format(rounded);
    }
    public static String used(UsageWindow window) { return number(window.preciseUsedPercent) + "%"; }
    public static String remaining(UsageWindow window) { return number(100.0 - window.preciseUsedPercent) + "%"; }
    /** Selected display precision only; raw observations and scoring are unchanged. */
    public static String number(double value, int digits) {
        if (!Double.isFinite(value)) return "—";
        return number(BigDecimal.valueOf(value),digits);
    }
    private static String number(BigDecimal value,int digits) {
        digits = digits == 2 ? 2 : 3;
        DecimalFormat format = new DecimalFormat(digits == 2 ? "0.00" : "0.000",
                DecimalFormatSymbols.getInstance(Locale.getDefault()));
        format.setRoundingMode(RoundingMode.HALF_UP);
        format.setGroupingUsed(false);
        return format.format(value.setScale(digits, RoundingMode.HALF_UP));
    }
    public static String percent(UsageWindow window,boolean used,int digits,boolean symbol) {
        if(window==null)return "—";
        BigDecimal value=BigDecimal.valueOf(window.preciseUsedPercent);
        if(!used)value=BigDecimal.valueOf(100).subtract(value);
        return number(value,digits)+(symbol?"%":"");
    }
    public static String used(UsageWindow window, int digits) { return percent(window,true,digits,true); }
    public static String remaining(UsageWindow window, int digits) { return percent(window,false,digits,true); }
}
