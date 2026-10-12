from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];out=root/'build/achievement-tests';out.mkdir(parents=True,exist_ok=True);jdk=Path(os.environ['JAVA_HOME'])/'bin';cp=os.pathsep.join(map(str,[out,root/'build/rpg-step1',root/'build/insight-verification/classes',root/'build/evolution-verification',root/'build/subscription-store-verification/classes',root/'build/test-libs/json-20250517.jar']))
subprocess.run([jdk/'javac','-encoding','UTF-8','-cp',cp,'-d',out,Path(__file__).with_name('AchievementJourneySelfTest.java'),Path(__file__).with_name('GrowthRebornSelfTest.java')],check=True)
subprocess.run([jdk/'java','-ea','-cp',cp,'dev.bennett.codexmeter.AchievementJourneySelfTest'],check=True)
a={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/journey_strings.xml').getroot()};b={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/journey_strings.xml').getroot()};assert a.keys()==b.keys() and all(a.values()) and all(b.values());print('Bilingual opt-in resources passed; native UI is separate.')

subprocess.run([jdk/'java','-ea','-cp',cp,'dev.bennett.codexmeter.GrowthRebornSelfTest'],check=True)

for file in ['reborn_strings.xml']:
 en={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values'/file).getroot()};ko={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko'/file).getroot()};assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
import json
plans=json.loads((root/'app/src/main/assets/plan-comparison-2026-10.json').read_text(encoding='utf-8'));assert plans['verified']=='2026-10-12' and plans['currency']=='USD';assert [p['monthly'] for p in plans['plans']]==[0,8,20,100,200,500];assert all(p['source'].startswith(('https://help.openai.com/','https://openai.com/','https://chatgpt.com/')) and p['ko'] and p['en'] for p in plans['plans']);print('Bilingual Growth Reborn resources and dated official offline plan comparison passed.')
