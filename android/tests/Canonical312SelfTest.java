package dev.bennett.codexmeter;
import java.util.*;import org.json.*;
/** Exact user-approved roots; original signer/package gates stay mandatory. */
public final class Canonical312SelfTest {
 static int n;static void check(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 static JSONObject row(String root){String tag="v3.1.2";return new JSONObject().put("tag_name",tag).put("html_url",root+"/releases/tag/"+tag).put("assets",new JSONArray().put(new JSONObject().put("name","CodexMeter-3.1.2-ko.apk").put("size",100).put("browser_download_url",root+"/releases/download/"+tag+"/CodexMeter-3.1.2-ko.apk")).put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",root+"/releases/download/"+tag+"/SHA256SUMS.txt")));}
 public static void main(String[] a)throws Exception {
 String base="https://github.com/ldjkorea/DJS-GPT-HUD";
 check(GitHubReleaseSource.REPOSITORY_URL.equals(base),"Canonical repository");
 check(GitHubReleaseSource.RELEASES_API_URL.equals("https://api.github.com/repos/ldjkorea/DJS-GPT-HUD/releases?per_page=30"),"Canonical API");
 for(String name:new String[]{"DJS-GPT-HUD","DJS-CODEX-METER","codex-meter-ko"}){
  String root="https://github.com/ldjkorea/"+name,url=root+"/releases/tag/v3.1.2";
  check(KoreanUpdateTrust.official(url),"Exact root "+name);
  check(KoreanUpdateTrust.releaseRoot(url).equals(root),"Root identity retained");
  var rows=GitHubReleaseParser.parse(new JSONArray().put(row(root)).toString());
  check(rows.size()==1&&UpdateChannel.selectUpdate(rows,"3.1.1","stable").version.equals("3.1.2"),"New update on approved root");
  check(UpdateChannel.selectUpdate(rows,"3.1.2","stable")==null,"No reinstall");
  for(String bad:new String[]{url+"?x=1",url+"#x",url.replace("https:","http:"),url.replace("github.com/","github.com:443/"),url.replace("github.com/","user@github.com/"),url.replace("github.com/","github.com.evil.example/"),url.replace("/ldjkorea/","/other/"),url.replace("/releases/","/releases-other/"),url.replace("/tag/","/../tag/"),url.replace("/tag/","/%74ag/")})check(!KoreanUpdateTrust.official(bad),"Malformed/foreign address refused");
  JSONObject mixed=row(root);mixed.getJSONArray("assets").getJSONObject(0).put("browser_download_url",(name.equals("DJS-GPT-HUD")?"https://github.com/ldjkorea/codex-meter-ko":base)+"/releases/download/v3.1.2/CodexMeter-3.1.2-ko.apk");
  check(GitHubReleaseParser.parse(new JSONArray().put(mixed).toString()).isEmpty(),"Mixed-root metadata refused");
 }
 check(!KoreanUpdateTrust.official("https://github.com/ldjkorea/UNAPPROVED-GPT-HUD/releases/tag/v3.1.2"),"Same owner is not sufficient");
 check(KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,88,89),"Same identity advances code");
 check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,88,89),"Cross-package refused");
 check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter","wrong",88,89),"Other cert refused");
 check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,89,89),"Equal code refused");
 check(!KoreanUpdateTrust.compatible("dev.bennett.codexmeter",KoreanUpdateTrust.CERTIFICATE,89,88),"Downgrade refused");
 System.out.println("DJS-GPT-HUD migration: "+n+" checks passed; exact roots, mixed-root rejection, original signer/package/version.");
 }
}
