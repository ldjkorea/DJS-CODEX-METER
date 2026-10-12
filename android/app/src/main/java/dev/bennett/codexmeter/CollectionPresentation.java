package dev.bennett.codexmeter;

import android.content.Context;
import java.util.List;
import org.json.JSONObject;

/** Activity-local presentation identities, never a journal/usage cache or an evaluation. */
final class CollectionPresentation {
    static String signature(Context c, String account) {
        String display = c.getSharedPreferences("codex_rpg_display_v1", 0).getString(account, "");
        // Missing display cache requires the durable journal read.
        if (display.isEmpty()) return "";
        return account + ":" + display + ":"
                + c.getSharedPreferences("codex_rpg_showcase_v1", 0).getAll().hashCode();
    }

    static String slots(Context c, JSONObject state, List<String> selected) {
        StringBuilder s = new StringBuilder(c.getResources().getConfiguration().toString())
                .append('|').append(TierTheme.tier(c));
        for (String id : selected) {
            s.append('|').append(id).append(':')
                    .append(RpgCollectionFacts.fact(state, RpgGrowthFacts.entry(id)));
        }
        return s.toString();
    }

    private CollectionPresentation() {}
}
