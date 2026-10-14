package dev.bennett.codexmeter;
import java.util.*;
import java.nio.file.*;
import org.json.*;

/** Runs twice against the actual normal and generated beta parser/trust sources. */
public final class BetaReleaseSelfTest {
    static int checks;
    static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static JSONObject release(String tag,String asset)throws Exception{
        String base=GitHubReleaseSource.REPOSITORY_URL;
        return new JSONObject().put("tag_name",tag).put("prerelease",tag.startsWith("beta-")).put("html_url",base+"/releases/tag/"+tag).put("assets",new JSONArray()
            .put(new JSONObject().put("name",asset).put("size",24000000).put("browser_download_url",base+"/releases/download/"+tag+"/"+asset))
            .put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",base+"/releases/download/"+tag+"/SHA256SUMS.txt")));
    }
    public static void main(String[] args)throws Exception {
        boolean beta=args[0].equals("beta");JSONArray fixture=new JSONArray().put(release("v3.0.0","CodexMeter-3.0.0-ko.apk")).put(release("beta-v9.9.0","GPT-HUD-BETA-9.9.0.apk")).put(release("beta-v9.9.1","GPT-HUD-BETA-9.9.1.apk")).put(release("beta-v9.9.2","GPT-HUD-BETA-9.9.2.apk"));
        List<GitHubRelease> list=GitHubReleaseParser.parse(fixture.toString());
        check(list.size()==(beta?3:1),"Separate catalog count");check(list.get(0).version.equals(beta?"9.9.2":"3.0.0"),"Separate newest release");
        check(UpdateChannel.selectUpdate(list,beta?"9.9.1":"2.9.2",beta?"stable":"alpha").version.equals(beta?"9.9.2":"3.0.0"),"Installed previous version update");
        check(UpdateChannel.selectUpdate(list,beta?"9.9.2":"3.0.0","alpha")==null,"Same version not offered");
        JSONObject bad=release(beta?"beta-v9.9.2":"v3.0.1",beta?"GPT-HUD-BETA-9.9.2.apk":"CodexMeter-3.0.1-ko.apk");bad.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://github.com/evil/repo/releases/download/bad/app.apk");check(GitHubReleaseParser.parse(new JSONArray().put(bad).toString()).isEmpty(),"Foreign repository refused");
        JSONObject draft=release(beta?"beta-v9.9.2":"v3.0.1",beta?"GPT-HUD-BETA-9.9.2.apk":"CodexMeter-3.0.1-ko.apk").put("draft",true);check(GitHubReleaseParser.parse(new JSONArray().put(draft).toString()).isEmpty(),"Draft refused");
        String pkg=beta?"dev.bennett.codexmeter.trial":"dev.bennett.codexmeter";
        check(KoreanUpdateTrust.compatible(pkg,KoreanUpdateTrust.CERTIFICATE,990000,990001),"Expected signing and code");check(!KoreanUpdateTrust.compatible(beta?"dev.bennett.codexmeter":"dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,86,990001),"Other package refused");check(!KoreanUpdateTrust.compatible(pkg,"wrong",990000,990001),"Wrong cert refused");check(!KoreanUpdateTrust.compatible(pkg,KoreanUpdateTrust.CERTIFICATE,990001,990001),"Equal code refused");check(!KoreanUpdateTrust.compatible(pkg,KoreanUpdateTrust.CERTIFICATE,990001,990000),"Downgrade refused");
        if(args.length>1){List<GitHubRelease> actual=GitHubReleaseParser.parse(Files.readString(Path.of(args[1])));check(actual.get(0).version.equals(beta?"9.9.2":"3.0.0"),"Live public API same contract");}
        System.out.println((beta?"Beta":"Normal")+" updater: "+checks+" checks passed");
    }
}
