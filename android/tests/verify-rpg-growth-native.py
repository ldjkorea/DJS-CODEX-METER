"""Native growth UI on an isolated QA application ID, preserving every existing app."""
from pathlib import Path
from zipfile import ZipFile
import os,subprocess,sys
root=Path(__file__).resolve().parents[1];serial=sys.argv[1];assert serial.startswith('emulator-')
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin';bt=sdk/'build-tools/36.0.0';adb=sdk/'platform-tools/adb.exe';jar=sdk/'platforms/android-36/android.jar';growth='--growth-ux' in sys.argv;regression='--collection-regression' in sys.argv;medals='--medals' in sys.argv or regression;test_class='RpgCollectionNativeTest' if medals and not regression else 'RpgGrowthNativeTest';out=root/('build/medal-growth-regression' if regression else 'build/medal-native' if medals else 'build/growth-ux-native' if growth else 'build/rpg3-native');out.mkdir(parents=True,exist_ok=True)
if '--achievement-journey' in sys.argv:
    test_class='AchievementJourneyNativeTest';out=root/'build/achievement-native';out.mkdir(parents=True,exist_ok=True)
if '--reborn' in sys.argv:
    test_class='GrowthRebornNativeTest';out=root/'build/reborn-native';out.mkdir(parents=True,exist_ok=True)
if '--reborn30' in sys.argv:
    test_class='GrowthRebornNativeTest';out=root/'build/reborn30-native';out.mkdir(parents=True,exist_ok=True)
if '--widget30' in sys.argv:
    test_class='WidgetSimplificationNativeTest';out=root/'build/widget30-native';out.mkdir(parents=True,exist_ok=True)
if '--phase-a' in sys.argv:
    test_class='PhaseANativeTest';out=root/'build/phase-a-native';out.mkdir(parents=True,exist_ok=True)
if '--rebornA' in sys.argv:
    test_class='GrowthRebornNativeTest';out=root/'build/phase-a-growth';out.mkdir(parents=True,exist_ok=True)
if '--rebornB' in sys.argv:
    test_class='GrowthRebornNativeTest';out=root/'build/phase-b-growth';out.mkdir(parents=True,exist_ok=True)
if '--phase-b' in sys.argv:
    test_class='PhaseBNativeTest';out=root/'build/phase-b-native';out.mkdir(parents=True,exist_ok=True)
if '--phase-c' in sys.argv:
    test_class='PhaseCNativeTest';out=root/'build/phase-c-native';out.mkdir(parents=True,exist_ok=True)
if '--phase-c-tasks' in sys.argv or '--phase-c-growth' in sys.argv:
    test_class='PhaseBNativeTest' if '--phase-c-tasks' in sys.argv else 'GrowthRebornNativeTest';out=root/('build/phase-c-tasks' if '--phase-c-tasks' in sys.argv else 'build/phase-c-growth');out.mkdir(parents=True,exist_ok=True)
if '--phase-d' in sys.argv or '--phase-d-baseline' in sys.argv:
    test_class='PhaseDNativeTest';out=root/('build/phase-d-native' if '--phase-d' in sys.argv else 'build/phase-d-baseline');out.mkdir(parents=True,exist_ok=True)
if '--phase-d-growth' in sys.argv or '--phase-d-tasks' in sys.argv:
    test_class='GrowthRebornNativeTest' if '--phase-d-growth' in sys.argv else 'PhaseBNativeTest';out=root/('build/phase-d-growth' if '--phase-d-growth' in sys.argv else 'build/phase-d-tasks');out.mkdir(parents=True,exist_ok=True)
if '--phase-d-platform' in sys.argv:
    test_class='PhaseCNativeTest';out=root/'build/phase-d-platform';out.mkdir(parents=True,exist_ok=True)
phase_e='--phase-e-upgrade' in sys.argv or '--phase-e-ui' in sys.argv
phase=next((a.split('=',1)[1] for a in sys.argv if a.startswith('--fixture-phase=')),'verify')
if phase_e:
    assert phase in ['legacy','checkpoint','verify']
    test_class='PhaseEUiNativeTest' if '--phase-e-ui' in sys.argv else 'PhaseEUpgradeNativeTest';out=root/('build/phase-e-ui' if '--phase-e-ui' in sys.argv else 'build/phase-e-upgrade-'+phase);out.mkdir(parents=True,exist_ok=True)
run=lambda args:subprocess.run(list(map(str,args)),check=True)
if '--presentation' in sys.argv:
    assert medals and not regression
    out=root/'build/presentation-native';out.mkdir(parents=True,exist_ok=True)
