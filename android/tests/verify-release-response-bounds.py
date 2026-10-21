"""Narrow offline JVM boundary tests + optional pre-release metadata budget gate.

--release-json must be a fresh anonymous /releases?per_page=30 response before a
release. Exit 2 means fewer than --reserve-bytes remain, 1 means incompatible.
Synthetic success alone is NOT a public release budget or install verification.
"""
from pathlib import Path
import argparse
import json
import os
import shutil
import subprocess

LEGACY_LIMIT = 524288
CURRENT_LIMIT = 2 * 1024 * 1024
LEGACY_COMMIT = 'afc7d0b9749a42c034e7b7c7305cf8a6cd7fd23a'
DEFAULT_RESERVE = 32768


def budget(raw, reserve=DEFAULT_RESERVE, additional=0):
    if reserve < 0 or reserve > LEGACY_LIMIT:
        raise ValueError('Reserve must be within legacy byte limit')
    if additional < 0:
        raise ValueError('Projected additional bytes must not be negative')
    projected = len(raw) + additional
    if projected > LEGACY_LIMIT:
        return 1, f'INCOMPATIBLE: observed={len(raw)}, projected={projected} bytes > legacy {LEGACY_LIMIT}; old clients cannot discover updates.'
    if not isinstance(json.loads(raw), list):
        raise ValueError('Expected the releases list, not /latest or an error object')
    remaining = LEGACY_LIMIT - projected
    if remaining < reserve:
        return 2, f'AT RISK: observed={len(raw)}, projected={projected} bytes; only {remaining} bytes remain, reserve={reserve}. Recheck post-release response before publishing.'
    return 0, f'Budget OK: observed={len(raw)}, projected={projected} bytes; {remaining} bytes remain, reserve={reserve}; point-in-time estimate only.'


