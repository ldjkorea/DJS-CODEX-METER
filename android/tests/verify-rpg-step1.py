from pathlib import Path
import os,subprocess
root=Path(__file__).resolve().parents[1];out=root/'build/rpg-step1';out.mkdir(parents=True,exist_ok=True)
shared=root/'shared/src/main/java/dev/bennett/codexmeter';jar=root/'build/test-libs/json-20250517.jar';jdk=Path(os.environ['JAVA_HOME'])/'bin'
cp=os.pathsep.join(map(str,[out,root/'build/insight-verification/classes',root/'build/subscription-store-verification/classes',root/'build/evolution-verification',jar]))
files=[shared/(n+'.java') for n in ['MonthlyLedger','AccountTier','AccountProgress']]+[Path(__file__).with_name('RpgStep1SelfTest.java')]
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(out),*map(str,files)],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.RpgStep1SelfTest'],check=True)
