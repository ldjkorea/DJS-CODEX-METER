from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET,re
root=Path(__file__).resolve().parents[1];repo=root.parent;out=root/'build/medal-tests';out.mkdir(parents=True,exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin';shared=root/'shared/src/main/java/dev/bennett/codexmeter';cp=os.pathsep.join(map(str,[out,root/'build/growth-ux-tests',root/'build/rpg-step2',root/'build/rpg-step1',root/'build/test-libs/json-20250517.jar']))
subprocess.run([jdk/'javac','-encoding','UTF-8','-cp',cp,'-d',out,shared/'RpgCollectionFacts.java',Path(__file__).with_name('RpgCollectionSelfTest.java')],check=True)
subprocess.run([jdk/'java','-ea','-cp',cp,'dev.bennett.codexmeter.RpgCollectionSelfTest'],check=True)
for name in ['android/shared/src/main/java/dev/bennett/codexmeter/'+n+'.java' for n in ['RpgCatalog','RpgEngine','AccountProgress','MonthlyLedger','RpgTier','RpgTokens']]+['android/app/src/main/java/dev/bennett/codexmeter/'+n+'.java' for n in ['RpgProgressStore','RpgShowcase','SubscriptionStore','UsageLedgerDatabase','UsageApi','OAuthClient','WidgetRenderer']]:
    assert (repo/name).read_bytes().replace(b'\r\n',b'\n')==subprocess.check_output(['git','show','0c2dace:'+name],cwd=repo).replace(b'\r\n',b'\n'),name
en={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/medal_strings.xml').getroot()};ko={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/medal_strings.xml').getroot()};assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
for key in en:assert sorted(re.findall(r'%\d+\$[sd]',en[key]))==sorted(re.findall(r'%\d+\$[sd]',ko[key])),key
app=root/'app/src/main/java/dev/bennett/codexmeter';collection=(app/'RpgCollectionActivity.java').read_text(encoding='utf-8')
for guard in ['setHasStableIds(true)','DiffUtil.calculateDiff','GridLayoutManager','onSaveInstanceState','RpgShowcase.save','RpgProgressStore.equip','RpgCollectionFacts.recommend']:assert guard in collection
assert 'RpgEngine.reconcile' not in collection and 'RpgProgressStore.recommend' not in collection
assert 'setMultiChoiceItems' not in (app/'GrowthActivity.java').read_text(encoding='utf-8')
print(f'53 unchanged catalog definitions and 13 unchanged engine/data/auth/widget files; {len(en)} bilingual resource keys; recycled UI contracts passed. Native render is separate.')