def run_jvm(public_json=None):
    root = Path(__file__).resolve().parents[2]
    android = root / 'android'
    out = android / 'build/release-response-bounds'
    out.mkdir(parents=True, exist_ok=True)
    jar = android / 'build/test-libs/json-20250517.jar'
    assert jar.is_file(), 'Use the existing JSON test jar; this test never downloads dependencies.'
    java = Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else Path(shutil.which('java')).parent
    suffix = '.exe' if os.name == 'nt' else ''
    # Minimal in-memory Android interfaces. Actual UpdatePreferences/parser/client run unchanged.
    fixtures = {
        'android/content/SharedPreferences.java': '''package android.content; import java.util.*;
public class SharedPreferences {final Map<String,Object> values=new HashMap<>();
public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
public boolean getBoolean(String k,boolean d){return (Boolean)values.getOrDefault(k,d);}
public int getInt(String k,int d){return (Integer)values.getOrDefault(k,d);}
public long getLong(String k,long d){return (Long)values.getOrDefault(k,d);}
public boolean contains(String k){return values.containsKey(k);} public Editor edit(){return new Editor();}
public class Editor {final Map<String,Object> pending=new HashMap<>();
public Editor putString(String k,String v){pending.put(k,v);return this;}
public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
public Editor putInt(String k,int v){pending.put(k,v);return this;}
public Editor putLong(String k,long v){pending.put(k,v);return this;}
public Editor remove(String k){pending.put(k,null);return this;}
public void apply(){pending.forEach((k,v)->{if(v==null)values.remove(k);else values.put(k,v);});}}}
''',
        'android/content/Context.java': '''package android.content; public class Context {
public static final int MODE_PRIVATE=0; private final SharedPreferences prefs=new SharedPreferences();
public Context getApplicationContext(){return this;} public SharedPreferences getSharedPreferences(String n,int m){return prefs;}
public String getString(int id){return "Unavailable";} public String getPackageName(){return "dev.bennett.codexmeter";}
public android.content.pm.PackageManager getPackageManager(){return new android.content.pm.PackageManager();}
public void sendBroadcast(Intent i,String permission){} }
''',
        'android/content/Intent.java': '''package android.content; public class Intent {
public static final int FLAG_RECEIVER_REGISTERED_ONLY=1;public Intent(String s){}
public Intent setPackage(String s){return this;}public Intent addFlags(int i){return this;}}
''',
        'android/content/pm/PackageManager.java': '''package android.content.pm; public class PackageManager {
public PackageInfo getPackageInfo(String name,int flags){return new PackageInfo();}}
''',
        'android/content/pm/PackageInfo.java': 'package android.content.pm; public class PackageInfo {public String versionName="3.1.0";}',
        'android/os/SystemClock.java': 'package android.os; public class SystemClock {public static long elapsedRealtime(){return 100;}}',
        'android/os/Build.java': 'package android.os; public class Build {public static String MODEL="fixture";public static class VERSION{public static String RELEASE="fixture";}}',
        'dev/bennett/codexmeter/BuildConfig.java': 'package dev.bennett.codexmeter; public class BuildConfig {public static final boolean DEBUG=false,KOREAN_UPDATES_ENABLED=true; public static final String UPDATE_API_URL="";}',
        'dev/bennett/codexmeter/R.java': 'package dev.bennett.codexmeter; public class R {public static class string {public static final int ko_updates_unavailable=1,update_metadata_incompatible=2;}}',
        'dev/bennett/codexmeter/DiagnosticLog.java': '''package dev.bennett.codexmeter;public class DiagnosticLog {
public static void info(Object...args){}public static void error(Object...args){}}
''',
        'dev/bennett/codexmeter/UpdateNotificationManager.java': '''package dev.bennett.codexmeter;public class UpdateNotificationManager {
public static void dismiss(Object c){}public static void onReleasesUpdated(Object c){}}
''',
    }
    files = []
    for relative, source in fixtures.items():
        target = out / 'fixtures' / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding='utf-8')
        files.append(target)
    app = android / 'app/src/main/java/dev/bennett/codexmeter'
    files += [app / 'DiagnosticSanitizer.java', android / 'tests/ReleaseResponseBoundsSelfTest.java']
    contracts = ['AppConstants','GitHubRelease','GitHubReleaseSource','GitHubReleaseParser','ReleaseVersion','UpdateChannel','UpdateCheckFrequency','ReleaseUpdateClient','UpdatePreferences']
    incompatible = False
    for label, ref, limit in [('legacy-3.1.0', LEGACY_COMMIT, LEGACY_LIMIT), ('published-3.1.1', '6638a50b3771d717bad9c6704f6c9924bda418fa', CURRENT_LIMIT), ('current', None, CURRENT_LIMIT)]:
        target = out / label
        target.mkdir(exist_ok=True)
        actual = []
        for name in contracts + ['KoreanUpdateTrust']:
            path = ('android/shared' if name=='KoreanUpdateTrust' else 'android/app')+'/src/main/java/dev/bennett/codexmeter/' + name + '.java'
            if ref:
                dest = target / (name + '.java')
                dest.write_bytes(subprocess.check_output(['git', 'show', ref + ':' + path], cwd=root))
                actual.append(dest)
            else:
                actual.append(root / path)
        quote = lambda value: '"' + str(value).replace('\\', '/') + '"'
        args = target / 'javac.args'
        args.write_text('-encoding UTF-8\n-cp ' + quote(jar) + '\n-d ' + quote(target) + '\n' + '\n'.join(map(quote, files + actual)), encoding='utf-8')
        subprocess.run([str(java / ('javac' + suffix)), '@' + str(args)], check=True)
        command = [str(java / ('java' + suffix)), '-Xmx128m', '-ea', '-cp', str(target) + os.pathsep + str(jar), 'dev.bennett.codexmeter.ReleaseResponseBoundsSelfTest']
        subprocess.run(command + [str(limit), 'legacy' if ref else 'guarded'], check=True)
        if public_json:
            print('Public metadata compatibility:', label, flush=True)
            result = subprocess.run(command + ['--probe', str(public_json)])
            if result.returncode not in (0, 1):
                raise RuntimeError('Metadata probe crashed')
            incompatible |= result.returncode == 1
    return incompatible


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--release-json', type=Path)
    parser.add_argument('--reserve-bytes', type=int, default=DEFAULT_RESERVE)
    parser.add_argument('--additional-bytes', type=int, default=0, help='Estimated net response growth; never substitutes for a fresh post-release response')
    parser.add_argument('--budget-only', action='store_true')
    args = parser.parse_args()
    if args.budget_only and not args.release_json:
        parser.error('--budget-only requires --release-json')
    incompatible = run_jvm(args.release_json) if not args.budget_only else False
    if args.release_json:
        with args.release_json.open('rb') as stream:
            raw = stream.read(CURRENT_LIMIT + 1)
        code, message = budget(raw, args.reserve_bytes, args.additional_bytes)
        print(message, flush=True)
        if args.budget_only:
            print('Byte budget only: installed parser/download compatibility NOT CHECKED.', flush=True)
        if incompatible:
            print('INCOMPATIBLE: installed parser selects no stable release. Do not publish an app update as compatible.', flush=True)
            code = 1
        raise SystemExit(code)
    print('Synthetic bounds passed. Public release budget NOT CHECKED: pass a fresh --release-json before publishing.', flush=True)


if __name__ == '__main__':
    main()
