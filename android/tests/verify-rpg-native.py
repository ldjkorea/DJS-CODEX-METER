"""Opt-in standalone test APK on an emulator. Does not install/update/open GPT HUD.
Uses production accounting/DAO with synthetic auth/ledger input; no network permission.
"""
from pathlib import Path
from zipfile import ZipFile
import os,re,subprocess,sys
root=Path(__file__).resolve().parents[1];serial=sys.argv[1];assert serial.startswith('emulator-')
sdk=Path(os.environ['ANDROID_HOME']);jdk=Path(os.environ['JAVA_HOME'])/'bin';bt=sdk/'build-tools/36.0.0';jar=sdk/'platforms/android-36/android.jar';adb=sdk/'platform-tools/adb.exe'
out=root/'build/rpg-native';out.mkdir(parents=True,exist_ok=True);package='dev.bennett.codexmeter.rpgstep1test'
if len(sys.argv)>2:
    assert re.fullmatch('[a-z][a-z0-9]*',sys.argv[2]), 'Only a safe isolated run suffix is permitted'
    package+='.'+sys.argv[2]
shell=lambda *args:subprocess.check_output([str(adb),'-s',serial,'shell',*args],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1'
assert 'package:'+package not in shell('pm','list','packages',package).splitlines(),'Preserve existing fixture; choose a fresh emulator instead'
run=lambda args:subprocess.run(list(map(str,args)),check=True)
manifest=out/'AndroidManifest.xml';manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="36"/><application android:label="RPG isolated QA" android:debuggable="true"/><instrumentation android:name="dev.bennett.codexmeter.RpgNativeTest" android:targetPackage="{package}"/></manifest>',encoding='utf-8')
fixtures=root/'build/rpg-store/fixtures';files=[fixtures/'dev/bennett/codexmeter'/name for name in ['SecureTokenStore.java','AppPreferences.java','UsageApi.java','UsageLedgerDatabase.java']]
app=root/'app/src/main/java/dev/bennett/codexmeter';shared=root/'shared/src/main/java/dev/bennett/codexmeter'
files += [app/(n+'.java') for n in ['AuthTokens','SubscriptionStore','RpgProgressStore']]
files += [shared/(n+'.java') for n in ['MonthlyLedger','AccountTier','AccountProgress','FunInsights','TierEvolution','SubscriptionCost','SubscriptionValue','LedgerPeriods','LedgerAggregation','LedgerRecord','UsageWindow','UsageSnapshot','UsageLimit','UsageCredits','UsageSample','UsageInsights','UsageHistory']]
files += [Path(__file__).with_name(n) for n in ['RpgNativeTest.java','RpgStep1SelfTest.java']]
classes=out/'classes';classes.mkdir(exist_ok=True);run([jdk/'javac.exe','-encoding','UTF-8','-source','17','-target','17','-cp',jar,'-d',classes,*files])
run([bt/'aapt2.exe','link','-I',jar,'--manifest',manifest,'-o',out/'base.apk'])
with ZipFile(out/'compiled.jar','w') as z:
    for path in classes.rglob('*.class'):z.write(path,path.relative_to(classes).as_posix())
dex=out/'dex';dex.mkdir(exist_ok=True);run([bt/'d8.bat','--lib',jar,'--min-api','26','--output',dex,out/'compiled.jar'])
with ZipFile(out/'base.apk','a') as z:z.write(dex/'classes.dex','classes.dex')
signing=root/'.local-signing';run([bt/'apksigner.bat','sign','--ks',signing/'codex-meter-local.p12','--ks-key-alias','codexmeter','--ks-pass','file:'+str(signing/'password'),'--out',out/'isolated-tests.apk',out/'base.apk'])
run([adb,'-s',serial,'install',out/'isolated-tests.apk'])
result=shell('am','instrument','-r','-w',package+'/dev.bennett.codexmeter.RpgNativeTest');(out/'RESULT.txt').write_text(result,encoding='utf-8');print(result)
assert 'INSTRUMENTATION_CODE: -1' in result and 'Native RPG:' in result
