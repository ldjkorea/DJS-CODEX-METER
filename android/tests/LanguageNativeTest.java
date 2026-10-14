package dev.bennett.codexmeter;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the language picker in an isolated application package. */
public final class LanguageNativeTest extends Instrumentation {
    private int checks;
    private void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        start();
    }

    @Override public void onStart() {
        super.onStart();
        Bundle result = new Bundle();
        AppCompatActivity activity = null;
        Context context = getTargetContext();
        var prefs = context.getSharedPreferences("gpt_hud_language", 0);
        var settings = context.getSharedPreferences("codex_meter_settings_v1", 0);
        boolean oldChosen = prefs.getBoolean("chosen", false);
        boolean hadComplete = settings.contains("onboarding_complete");
        boolean oldComplete = settings.getBoolean("onboarding_complete", false);
        LocaleListCompat oldLocales = AppCompatDelegate.getApplicationLocales();
        try {
            check("dev.bennett.codexmeter.rpg3qa".equals(context.getPackageName()), "Isolated QA package only");
            prefs.edit().putBoolean("chosen", true).commit();
            settings.edit().putBoolean("onboarding_complete", false).commit();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
            activity = (AppCompatActivity) startActivitySync(
                    new Intent(context, OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            check(activity != null, "Onboarding screen opens");
            check("English".equals(AppLanguage.currentName(activity)), "English default label");

            prefs.edit().putBoolean("chosen", false).commit();
            settings.edit().putBoolean("onboarding_complete", false).commit();
            check(AppLanguage.needsFirstChoice(activity), "New install gets a first language choice (complete="
                    + AppPreferences.isOnboardingComplete(activity) + ", chosen=" + prefs.getBoolean("chosen", false)
                    + ", locales=" + AppCompatDelegate.getApplicationLocales().toLanguageTags() + ")");
            prefs.edit().putBoolean("chosen", true).commit();
            check(!AppLanguage.needsFirstChoice(activity), "Returning install does not see first-run prompt again");
            prefs.edit().putBoolean("chosen", false).commit();

            AtomicReference<AlertDialog> holder = new AtomicReference<>();
            AppCompatActivity active = activity;
            runOnMainSync(() -> holder.set(AppLanguage.show(active, true)));
            AlertDialog dialog = holder.get();
            check(dialog.getListView().getCount() == 2, "Picker offers exactly Korean and English");
            check(dialog.getListView().getChoiceMode() == android.widget.ListView.CHOICE_MODE_SINGLE,
                    "Language choice is single-select");
            runOnMainSync(() -> {
                var list = dialog.getListView();
                list.getOnItemClickListener().onItemClick(list, list.getChildAt(1), 1,
                        list.getAdapter().getItemId(1));
            });
            check(prefs.getBoolean("chosen", false), "Choice is saved for this installation");
            check("en".equals(AppCompatDelegate.getApplicationLocales().toLanguageTags()), "English is applied");
            check("English".equals(AppLanguage.currentName(context)), "Chosen language is reflected in app context");
        } catch (Throwable e) {
            result.putString("stream", android.util.Log.getStackTraceString(e));
            finish(0, result);
            return;
        } finally {
            if (activity != null) activity.finish();
            AppCompatDelegate.setApplicationLocales(oldLocales);
            prefs.edit().putBoolean("chosen", oldChosen).commit();
            if (hadComplete) settings.edit().putBoolean("onboarding_complete", oldComplete).commit();
            else settings.edit().remove("onboarding_complete").commit();
        }
        result.putString("stream", "App language picker: " + checks + " checks passed; isolated emulator only\n");
        finish(-1, result);
    }
}
