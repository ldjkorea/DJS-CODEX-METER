"""Own emulator only, signed production package upgrade and real-window matrix. No phone access."""
from pathlib import Path
from zipfile import ZipFile
import os,sys,subprocess,json
root=Path(__file__).resolve().parents[1];out=root/'build/refinement/native';out.mkdir(parents=True,exist_ok=True)
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin';bt=sdk/'build-tools/36.0.0';adb=sdk/'platform-tools/adb.exe';jar=sdk/'platforms/android-36/android.jar'
serial=sys.argv[1];assert serial=='emulator-5556','Exclusively owned fixture emulator required'
run=lambda a:subprocess.run(list(map(str,a)),check=True)
shell=lambda *a:subprocess.check_output([str(adb),'-s',serial,'shell',*map(str,a)],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1'
package='dev.bennett.codexmeter';test_package='dev.bennett.codexmeter.refinementtest'
manifest=out/'AndroidManifest.xml';manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{test_package}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:label="Refinement synthetic QA" android:debuggable="true"/><instrumentation android:name="dev.bennett.codexmeter.RpgRefinementNativeTest" android:targetPackage="{package}"/></manifest>',encoding='utf-8')
deps=[];cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
for aar in cache.rglob('*.aar'):
    if not any(str(aar).split(os.sep)[-5].startswith(g) for g in ['androidx.','io.github.oneuiproject']):continue
    with ZipFile(aar) as z:
        if 'classes.jar' in z.namelist():
            path=out/(aar.parent.name+'.jar');path.write_bytes(z.read('classes.jar'));deps.append(path)
deps+=list(cache.glob('androidx.*/*/*/*/*.jar'))
classes=out/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[jar,root/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes',root/'app/build/intermediates/compile_r_class_jar/release/generateReleaseRFile/R.jar',root/'shared/build/classes/java/main',*deps]))
args=out/'javac.args';args.write_text('-encoding UTF-8 -source 17 -target 17\n-cp "'+cp.replace('\\','/')+'"\n-d "'+str(classes).replace('\\','/')+'"\n"'+str(Path(__file__).with_name('RpgRefinementNativeTest.java')).replace('\\','/')+'"',encoding='utf-8')
run([jdk/'javac.exe','-proc:none','@'+str(args)]);run([bt/'aapt2.exe','link','-I',jar,'--manifest',manifest,'-o',out/'base.apk']);dex=out/'dex';dex.mkdir(exist_ok=True)
run([bt/'d8.bat','--lib',jar,'--min-api','26','--output',dex,*classes.rglob('*.class')])
with ZipFile(out/'base.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
signing=root/'.local-signing';run([bt/'apksigner.bat','sign','--ks',signing/'codex-meter-local.p12','--ks-key-alias','codexmeter','--ks-pass','file:'+str(signing/'password'),'--out',out/'tests.apk',out/'base.apk'])
if '--compile-only' in sys.argv:sys.exit(0)
installed=shell('pm','list','packages',package).splitlines()
if '--reuse-owned-production-fixture' not in sys.argv:
    assert 'package:'+package not in installed,'Never overwrite an unknown production installation'
    run([adb,'-s',serial,'install',root/'build/medal-release/final/CodexMeter-2.8.23-ko.apk'])
    run([adb,'-s',serial,'install',out/'tests.apk'])
    seed=shell('am','instrument','-r','-w','-e','mode','seed',test_package+'/dev.bennett.codexmeter.RpgRefinementNativeTest');(out/'SEED.txt').write_text(seed,encoding='utf-8');print(seed);assert 'checks passed' in seed and 'INSTRUMENTATION_CODE: -1' in seed
else:
    assert (out/'SEED.txt').exists() and 'package:'+test_package in shell('pm','list','packages',test_package).splitlines()
    run([adb,'-s',serial,'install','-r',out/'tests.apk'])
run([adb,'-s',serial,'install','-r',root/'app/build/outputs/apk/release/app-release.apk'])
if '--saf-only' in sys.argv:
    run([adb,'-s',serial,'shell','am','force-stop','com.google.android.documentsui'])
    run([adb,'-s',serial,'shell','am','force-stop',package])
    result=shell('am','instrument','-r','-w','-e','mode','saf',test_package+'/dev.bennett.codexmeter.RpgRefinementNativeTest');(out/'SAF.txt').write_text(result,encoding='utf-8');print(result);assert 'checks passed' in result and 'INSTRUMENTATION_CODE: -1' in result
    sys.exit(0)
verify=shell('am','instrument','-r','-w','-e','mode','verify','-e','resumed','true' if '--resume-matrix' in sys.argv else 'false',test_package+'/dev.bennett.codexmeter.RpgRefinementNativeTest');(out/'UPDATE.txt').write_text(verify,encoding='utf-8');print(verify);assert 'checks passed' in verify and 'INSTRUMENTATION_CODE: -1' in verify
run([adb,'-s',serial,'shell','wm','density','160'])
results=[]
for width in [320,360,411,700]:
    for scale in [1,1.5]:
        for locale in ['ko-KR','en-US']:
            label=f'{width}-{scale}-{locale}';run([adb,'-s',serial,'shell','wm','size',f'{width}x1000']);run([adb,'-s',serial,'shell','settings','put','system','font_scale',str(scale)]);run([adb,'-s',serial,'shell','cmd','locale','set-app-locales',package,'--locales',locale]);run([adb,'-s',serial,'shell','cmd','uimode','night','yes'])
            result=shell('am','instrument','-r','-w','-e','mode','matrix','-e','label',label,test_package+'/dev.bennett.codexmeter.RpgRefinementNativeTest');(out/(label+'.txt')).write_text(result,encoding='utf-8');print(label,result.strip(),flush=True);assert 'checks passed' in result and 'INSTRUMENTATION_CODE: -1' in result
            results.append(label)
run([adb,'-s',serial,'pull','/sdcard/Android/data/'+package+'/files/refinement-captures',out/'captures'])
(out/'MATRIX.json').write_text(json.dumps(dict(configurations=results,package=package,production_upgrade='2.8.23/code53 -> 2.8.25/code55',synthetic=True,physical_phone=False),indent=2),encoding='utf-8')
print('Production package non-destructive update and 16 real Android window configurations passed.')
