from pathlib import Path
import os,subprocess,json,re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];app=root/'app/src/main/java/dev/bennett/codexmeter';out=root/'build/rpg3-tests';out.mkdir(parents=True,exist_ok=True)
cp=os.pathsep.join(map(str,[out,root/'build/rpg-step2',root/'build/rpg-step1',root/'build/insight-verification/classes',root/'build/evolution-verification',root/'build/subscription-store-verification/classes',root/'build/tests',root/'build/test-libs/json-20250517.jar']))
jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',out,Path(__file__).with_name('RpgStep3SelfTest.java'),Path(__file__).with_name('RpgReleaseSelfTest.java')],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.RpgStep3SelfTest'],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.RpgReleaseSelfTest'],check=True)
nav=(app/'MatteNav.java').read_text(encoding='utf-8');assert 'int[] roots={2,1,3}' in nav and 'GrowthActivity.class' in nav
store=(app/'RpgProgressStore.java').read_text(encoding='utf-8');assert 'r.optJSONObject("KR")' in store
growth=(app/'GrowthActivity.java').read_text(encoding='utf-8');assert 'WORKER.execute' in growth and 'account.equals(SubscriptionStore.accountKey(this))' in growth
render=(app/'RpgGrowthUi.java').read_text(encoding='utf-8');assert 'RpgEngine.reconcile' not in render and 'RpgTier.current' not in render
for needed in ['RpgEngine.threshold','RpgPlanSeals.image','rpg3_token_note','rpg3_local_scope']:assert needed in render
collection=(app/'RpgCollectionActivity.java').read_text(encoding='utf-8');detail=(app/'RpgMedalUi.java').read_text(encoding='utf-8');assert 'RpgCollectionFacts.select' in collection and 'RpgCollectionFacts.time' in detail
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot();names={a.get('{http://schemas.android.com/apk/res/android}name'):a for a in manifest.find('application').findall('activity')}
for name in ['GrowthActivity','BillingHistoryActivity']:assert names['dev.bennett.codexmeter.'+name].get('{http://schemas.android.com/apk/res/android}exported')=='false'
en={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/rpg3_strings.xml').getroot()};ko={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/rpg3_strings.xml').getroot()};assert en.keys()==ko.keys() and all(ko.values())
for key in en:assert sorted(re.findall(r'%\d+\$[sd]',en[key]))==sorted(re.findall(r'%\d+\$[sd]',ko[key])),key
history=json.loads((root/'app/src/main/assets/release-history.json').read_text(encoding='utf-8'));assert len(history)==53 and history[0]['version']=='2.8.25'
assets=json.loads((root.parent/'docs/RPG_PLAN_SEALS.json').read_text(encoding='utf-8'))['assets'];assert len(assets)==6
for asset in assets:assert asset['license']=='MIT' and ET.parse(root.parent/asset['path']).getroot().tag=='vector'
assert 'Tier=mirror.Tier' in (root.parent/'windows-widget/src/Widget.cs').read_text(encoding='utf-8')
assert 'if(stamp.equals(todayStamp))' in (app/'LedgerDashboard.java').read_text(encoding='utf-8')
print('RPG growth/navigation/bilingual resources/vector seals/common Korea/reset-safe Windows source checks passed. Native rendering is separate.')
