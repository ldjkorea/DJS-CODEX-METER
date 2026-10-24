package dev.bennett.codexmeter;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Same fixed-seed probe compiles against both published and candidate selectors. */
public final class CoachBanterDistribution {
    public static void main(String[] args) throws Exception {
        CoachCatalog catalog = new CoachCatalog(new JSONObject(Files.readString(Path.of(args[0]))));
        JSONArray revised = new JSONObject(Files.readString(Path.of(args[1]))).getJSONArray("quotes");
        Set<String> curated = new HashSet<>();
        for (int i = 0; i < revised.length(); i++) if (revised.getJSONObject(i).optInt("selectionPriority") == 1) curated.add(revised.getJSONObject(i).getString("id"));
        JSONObject result = new JSONObject();
        for (String group : new String[]{"zero", "tiny", "low", "moderate", "mid", "high", "heavy", "burst", "reset", "imminent", "critical", "missing", "stale", "network"}) {
            JSONObject state = catalog.migrate(new JSONObject());
            Random random = new Random(20261024);
            int preferred = 0;
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 3000; i++) {
                CoachCatalog.Quote quote = catalog.select(state, group, "spicy", "", "draw:" + i, true, random);
                if (curated.contains(quote.id)) preferred++;
                seen.add(quote.id);
            }
            result.put(group, new JSONObject().put("draws", 3000).put("curated", preferred).put("distinct", seen.size()));
        }
        System.out.println(result.toString(2));
    }
}
