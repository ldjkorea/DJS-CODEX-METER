package dev.bennett.codexmeter;

import org.json.*;

/** One-time, account-local collection restart. Lifetime awards and usage are never reset. */
public final class CollectionRestart {
    public static final int REVISION = 2;
    private CollectionRestart() {}
    public static boolean prepare(JSONObject d, long now) throws JSONException {
        JSONObject s = RpgEngine.state(d);
        if (s.optInt("collection_revision") >= REVISION) return false;
        AchievementJourney.prepare(d, now);
        TitleJourney.prepare(d, now);
        reset(d, false, now, false);
        reset(d, true, now, false);
        s.put("collection_revision", REVISION);
        return true;
    }
    public static void reset(JSONObject d, boolean titles, long now, boolean manual) throws JSONException {
        JSONObject s = RpgEngine.state(d);
        String key = titles ? TitleJourney.KEY : AchievementJourney.KEY;
        JSONObject previous = s.optJSONObject(key);
        JSONObject future = d.getJSONObject("future");
        JSONObject archive = future.optJSONObject("collection_archives");
        if (archive == null) { archive = new JSONObject(); future.put("collection_archives", archive); }
        if (previous != null) {
            String id = key + ":" + previous.optString("id", "initial") + ":" + now;
            archive.put(id, new JSONObject().put("journey", new JSONObject(previous.toString()))
                    .put("facts", new JSONObject((titles ? TitleJourney.view(s).getJSONObject("titles") : s.getJSONObject("achievements")).toString())));
        }
        JSONObject shadow = new JSONObject(d.toString());
        RpgEngine.state(shadow).remove(AchievementJourney.KEY);
        AchievementJourney.prepare(shadow, now);
        JSONObject fresh = AchievementJourney.journey(RpgEngine.state(shadow));
        fresh.put("collection_revision", REVISION).put("reset_at", manual ? now : 0);
        if (titles) {
            fresh.put("legacy", previous == null ? new JSONObject() : new JSONObject(previous.getJSONObject("legacy").toString()))
                    .put("facts", new JSONObject());
            s.remove("equipped_title"); s.remove("recommended_title"); s.put("title_selection_set", true);
        } else {
            if (previous != null) fresh.put("legacy", new JSONObject(previous.getJSONObject("legacy").toString()));
            s.put("achievements", RpgEngine.state(shadow).getJSONObject("achievements"));
            future.put("showcase", new JSONObject().put("ids", new JSONArray()).put("custom", false));
        }
        s.put(key, fresh);
    }
    /** A new revision cannot be repopulated by old backups or a superseded reset session. */
    public static void mergeScopes(JSONObject own, JSONObject incoming) throws JSONException {
        if (own.optInt("collection_revision") < REVISION) return;
        for (String key : new String[]{AchievementJourney.KEY, TitleJourney.KEY}) {
            JSONObject a = own.optJSONObject(key), b = incoming.optJSONObject(key);
            if (a == null || b == null || b.optInt("collection_revision") < REVISION) continue;
            long reset = a.optLong("reset_at"), otherReset = b.optLong("reset_at");
            // Empty new installs may restore this revision; an explicit reset blocks older backups.
            if (otherReset > reset || reset == 0 && a.optLong("first_started_at") == 0 && b.optLong("first_started_at") > 0) {
                own.put(key, new JSONObject(b.toString()));
                if (key.equals(AchievementJourney.KEY)) own.put("achievements", new JSONObject(incoming.getJSONObject("achievements").toString()));
                else { own.remove("equipped_title"); own.remove("recommended_title");String title=incoming.optString("equipped_title");if(TitleJourney.owned(own).contains(title))own.put("equipped_title",title);own.put("title_selection_set",true); }
            }
        }
    }
}
