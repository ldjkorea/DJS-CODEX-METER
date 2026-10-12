from pathlib import Path
import os,subprocess
root=Path(__file__).resolve().parents[1];out=root/'build/rpg-step2';out.mkdir(parents=True,exist_ok=True)
shared=root/'shared/src/main/java/dev/bennett/codexmeter';jdk=Path(os.environ['JAVA_HOME'])/'bin'
cp=os.pathsep.join(map(str,[out,root/'build/rpg-step1',root/'build/insight-verification/classes',root/'build/subscription-store-verification/classes',root/'build/evolution-verification',root/'build/test-libs/json-20250517.jar']))
files=[shared/(n+'.java') for n in ['AchievementJourney','TitleJourney','RpgTokens','RpgTier','RpgCatalog','RpgTitleCatalog','RpgGrowthFacts','RpgEngine','MonthlyLedger','AccountTier','AccountProgress']]+[Path(__file__).with_name('RpgStep2SelfTest.java')]
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(out),*map(str,files)],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.RpgStep2SelfTest'],check=True)
