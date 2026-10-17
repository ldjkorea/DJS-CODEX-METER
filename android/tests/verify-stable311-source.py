"""Reviewed promotion scope. No runtime I/O, secrets or fixture rewriting."""
from pathlib import Path
import subprocess,json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2];beta='5a2d7b77c23683435f45c1d4f13eb0b35e4f4fe9';stable='afc7d0b9749a42c034e7b7c7305cf8a6cd7fd23a'
def old(ref,path):return subprocess.check_output(['git','show',ref+':'+path],cwd=root).replace(b'\r\n',b'\n')
def actual(path):return (root/path).read_bytes().replace(b'\r\n',b'\n')
app='android/app/src/main/java/dev/bennett/codexmeter/'
for name in ['BetaMedalModel','BetaMedalStore','BetaMedalCoach','BetaMedalUi','BetaV4','BetaGrowthStore','BetaBundle','BetaCompactGrowthUi','BetaGrowthUi','BetaHomeUi','BetaHomeDetailsUi','BetaHomeSummaryUi','BetaQuotaUi','BetaWeeklyUi','BetaWeeklyReader','BetaNextGoal','CoachUi','SpicyQuotes','FunHome','UsageApi','UsageRefreshJobService','RefreshScheduler','OAuthClient','OAuthService','AccountSession','SecureTokenStore','UpdateInstaller','UpdatePreferences','ReleaseUpdateClient','TierTheme','TrialDemo']:
 assert actual(app+name+'.java')==old(beta,app+name+'.java'),name
for path in ['android/shared/src/main/java/dev/bennett/codexmeter/'+n+'.java' for n in ['MxpEconomy','RpgEngine','RpgArchive','TierEvolution','CoachCatalog']]+['android/app/src/main/assets/coach-ko-v2.json','android/app/src/main/assets/coach-ko-medals-v1.json']:
 assert actual(path)==old(beta,path),path
for name in ['GitHubReleaseParser','UpdateChannel']:
 assert actual(app+name+'.java')==old(stable,app+name+'.java'),name
path='android/shared/src/main/java/dev/bennett/codexmeter/KoreanUpdateTrust.java';assert actual(path)==old(stable,path)
assert actual('android/settings.gradle.kts')==old(stable,'android/settings.gradle.kts'),'Stable Wear module remains included'
stable_paths=subprocess.check_output(['git','ls-tree','-r','--name-only',stable],cwd=root,text=True).splitlines()
beta_paths=set(subprocess.check_output(['git','ls-tree','-r','--name-only',beta],cwd=root,text=True).splitlines())
for path in stable_paths:
 if path.startswith('windows-widget/') or path=='android/wear/build.gradle.kts':assert actual(path)==old(stable,path),path
 # All stable-only regression sources survive promotion.
 if path.startswith('android/tests/') and path not in beta_paths:assert actual(path)==old(stable,path),path
for path in ['android/app/build.gradle.kts',app+'AppConstants.java','android/build.sh','android/run-tests.sh']:assert '3.1.1' in actual(path).decode(),path
assert 'versionCode = 88' in actual('android/app/build.gradle.kts').decode();constants=actual(app+'AppConstants.java').decode();assert 'VERSION_CODE = 88' in constants and 'codexmeter://auth/complete' in constants
assert 'dev.bennett.codexmeter.trial.' not in constants and 'codexmetertrial' not in constants
ns='{http://schemas.android.com/apk/res/android}';manifest=ET.fromstring(actual('android/app/src/main/AndroidManifest.xml'));prior=ET.fromstring(old(stable,'android/app/src/main/AndroidManifest.xml'))
now_app=manifest.find('application');old_app=prior.find('application');assert now_app.attrib==old_app.attrib,'Stable app label/icon/backup/locale contract'
components={node.get(ns+'name'):node for node in now_app if node.get(ns+'name')}
def tree(node):return (node.tag,tuple(sorted(node.attrib.items())),tuple(sorted(tree(child) for child in node)))
for node in old_app:
 name=node.get(ns+'name')
 if name:assert name in components,(name,'missing stable component');assert tree(components[name])==tree(node),(name,'stable component identity or intent changed')
assert sorted(tree(n) for n in manifest if n.tag!='application')==sorted(tree(n) for n in prior if n.tag!='application'),'Stable permissions and SDK contract'
launcher=[]
for node in now_app.findall('activity'):
 if any(c.get(ns+'name')=='android.intent.category.LAUNCHER' for f in node.findall('intent-filter') for c in f.findall('category')):launcher.append(node.get(ns+'name'))
assert launcher==['dev.bennett.codexmeter.MainActivity'];assert components['dev.bennett.codexmeter.TrialWelcomeActivity'].get(ns+'exported')=='false'
assert 'android:scheme="codexmeter"' in actual('android/app/src/main/AndroidManifest.xml').decode()
assert '"codexmetertrial"' not in actual(app+'MainActivity.java').decode()
for name in ['BetaSettingsUi','BetaSetupActivity','BetaDiagnostics','BetaProfileActivity','UpdateActivity']:assert 'GPT HUD BETA' not in actual(app+name+'.java').decode(),name
original=json.loads(actual('android/app/src/main/assets/coach-ko-v2.json'));extra=json.loads(actual('android/app/src/main/assets/coach-ko-medals-v1.json'));assert len(original['quotes'])==600 and len(extra['quotes'])==24 and len(original['tips'])==40
print('Stable311 scope: beta9911 engines/auth/content preserved; stable package/update/components; Wear/Windows/stable-only fixtures retained;624 quotes40 tips')
