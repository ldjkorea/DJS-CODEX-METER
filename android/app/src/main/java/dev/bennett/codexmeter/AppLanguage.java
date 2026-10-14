package dev.bennett.codexmeter;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/** Device-local language choice, independent of account and collection settings. */
final class AppLanguage {
    private static final String PREFS = "gpt_hud_language";
    private static final String[] TAGS = {"ko", "en"};
    private static final String[] NAMES = {"한국어", "English"};

    private AppLanguage() { }

    static boolean needsFirstChoice(Context c) {
        return !AppPreferences.isOnboardingComplete(c)
                && !c.getSharedPreferences(PREFS, 0).getBoolean("chosen", false);
    }

    static String currentName(Context c) {
        return NAMES[currentIndex(c)];
    }

    private static int currentIndex(Context c) {
        return "ko".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage()) ? 0 : 1;
    }

    static AlertDialog show(AppCompatActivity activity, boolean firstRun) {
        int[] selected = {currentIndex(activity)};
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(firstRun ? R.string.language_first_title : R.string.app_language)
                .setSingleChoiceItems(NAMES, selected[0], (dialog, index) -> {
                    activity.getSharedPreferences(PREFS, 0).edit().putBoolean("chosen", true).apply();
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(TAGS[index]));
                    dialog.dismiss();
                });
        if (!firstRun) builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(!firstRun);
        dialog.setCanceledOnTouchOutside(!firstRun);
        dialog.show();
        return dialog;
    }
}
