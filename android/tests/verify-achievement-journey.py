from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];out=root/'build/achievement-tests';out.mkdir(parents=True,exist_ok=True);jdk=Path(os.environ['JAVA_HOME'])/'bin';cp=os.pathsep.join(map(str,[out,root/'build/rpg-step1',root/'build/insight-verification/classes',root/'build/evolution-verification',root/'build/subscription-store-verification/classes',root/'build/test-libs/json-20250517.jar']))
subprocess.run([jdk/'javac','-encoding','UTF-8','-cp',cp,'-d',out,Path(__file__).with_name('AchievementJourneySelfTest.java')],check=True)
subprocess.run([jdk/'java','-ea','-cp',cp,'dev.bennett.codexmeter.AchievementJourneySelfTest'],check=True)
a={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/journey_strings.xml').getroot()};b={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/journey_strings.xml').getroot()};assert a.keys()==b.keys() and all(a.values()) and all(b.values());print('Bilingual opt-in resources passed; native UI is separate.')
