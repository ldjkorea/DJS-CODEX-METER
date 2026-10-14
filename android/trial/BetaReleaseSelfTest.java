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
        boolean beta=args[0].equals("beta");JSONArray fixture=new JSONArray().put(release("v3.0.0","CodexMeter-3.0.0-ko.apk")).put(release("beta-v9.9.0","GPT-HUD-BETA-9.9.0.apk")).put(release("beta-v9.9.1","GPT-HUD-BETA-9.9.1.apk")).put(release("beta-v9.9.2","GPT-HUD-BETA-9.9.2.apk")).put(release("beta-v9.9.3","GPT-HUD-BETA-9.9.3.apk"));
        List<GitHubRelease> list=GitHubReleaseParser.parse(fixture.toString());
        check(list.size()==(beta?4:1),"Separate catalog count");check(list.get(0).version.equals(beta?"9.9.3":"3.0.0"),"Separate newest release");
        check(UpdateChannel.selectUpdate(list,beta?"9.9.2":"2.9.2",beta?"stable":"alpha").version.equals(beta?"9.9.3":"3.0.0"),"Installed previous version update");
        check(UpdateChannel.selectUpdate(list,beta?"9.9.3":"3.0.0","alpha")==null,"Same version not offered");
        JSONArray newer=new JSONArray(fixture.toString()).put(release("beta-v9.9.4","GPT-HUD-BETA-9.9.4.apk"));
        List<GitHubRelease> nextList=GitHubReleaseParser.parse(newer.toString());
        check(nextList.get(0).version.equals(beta?"9.9.4":"3.0.0"),"9.9.4 candidate stays beta-only");
        if(beta){check(UpdateChannel.selectUpdate(nextList,"9.9.3","stable").version.equals("9.9.4"),"9.9.3 to 9.9.4 update discovery");check(UpdateChannel.selectUpdate(nextList,"9.9.4","alpha")==null,"9.9.4 not offered again");}
        JSONArray newest=new JSONArray(newer.toString()).put(release("beta-v9.9.5","GPT-HUD-BETA-9.9.5.apk"));List<GitHubRelease> newestList=GitHubReleaseParser.parse(newest.toString());
        check(newestList.get(0).version.equals(beta?"9.9.5":"3.0.0"),"9.9.5 beta-only selection");
        if(beta){for(String oldVersion:new String[]{"9.9.0","9.9.1","9.9.2","9.9.3","9.9.4"})for(String channel:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(newestList,oldVersion,channel).version.equals("9.9.5"),"Previous beta update "+oldVersion+channel);check(UpdateChannel.selectUpdate(newestList,"9.9.5","stable")==null,"Current beta not offered again");}
        JSONArray beta996=new JSONArray(newest.toString()).put(release("beta-v9.9.6","GPT-HUD-BETA-9.9.6.apk"));List<GitHubRelease> beta996List=GitHubReleaseParser.parse(beta996.toString());check(beta996List.get(0).version.equals(beta?"9.9.6":"3.0.0"),"9.9.6 beta-only");if(beta){for(String previous:new String[]{"9.9.0","9.9.1","9.9.2","9.9.3","9.9.4","9.9.5"})for(String channel:new String[]{"stable","alpha"})check(UpdateChannel.selectUpdate(beta996List,previous,channel).version.equals("9.9.6"),"9.9.6 update "+previous+channel);check(UpdateChannel.selectUpdate(beta996List,"9.9.6","alpha")==null,"Same9.9.6 not offered again");}
        JSONObject bad=release(beta?"beta-v9.9.2":"v3.0.1",beta?"GPT-HUD-BETA-9.9.2.apk":"CodexMeter-3.0.1-ko.apk");bad.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://github.com/evil/repo/releases/download/bad/app.apk");check(GitHubReleaseParser.parse(new JSONArray().put(bad).toString()).isEmpty(),"Foreign repository refused");
        JSONObject draft=release(beta?"beta-v9.9.2":"v3.0.1",beta?"GPT-HUD-BETA-9.9.2.apk":"CodexMeter-3.0.1-ko.apk").put("draft",true);check(GitHubReleaseParser.parse(new JSONArray().put(draft).toString()).isEmpty(),"Draft refused");
        String pkg=beta?"dev.bennett.codexmeter.trial":"dev.bennett.codexmeter";
        check(KoreanUpdateTrust.compatible(pkg,KoreanUpdateTrust.CERTIFICATE,990000,990001),"Expected signing and code");check(!KoreanUpdateTrust.compatible(beta?"dev.bennett.codexmeter":"dev.bennett.codexmeter.trial",KoreanUpdateTrust.CERTIFICATE,86,990001),"Other package refused");check(!KoreanUpdateTrust.compatible(pkg,"wrong",990000,990001),"Wrong cert refused");check(!KoreanUpdateTrust.compatible(pkg,KoreanUpdateTrust.CERTIFICATE,990001,990001),"Equal code refused");check(!KoreanUpdateTrust.compatible(pkg,KoreanUpdateTrust.CERTIFICATE,990001,990000),"Downgrade refused");
        if(args.length>1){List<GitHubRelease> actual=GitHubReleaseParser.parse(Files.readString(Path.of(args[1])));check(actual.get(0).version.equals(beta?"9.9.6":"3.0.0"),"Live public API same contract");}
        System.out.println((beta?"Beta":"Normal")+" updater: "+checks+" checks passed");
    }
}
