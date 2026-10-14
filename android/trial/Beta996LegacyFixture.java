package dev.bennett.codexmeter;
import org.json.*;import java.time.*;import java.nio.file.*;
/** Compile against the unchanged 9.9.5 engine, not the new expanded catalog. */
public final class Beta996LegacyFixture {
 public static void main(String[] args)throws Exception{String account="a".repeat(64);LocalDate start=LocalDate.of(2027,1,1);long cutover=start.atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli();JSONObject state=BetaV4.create(account,cutover);for(int i=0;i<400;i++){long at=start.plusDays(i).atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli()+3600000;BetaV4.confirm(state,account,at,at,true);if(i==29){state.put("equipped","rpg3.workbench");Files.writeString(Path.of(args[0]),state.toString());}}Files.writeString(Path.of(args[1]),state.toString());}
}
