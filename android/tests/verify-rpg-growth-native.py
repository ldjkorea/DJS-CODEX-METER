"""Native growth UI on an isolated QA application ID, preserving every existing app."""
from pathlib import Path
from zipfile import ZipFile
import os,subprocess,sys
root=Path(__file__).resolve().parents[1];serial=sys.argv[1];assert serial.startswith('emulator-')
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin';bt=sdk/'build-tools/36.0.0';adb=sdk/'platform-tools/adb.exe';jar=sdk/'platforms/android-36/android.jar';growth='--growth-ux' in sys.argv;regression='--collection-regression' in sys.argv;medals='--medals' in sys.argv or regression;test_class='RpgCollectionNativeTest' if medals and not regression else 'RpgGrowthNativeTest';out=root/('build/medal-growth-regression' if regression else 'build/medal-native' if medals else 'build/growth-ux-native' if growth else 'build/rpg3-native');out.mkdir(parents=True,exist_ok=True)
run=lambda args:subprocess.run(list(map(str,args)),check=True)
if '--presentation' in sys.argv:
    assert medals and not regression
    out=root/'build/presentation-native';out.mkdir(parents=True,exist_ok=True)
shell=lambda *args:subprocess.check_output([str(adb),'-s',serial,'shell',*args],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1'
package='dev.bennett.codexmeter.rpg3qa';test_package='dev.bennett.codexmeter.rpg3test'
apk=root/('build/presentation-native/qa.apk' if '--presentation' in sys.argv else 'build/medal-qa/qa.apk' if medals else 'build/growth-ux-qa/qa.apk' if growth else 'build/rpg3-qa/qa.apk');badging=subprocess.check_output([str(bt/'aapt2.exe'),'dump','badging',str(apk)],text=True,encoding='utf-8');assert "name='"+package+"'" in badging
installed=shell('pm','list','packages',package).splitlines();reuse='--reuse-owned-fixture' in sys.argv
if reuse:assert 'package:'+test_package in shell('pm','list','packages',test_package).splitlines()
else:assert 'package:'+package not in installed and 'package:'+test_package not in shell('pm','list','packages',test_package).splitlines(),'Preserve existing fixture'
manifest=out/'AndroidManifest.xml';manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{test_package}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:label="RPG growth QA" android:debuggable="true"/><instrumentation android:name="dev.bennett.codexmeter.{test_class}" android:targetPackage="{package}"/></manifest>',encoding='utf-8')
deps=[];cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
for aar in cache.rglob('*.aar'):
    if not any(str(aar).split(os.sep)[-5].startswith(g) for g in ['androidx.','io.github.oneuiproject']):continue
    with ZipFile(aar) as z:
        if 'classes.jar' in z.namelist():
            path=out/(aar.parent.name+'.jar');path.write_bytes(z.read('classes.jar'));deps.append(path)
deps+=list(cache.glob('androidx.*/*/*/*/*.jar'))
classes=out/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[jar,root/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes',root/'app/build/intermediates/compile_r_class_jar/release/generateReleaseRFile/R.jar',root/'shared/build/classes/java/main',*deps]))
args=out/'javac.args';args.write_text('-encoding UTF-8 -source 17 -target 17\n-cp "'+cp.replace('\\','/')+'"\n-d "'+str(classes).replace('\\','/')+'"\n"'+str(Path(__file__).with_name(test_class+'.java')).replace('\\','/')+'"',encoding='utf-8')
if medals and not regression:
    with args.open('a',encoding='utf-8') as extra:extra.write('\n"'+str(Path(__file__).with_name('RpgGrowthNativeTest.java')).replace('\\','/')+'"')
run([jdk/'javac.exe','-proc:none','@'+str(args)]);run([bt/'aapt2.exe','link','-I',jar,'--manifest',manifest,'-o',out/'base.apk']);dex=out/'dex';dex.mkdir(exist_ok=True)
run([bt/'d8.bat','--lib',jar,'--min-api','26','--output',dex,*classes.rglob('*.class')])
with ZipFile(out/'base.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
signing=root/'.local-signing';run([bt/'apksigner.bat','sign','--ks',signing/'codex-meter-local.p12','--ks-key-alias','codexmeter','--ks-pass','file:'+str(signing/'password'),'--out',out/'tests.apk',out/'base.apk'])
run([adb,'-s',serial,'install',*(['-r'] if reuse else []),apk]);run([adb,'-s',serial,'install',*(['-r'] if reuse else []),out/'tests.apk'])
result=shell('am','instrument','-r','-w',test_package+'/dev.bennett.codexmeter.'+test_class);(out/'RESULT.txt').write_text(result,encoding='utf-8');print(result);assert 'INSTRUMENTATION_CODE: -1' in result and 'checks passed' in result
run([adb,'-s',serial,'pull','/sdcard/Android/data/'+package+'/files/rpg3-fixtures',out/'captures'])
