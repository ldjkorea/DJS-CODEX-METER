package dev.bennett.codexmeter;
import java.util.*;import org.json.*;
/** Stable promotion uses the deployed parser/trust contract; historical fixtures are retained. */
public final class Stable311ReleaseSelfTest {
 static int checks;static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 static JSONObject release(String tag,String asset,boolean pre)throws Exception{String b=GitHubReleaseSource.REPOSITORY_URL;return new JSONObject().put("tag_name",tag).put("prerelease",pre).put("html_url",b+"/releases/tag/"+tag).put("assets",new JSONArray().put(new JSONObject().put("name",asset).put("size",24700000).put("browser_download_url",b+"/releases/download/"+tag+"/"+asset)).put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",b+"/releases/download/"+tag+"/SHA256SUMS.txt")));}
 public static void main(String[] a)throws Exception{
  JSONObject stable=release("v3.1.1","CodexMeter-3.1.1-ko.apk",false);JSONArray fixtures=new JSONArray().put(release("v3.1.0","CodexMeter-3.1.0-ko.apk",false)).put(release("beta-v9.9.11","GPT-HUD-BETA-9.9.11.apk",true)).put(stable);
  List<GitHubRelease> rows=GitHubReleaseParser.parse(fixtures.toString());check(rows.size()==2,"Only stable assets accepted");check(rows.get(0).version.equals("3.1.1"),"Stable latest");
  for(String old:new String[]{"2.9.2","2.9.3","3.0.0","3.1.0"})for(String ch:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(rows,old,ch).version.equals("3.1.1"),"Previous stable discovery "+old+ch);
  for(String ch:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(rows,"3.1.1",ch)==null,"No reinstall");
  check(ReleaseVersion.compare("0.3.9","3.1.0")<0,"Rejected lower naming preserves comparison");
  check(KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,87,88),"Same stable identity advances code88");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,87,88),"Beta package refused");
  check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter","WRONG",87,88),"Other signing refused");check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,88,88),"Same code refused");check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,88,87),"Downgrade refused");
  JSONObject draft=new JSONObject(stable.toString()).put("draft",true);check(GitHubReleaseParser.parse(new JSONArray().put(draft).toString()).isEmpty(),"Draft excluded");
  JSONObject foreign=new JSONObject(stable.toString());foreign.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://github.com/other/project/releases/download/v3.1.1/CodexMeter-3.1.1-ko.apk");check(GitHubReleaseParser.parse(new JSONArray().put(foreign).toString()).isEmpty(),"Foreign source refused");
  System.out.println("Stable311 updater: "+checks+" checks passed; deployed parser and signing boundary");
 }
}
