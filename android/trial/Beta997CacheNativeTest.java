package dev.bennett.codexmeter;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import org.json.JSONObject;

/** Runs only against the owned synthetic beta fixture. Never clears app data. */
public final class Beta997CacheNativeTest extends Instrumentation {
    int checks;
    void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        SharedPreferences display = null;
        String account = null, savedCache = null;
        int resultCode=0;
        try {
            Context c = getTargetContext();
            check(Build.MODEL.contains("sdk") && c.getPackageName().equals("dev.bennett.codexmeter.trial"), "Owned beta emulator only");
            check(TrialDemo.active(c), "Existing synthetic demo only");
            account = SubscriptionStore.accountKey(c);
            JSONObject before = BetaGrowthStore.load(c, account);
            check(before != null, "Existing synthetic growth required");
            c.getSharedPreferences("beta997_qa", 0).edit()
                .putString("state", before.toString())
                .putString("archive", BetaGrowthStore.archive(c, account).toString())
                .putString("name", BetaIdentity.name(c)).commit();
            display = c.getSharedPreferences("beta_growth_display_v4", 0);
            savedCache = display.getString(account, null);
            check(display.edit().remove(account).commit(), "Remove synthetic derived cache only");
            check(BetaGrowthStore.peek(c) == null, "Missing cache reproduced");
            JSONObject recovered = BetaGrowthStore.load(c, account);
            check(before.toString().equals(recovered.toString()), "Original DB remains identical");
            check(BetaGrowthStore.peek(c) != null, "CACHE_RECOVERY_REQUIRED: load must restore missing display cache");
            check(BetaGrowthStore.peek(c).getLong("total") == before.getLong("total"), "Recovered XP matches DB");
            result.putString("stream", "Beta997 cache probe: " + checks + " checks passed.\n");
            resultCode=-1;
        } catch (Throwable error) {
            result.putString("stream", android.util.Log.getStackTraceString(error));

        } finally {
            if (display != null && account != null) {
                SharedPreferences.Editor editor = display.edit();
                if (savedCache == null) editor.remove(account); else editor.putString(account, savedCache);
                editor.commit();
            }
        }
        finish(resultCode,result);
    }
}