shell=lambda *args:subprocess.check_output([str(adb),'-s',serial,'shell',*args],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1'
package='dev.bennett.codexmeter.rpg3qa';test_package='dev.bennett.codexmeter.rpg3test'
apk=root/('build/widget30-qa/qa.apk' if '--widget30' in sys.argv or '--reborn30' in sys.argv else 'build/reborn-qa/qa.apk' if '--reborn' in sys.argv else 'build/achievement-qa/qa.apk' if '--achievement-journey' in sys.argv else 'build/presentation-native/qa.apk' if '--presentation' in sys.argv else 'build/medal-qa/qa.apk' if medals else 'build/growth-ux-qa/qa.apk' if growth else 'build/rpg3-qa/qa.apk');apk=root/'build/phase-a-qa/qa.apk' if '--phase-a' in sys.argv or '--rebornA' in sys.argv else apk
if '--phase-b' in sys.argv or '--rebornB' in sys.argv:apk=root/'build/phase-b-qa/qa.apk'
if any(flag in sys.argv for flag in ['--phase-c','--phase-c-tasks','--phase-c-growth','--phase-d-baseline']):apk=root/'build/phase-c-qa/qa.apk'
if any(flag in sys.argv for flag in ['--phase-d','--phase-d-growth','--phase-d-tasks','--phase-d-platform']):apk=root/'build/phase-d-qa/qa.apk'
if phase_e:apk=root/('build/phase-e-qa/'+{'legacy':'v28','checkpoint':'v30','verify':'v50'}[phase]+'.apk');package='dev.bennett.codexmeter.phaseeqa';test_package='dev.bennett.codexmeter.phaseetest'
if '--phase-e-regression' in sys.argv:
    assert not phase_e
    apk=root/'build/phase-e-regression-qa/qa.apk';out=root/'build/phase-e-regression'/test_class;out.mkdir(parents=True,exist_ok=True)
if '--phase-e-baseline' in sys.argv:
    assert '--phase-d-baseline' in sys.argv and not phase_e
    apk=root/'build/phase-e-qa/perf-v30.apk';out=root/'build/phase-e-performance-before';out.mkdir(parents=True,exist_ok=True)
badging=subprocess.check_output([str(bt/'aapt2.exe'),'dump','badging',str(apk)],text=True,encoding='utf-8');assert "name='"+package+"'" in badging
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
if '--widget30' in sys.argv:
    assert reuse,'Widget update fixture requires the existing isolated QA app'
    run([adb,'-s',serial,'install','-r',out/'tests.apk'])
    shell('appwidget','grantbind','--package',package,'--user','0')
    seed=shell('am','instrument','-r','-w','-e','phase','seed',test_package+'/dev.bennett.codexmeter.'+test_class);(out/'SEED.txt').write_text(seed,encoding='utf-8');print(seed)
    assert 'INSTRUMENTATION_CODE: -1' in seed and 'checks passed' in seed
run([adb,'-s',serial,'install',*(['-r'] if reuse else []),apk]);run([adb,'-s',serial,'install',*(['-r'] if reuse else []),out/'tests.apk'])
if phase_e and phase=='checkpoint':shell('appwidget','grantbind','--package',package,'--user','0')
try:
    result=shell('am','instrument','-r','-w',*(['-e','optimized','true'] if '--phase-d' in sys.argv else []),*(['-e','phase',phase] if phase_e else []),*(['-e','resume','true'] if '--resume-checkpoint' in sys.argv else []),test_package+'/dev.bennett.codexmeter.'+test_class);(out/'RESULT.txt').write_text(result,encoding='utf-8');print(result);assert 'INSTRUMENTATION_CODE: -1' in result and 'checks passed' in result
finally:
    if '--widget30' in sys.argv:shell('appwidget','revokebind','--package',package,'--user','0')
    if phase_e and phase=='checkpoint':shell('appwidget','revokebind','--package',package,'--user','0')
if not phase_e:run([adb,'-s',serial,'pull','/sdcard/Android/data/'+package+('/files/phase-d-fixtures' if '--phase-d' in sys.argv or '--phase-d-baseline' in sys.argv else '/files/phase-c-fixtures' if '--phase-c' in sys.argv or '--phase-d-platform' in sys.argv else '/files/phase-b-fixtures' if '--phase-b' in sys.argv or '--phase-c-tasks' in sys.argv or '--phase-d-tasks' in sys.argv else '/files/phase-a-fixtures' if '--phase-a' in sys.argv else '/files/widget30-fixtures' if '--widget30' in sys.argv else '/files/reborn-fixtures' if '--reborn' in sys.argv or '--reborn30' in sys.argv or '--rebornA' in sys.argv or '--rebornB' in sys.argv or '--phase-c-growth' in sys.argv or '--phase-d-growth' in sys.argv else '/files/rpg3-fixtures'),out/'captures'])

if '--phase-e-ui' in sys.argv:run([adb,'-s',serial,'pull','/sdcard/Android/data/'+package+'/files/phase-e-ui',out/'captures'])
