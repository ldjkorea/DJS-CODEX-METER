package dev.bennett.codexmeter;
import java.util.*;import java.nio.file.*;import org.json.*;
/** Existing stable discovery and signing contract; synthetic/public feed checks remain distinct. */
public final class Stable313ReleaseSelfTest {
 static int n;static void check(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 static JSONObject row(String tag,String version,String name){String r=GitHubReleaseSource.REPOSITORY_URL;return new JSONObject().put("tag_name",tag).put("prerelease",false).put("html_url",r+"/releases/tag/"+tag).put("assets",new JSONArray().put(new JSONObject().put("name",name).put("size",25000000).put("browser_download_url",r+"/releases/download/"+tag+"/"+name)).put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",r+"/releases/download/"+tag+"/SHA256SUMS.txt")));}
 public static void main(String[] args)throws Exception{
  List<GitHubRelease> rows=GitHubReleaseParser.parse(new JSONArray().put(row("v3.1.2","3.1.2","CodexMeter-3.1.2-ko.apk")).put(row("v3.1.3","3.1.3","CodexMeter-3.1.3-ko.apk")).put(row("beta-v9.9.14","9.9.14","GPT-HUD-BETA-9.9.14.apk")).toString());
  check(rows.size()==2,"Separate beta package never selected");
  for(String old:new String[]{"2.9.2","3.0.0","3.1.0","3.1.1","3.1.2"})for(String channel:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(rows,old,channel).version.equals("3.1.3"),"Existing stable client advances");
  check(UpdateChannel.selectUpdate(rows,"3.1.3","stable")==null,"Current version not reoffered");check(UpdateChannel.selectUpdate(rows,"3.1.4","alpha")==null,"No downgrade");
  check(KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,89,90),"Existing package signer and increased code");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,89,90),"Separate package rejected");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter","wrong",89,90),"Wrong certificate rejected");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,90,90),"Same code rejected");
  if(args.length>0){rows=GitHubReleaseParser.parse(Files.readString(Path.of(args[0])));for(String channel:new String[]{"stable","alpha"}){GitHubRelease update=UpdateChannel.selectUpdate(rows,"3.1.2",channel);check(update!=null&&update.version.equals("3.1.3")&&update.apkName.equals("CodexMeter-3.1.3-ko.apk"),"Published feed discovered by unchanged installed client parser");}check(UpdateChannel.selectUpdate(rows,"3.1.3","stable")==null,"Public current version not reoffered");}
  System.out.println("Stable313 release: "+n+" checks passed; same production parser/package/signer, increasing code"+(args.length>0?", public feed":"")+".");
 }
}
