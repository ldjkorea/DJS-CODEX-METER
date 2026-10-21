package dev.bennett.codexmeter;
import java.util.*;import java.nio.file.*;import org.json.*;
/** New beta discovery/code monotonicity; original release and trust tests remain intact. */
public final class Beta9913ReleaseSelfTest {
 static int n;static void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
 static JSONObject release(String tag,String asset){String root=GitHubReleaseSource.REPOSITORY_URL;return new JSONObject().put("tag_name",tag).put("prerelease",tag.startsWith("beta-")).put("html_url",root+"/releases/tag/"+tag).put("assets",new JSONArray().put(new JSONObject().put("name",asset).put("size",25000000).put("browser_download_url",root+"/releases/download/"+tag+"/"+asset)).put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",root+"/releases/download/"+tag+"/SHA256SUMS.txt")));}
 public static void main(String[] args)throws Exception{
  JSONArray fixture=new JSONArray().put(release("v3.1.2","CodexMeter-3.1.2-ko.apk")).put(release("beta-v9.9.12","GPT-HUD-BETA-9.9.12.apk")).put(release("beta-v9.9.13","GPT-HUD-BETA-9.9.13.apk"));
  List<GitHubRelease> rows=GitHubReleaseParser.parse(fixture.toString());check(rows.size()==2,"Formal release excluded from beta");
  for(int i=0;i<=12;i++)for(String channel:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(rows,"9.9."+i,channel).version.equals("9.9.13"),"Previous beta advances");
  check(UpdateChannel.selectUpdate(rows,"9.9.13","stable")==null,"Current beta not reoffered");
  check(UpdateChannel.selectUpdate(rows,"9.9.14","stable")==null,"Future beta not downgraded");
  check(KoreanUpdateTrust.compatible("dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,990012,990013),"Existing package/certificate advances");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,990012,990013),"Formal package rejected");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter.trial","wrong",990012,990013),"Other signer rejected");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,990013,990013),"Same code rejected");
  if(args.length>0){rows=GitHubReleaseParser.parse(Files.readString(Path.of(args[0])));for(String channel:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(rows,"9.9.12",channel).version.equals("9.9.13"),"Public installed beta update");check(UpdateChannel.selectUpdate(rows,"9.9.13","stable")==null,"Public current beta not reoffered");}
  System.out.println("Beta9913 updater: "+n+" checks passed; channel split/original signer/monotonic code"+(args.length>0?"/public feed":"/synthetic feed")+".");
 }
}
