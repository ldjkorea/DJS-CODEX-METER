"""Compile native regression against this beta; only an existing owned AVD is allowed."""
from pathlib import Path
from zipfile import ZipFile
import argparse, os, subprocess

p = argparse.ArgumentParser()
p.add_argument('--serial', default='emulator-5590')
p.add_argument('--class-name', default='Beta997CacheNativeTest')
p.add_argument('--expect-cache-gap', action='store_true')
p.add_argument('--cold-restart', action='store_true')
p.add_argument('--auth-restart', action='store_true')
p.add_argument('--locale', default='ko')
p.add_argument('--theme', default='dark')
p.add_argument('--expected-version', default='9.9.7')
p.add_argument('--checkpoint', default='beta997_qa')
p.add_argument('--phase', choices=['seed', 'before', 'verify'], default='verify')
a = p.parse_args()
root = Path(__file__).resolve().parents[1]
out = root / 'build/beta997' / a.class_name
out.mkdir(parents=True, exist_ok=True)
sdk = Path(os.environ['ANDROID_HOME'])
jdk = Path(os.environ['JAVA_HOME']) / 'bin'
bt = sdk / 'build-tools/36.0.0'
adb = sdk / 'platform-tools/adb.exe'
android = sdk / 'platforms/android-36/android.jar'
run = lambda args: subprocess.run(list(map(str, args)), check=True)
shell = lambda *args: subprocess.check_output([str(adb), '-s', a.serial, 'shell', *args], text=True, encoding='utf-8')
assert a.serial.startswith('emulator-') and shell('getprop', 'ro.kernel.qemu').strip() == '1'
avd = subprocess.check_output([str(adb), '-s', a.serial, 'emu', 'avd', 'name'], text=True).splitlines()[0].strip()
assert avd == 'gpthudbeta99', 'Preserve unrelated emulators'
target = 'dev.bennett.codexmeter.trial'
package = 'dev.bennett.codexmeter.beta997test'
assert 'package:' + target in shell('pm', 'list', 'packages', target).splitlines()
deps = []
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
for aar in cache.rglob('*.aar'):
    if not any(str(aar).split(os.sep)[-5].startswith(g) for g in ['androidx.', 'io.github.oneuiproject']):
        continue
    with ZipFile(aar) as z:
        if 'classes.jar' in z.namelist():
            dest = out / (aar.parent.name + '.jar')
            dest.write_bytes(z.read('classes.jar'))
            deps.append(dest)
deps += list(cache.glob('androidx.*/*/*/*/*.jar'))
cp = [android, root/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes',
      root/'app/build/intermediates/compile_r_class_jar/release/generateReleaseRFile/R.jar',
      root/'shared/build/classes/java/main', *deps]
classes = out / 'classes'
classes.mkdir(exist_ok=True)
quote = lambda s: '"' + str(s).replace('\\', '/') + '"'
args = out / 'javac.args'
args.write_text('-encoding UTF-8 -source 17 -target 17\n-cp ' + quote(os.pathsep.join(map(str, cp))) +
                '\n-d ' + quote(classes) + '\n' + quote(root/'trial'/(a.class_name+'.java')), encoding='utf-8')
run([jdk/'javac.exe', '-proc:none', '@'+str(args)])
manifest = out / 'AndroidManifest.xml'
manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:label="Beta997 synthetic tests"/><instrumentation android:name="dev.bennett.codexmeter.{a.class_name}" android:targetPackage="{target}"/></manifest>', encoding='utf-8')
run([bt/'aapt2.exe', 'link', '-I', android, '--manifest', manifest, '-o', out/'base.apk'])
dex = out / 'dex'
dex.mkdir(exist_ok=True)
run([bt/'d8.bat', '--lib', android, '--min-api', '26', '--output', dex, *classes.rglob('*.class')])
with ZipFile(out/'base.apk', 'a') as z:
    z.write(dex/'classes.dex', 'classes.dex')
signing = root / '.local-signing'
run([bt/'apksigner.bat', 'sign', '--ks', signing/'codex-meter-local.p12', '--ks-key-alias', 'codexmeter',
     '--ks-pass', 'file:'+str(signing/'password'), '--out', out/'tests.apk', out/'base.apk'])
run([adb, '-s', a.serial, 'install', '-r', out/'tests.apk'])
component = package+'/dev.bennett.codexmeter.'+a.class_name
if a.cold_restart or a.auth_restart:
    assert a.class_name == ('Beta997AuthProcessNativeTest' if a.auth_restart else 'Beta997ColdStartNativeTest')
    seed = shell('am', 'instrument', '-r', '-w', '-e', 'phase', 'seed', component)
    (out/'seed.txt').write_text(seed, encoding='utf-8')
    print(seed)
    assert 'checks passed' in seed and 'INSTRUMENTATION_CODE: -1' in seed
    shell('am', 'force-stop', target)
    shell('am', 'start', '-W', '-n', target+'/dev.bennett.codexmeter.MainActivity')
    import time
    time.sleep(3)
result = shell('am', 'instrument', '-r', '-w', '-e', 'phase', a.phase, '-e', 'expected_version', a.expected_version, '-e', 'checkpoint', a.checkpoint, '-e', 'locale', a.locale, '-e', 'theme', a.theme, component)
(out/('baseline.txt' if a.expect_cache_gap else 'result.txt')).write_text(result, encoding='utf-8')
print(result)
if a.expect_cache_gap:
    assert 'CACHE_RECOVERY_REQUIRED' in result and 'INSTRUMENTATION_CODE: 0' in result
    print('CONFIRMED: published 9.9.6 does not rebuild missing growth display cache')
else:
    assert 'checks passed' in result and 'INSTRUMENTATION_CODE: -1' in result
