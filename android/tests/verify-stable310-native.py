"""Companion instrumentation for the explicitly owned emulator; never install on user devices."""
from pathlib import Path
from zipfile import ZipFile
import os,subprocess,sys,argparse
p=argparse.ArgumentParser();p.add_argument('--phase',choices=['seed','upgrade','ui','final-before','final-after'],required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[1];work=root;out=root/('build/native310-'+a.phase);out.mkdir(exist_ok=True)
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin';bt=sdk/'build-tools/36.0.0';adb=sdk/'platform-tools/adb.exe';jar=sdk/'platforms/android-36/android.jar'
serial='emulator-5592';package='dev.bennett.codexmeter';test_package=package+'promotetest';test_class={'seed':'Stable310SeedNativeTest','upgrade':'Stable310NativeTest','ui':'Stable310UiNativeTest','final-before':'Stable310FinalNativeTest','final-after':'Stable310FinalNativeTest'}[a.phase]
run=lambda args:subprocess.run(list(map(str,args)),check=True)
shell=lambda *args:subprocess.check_output([str(adb),'-s',serial,'shell',*args],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1' and subprocess.check_output([str(adb),'-s',serial,'emu','avd','name'],text=True).splitlines()[0]=='gpthudstable310'
assert ('versionName=3.0.0' if a.phase=='seed' else 'versionName=3.1.0') in shell('dumpsys','package',package)
manifest=out/'AndroidManifest.xml';manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{test_package}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:label="Stable synthetic QA" android:debuggable="true"/><instrumentation android:name="dev.bennett.codexmeter.{test_class}" android:targetPackage="{package}"/></manifest>',encoding='utf-8')
deps=[];cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
for aar in cache.rglob('*.aar'):
    if not any(str(aar).split(os.sep)[-5].startswith(g) for g in ['androidx.','io.github.oneuiproject']):continue
    with ZipFile(aar) as z:
        if 'classes.jar' in z.namelist():
            path=out/(aar.parent.name+'.jar');path.write_bytes(z.read('classes.jar'));deps.append(path)
deps+=list(cache.glob('androidx.*/*/*/*/*.jar'))
classes=out/'classes';classes.mkdir(exist_ok=True);cp=os.pathsep.join(map(str,[jar,work/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes',work/'app/build/intermediates/compile_r_class_jar/release/generateReleaseRFile/R.jar',work/'shared/build/classes/java/main',*deps]))
args=out/'javac.args';args.write_text('-encoding UTF-8 -source 17 -target 17\n-cp "'+cp.replace('\\','/')+'"\n-d "'+str(classes).replace('\\','/')+'"\n"'+str(root/'tests'/f'{test_class}.java').replace('\\','/')+'"',encoding='utf-8')
run([jdk/'javac.exe','-proc:none','@'+str(args)]);run([bt/'aapt2.exe','link','-I',jar,'--manifest',manifest,'-o',out/'base.apk']);dex=out/'dex';dex.mkdir(exist_ok=True)
run([bt/'d8.bat','--lib',jar,'--min-api','26','--output',dex,*classes.rglob('*.class')])
with ZipFile(out/'base.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
signing=root/'.local-signing';run([bt/'apksigner.bat','sign','--ks',signing/'codex-meter-local.p12','--ks-key-alias','codexmeter','--ks-pass','file:'+str(signing/'password'),'--out',out/'tests.apk',out/'base.apk'])
run([adb,'-s',serial,'install','-r',out/'tests.apk'])
result=shell('am','instrument','-r','-w','-e','phase',a.phase,test_package+'/dev.bennett.codexmeter.'+test_class);(out/'RESULT.txt').write_text(result,encoding='utf-8');print(result);assert 'INSTRUMENTATION_CODE: -1' in result
