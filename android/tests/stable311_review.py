"""Explicit 3.1.1 localization deltas, reviewed against published commit 6638a50.

No historical baseline is rewritten. The eight pins below cover five promoted
integration files and three NEW shared classes absent from upstream 7de3b0c.
Unknown changes fail closed; never refresh these hashes from the working tree.
See docs/GPT_HUD_3.1.1.ko.md, local follow-up 2026-10-21.
"""
import hashlib

REVIEWED_COMMIT = "6638a50b3771d717bad9c6704f6c9924bda418fa"
PINS = {
    'android/app/build.gradle.kts': '822aa26d69dfa02d58c184a38c4861c54fe94ae91dca903d3ce13b22243d1d7e',
    'android/wear/build.gradle.kts': 'acd58e85327e407189bc5729c377372f3c92f97a28892856f3ab96b289e05340',
    'android/app/src/main/AndroidManifest.xml': 'a82072a96cc825442472201cc1503f9d503a216d39e702b67949953ed086ac1a',
    'android/app/src/main/java/dev/bennett/codexmeter/AppConstants.java': '387bf48df8cf684a42b0db6d38bdeadcfcc04fbd3a2527ff5a7db77de759a33b',
    'android/app/src/main/java/dev/bennett/codexmeter/UsageApi.java': '967bbc6895015a2111a0ce69f10d3fcbf780edf16e72d3b810d9f4e6a276ffbd',
    'android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java': 'f2307e646f69523ff4a37fd156f8f69ce8177e3511f508747513f16eeeb09320',
    'android/shared/src/main/java/dev/bennett/codexmeter/MxpEconomy.java': '026cac4597f3b03b80e71f742b41678dfc084b26e5e2e64f814576479fac707a',
    'android/shared/src/main/java/dev/bennett/codexmeter/MxpHonors.java': '78ee477dba67ab2712df90e976fb8b324e05747fadad53cc0e4d037282611672',
}


def reviewed_source(relative, raw):
    """Return False for upstream files; require EXACT published bytes otherwise."""
    if relative not in PINS:
        return False
    actual = hashlib.sha256(raw.replace(b'\r\n', b'\n')).hexdigest()
    assert actual == PINS[relative], ('Unreviewed 3.1.1 source change', relative)
    return True


def reviewed_update_cache_guard(prior):
    """Exact unreleased fail-closed delta; no URL, size or signing trust changes."""
    parsed = '        List<GitHubRelease> parsed = GitHubReleaseParser.parse(json, BuildConfig.DEBUG);\n'
    editor = '        SharedPreferences.Editor editor = prefs(context).edit()\n'
    anchor = parsed + editor
    assert prior.count(anchor) == 1, 'Original update save contract changed'
    return prior.replace(anchor, parsed + '''        // A populated but incompatible feed is not a successful empty update list.
        // Keep the prior cache/ETag; the caller records the failure for the existing UI.
        if (parsed.isEmpty() && new org.json.JSONArray(json).length() > 0) {
            throw new IllegalStateException(context.getString(R.string.update_metadata_incompatible));
        }
''' + editor)


def strip_runtime_delta(relative, text):
    """Undo only the documented demo network fences, then compare ALL old bytes."""
    pairs = []
    if relative.endswith('/OAuthClient.java'):
        pairs = [
            ('', '        if(TrialDemo.ACCOUNT.equals(authTokens.accountId))throw new Exception("Synthetic demo cannot refresh OAuth");\n'),
            ('', '        if(authTokens!=null&&TrialDemo.ACCOUNT.equals(authTokens.accountId))return;\n'),
        ]
    elif relative.endswith('/RefreshScheduler.java'):
        pairs = [('', line) for line in [
            '        if(context!=null&&TrialDemo.active(context)){cancelAll(context);return true;}\n',
            '        if(context!=null&&usageSnapshot!=null&&TrialDemo.active(context)){cancelAll(context);return true;}\n',
        ]]
        # First line is intentionally identical in schedulePeriodic/scheduleNextShort.
        assert text.count(pairs[0][1]) == 2, (relative, 'Two reviewed scheduling fences')
        text = text.replace(pairs[0][1], '')
        pairs = pairs[1:]
    elif relative.endswith('/UsageRefreshJobService.java'):
        pairs = [('', '''        if(TrialDemo.active(this)){
            RefreshScheduler.cancelAll(this);
            DiagnosticLog.info(this,"scheduler","refresh_job_skipped_demo","job_id",jobParameters.getJobId());
            return false;
        }
'''), ('''                    RefreshScheduler.scheduleAtNextReset(UsageRefreshJobService.this.getApplicationContext(), UsageApi.refreshAndCache(UsageRefreshJobService.this.getApplicationContext()));
''', '''                    UsageSnapshot refreshed;
                    synchronized(UsageApi.NETWORK_LOCK){
                        // Fence account-to-demo transitions before any synthetic snapshot write.
                        if(stopped||TrialDemo.active(UsageRefreshJobService.this)){
                            UsageRefreshJobService.this.active.remove(Integer.valueOf(params.getJobId()),this);
                            if(!stopped){DiagnosticLog.info(UsageRefreshJobService.this,"scheduler","refresh_job_skipped_demo_worker","job_id",params.getJobId());RefreshScheduler.cancelAll(UsageRefreshJobService.this);UsageRefreshJobService.this.jobFinished(params,false);}
                            return;
                        }
                        refreshed=UsageApi.refreshAndCache(UsageRefreshJobService.this.getApplicationContext());
                    }
                    RefreshScheduler.scheduleAtNextReset(UsageRefreshJobService.this.getApplicationContext(),refreshed);
''')]
    for before, after in pairs:
        assert text.count(after) == 1, (relative, 'Reviewed demo fence changed')
        text = text.replace(after, before)
    return text
