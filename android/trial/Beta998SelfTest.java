package dev.bennett.codexmeter;
import java.nio.file.*;import java.time.*;import org.json.*;
public final class Beta998SelfTest {
    static int checks;static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        long now=ZonedDateTime.of(2028,3,1,12,0,0,0,BetaV4.ZONE).toInstant().toEpochMilli();
        JSONObject state=BetaV4.create("a".repeat(64),now-35*86400000L);
        for(int i=29;i>=0;i--){long at=now-i*86400000L;BetaV4.confirm(state,"a".repeat(64),at,at,true);}
        String original=state.toString();JSONObject week=BetaExperience.recap(state,now,false),month=BetaExperience.recap(state,now,true);
        check(week.getInt("days")==3,"Monday-based week across leap February");check(month.getInt("days")==1,"Calendar month boundary");check(month.getLong("xp")==21,"New month includes the weekly reward paid on March1");
        long expected=0;JSONObject rewards=state.getJSONObject("rewards");java.util.Iterator<String> keys=rewards.keys();long boundary=LocalDate.of(2028,2,28).atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli();while(keys.hasNext()){JSONObject reward=rewards.getJSONObject(keys.next());if(reward.getLong("at")>=boundary)expected+=reward.getLong("xp");}check(week.getLong("xp")==expected,"Recap sums existing rewards, not recalculated XP");
        String unearned="";for(String[] e:BetaCatalog.ALL)if(!BetaCatalog.title(e[0])&&!state.getJSONObject("honors").has(e[0])){unearned=e[0];break;}
        check(!unearned.isEmpty(),"Unowned goal fixture");check(BetaExperience.goal(state,now,unearned).getString("id").equals(unearned),"Valid pinned goal");check(BetaExperience.goal(state,now,unearned).getBoolean("pinned"),"Pinned status");
        for(String id:new String[]{"missing-id","rpg3.starter","rpg3.first_day",""})check(!BetaExperience.goal(state,now,id).optBoolean("pinned"),"Invalid/title/owned pin falls back without mutation");
        check(BetaExperience.timeline(state).size()==state.getJSONObject("honors").length()+state.getJSONObject("titles").length(),"Timeline includes both recorded award kinds");check(state.toString().equals(original),"Every view is read-only");
        JSONObject raw=new JSONObject(Files.readString(Path.of(args[0])));CoachCatalog catalog=new CoachCatalog(raw);check(catalog.quotes.size()==600&&catalog.tips.size()==40,"Stable quote/tip counts");check(raw.getInt("contentVersion")==4,"Content revision");
        int revised=0;for(int i=0;i<raw.getJSONArray("quotes").length();i++){JSONObject q=raw.getJSONArray("quotes").getJSONObject(i);String text=q.getString("text");check(!text.toUpperCase(java.util.Locale.ROOT).contains("DICK"),"No excluded meme");if(q.optString("review998").equals("rewritten")){revised++;check(text.length()>=12&&text.length()<140,"Readable revised copy length");}}
        check(revised==150,"150 reviewed replacements; stable IDs preserved separately");
        System.out.println("Beta998: "+checks+" checks passed; leap boundaries, pinned fallback, read-only recap, built-in copy");
    }
}
