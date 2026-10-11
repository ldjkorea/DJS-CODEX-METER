package dev.bennett.codexmeter;
import java.nio.file.*;import java.util.*;import org.json.*;

/** Final stable release contract; optional real GitHub JSON readback. No install/network operation. */
public final class RpgReleaseSelfTest {
    static int checks;static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception {
        String base="https://github.com/ldjkorea/codex-meter-ko",tag="v2.8.25",name="CodexMeter-2.8.25-ko.apk";
        JSONObject release=new JSONObject().put("tag_name",tag).put("draft",false).put("prerelease",false).put("name","GPT HUD 2.8.25").put("html_url",base+"/releases/tag/"+tag)
            .put("assets",new JSONArray().put(new JSONObject().put("name",name).put("size",26810664).put("browser_download_url",base+"/releases/download/"+tag+"/"+name))
            .put(new JSONObject().put("name","SHA256SUMS.txt").put("browser_download_url",base+"/releases/download/"+tag+"/SHA256SUMS.txt")));
        String json=args.length==0?new JSONArray().put(release).toString():Files.readString(Path.of(args[0]));List<GitHubRelease> releases=GitHubReleaseParser.parse(json);GitHubRelease parsed=GitHubReleaseParser.findVersion(releases,"2.8.25");
        long expectedSize=args.length==0?26810664:Long.parseLong(args[1]);
        check(parsed!=null,"Final release recognized by production updater");check(!parsed.prerelease,"Stable final, no intermediate beta");check(parsed.apkName.equals(name)&&parsed.apkSize==expectedSize,"Exact phone filename and verified size");
        for(String channel:new String[]{UpdateChannel.STABLE,UpdateChannel.ALPHA}){check(UpdateChannel.selectUpdate(releases,"2.8.22",channel)==parsed,"Previous installed version receives final update");check(UpdateChannel.selectUpdate(releases,"2.8.23",channel)==parsed,"Public v23 installed version receives final update");check(UpdateChannel.selectUpdate(releases,"2.8.25",channel)==null,"No equal-version reinstall");check(UpdateChannel.selectUpdate(releases,"2.8.26",channel)==null,"No downgrade");}
        System.out.println("RPG release/update channel: "+checks+" checks passed; "+(args.length==0?"synthetic metadata":"actual GitHub metadata readback"));
    }
}
