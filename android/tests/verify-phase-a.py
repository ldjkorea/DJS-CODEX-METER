"""Phase A targeted data regressions against real shared implementations."""
from pathlib import Path
import os, subprocess
root=Path(__file__).resolve().parents[1];out=root/'build/phase-a-tests';out.mkdir(exist_ok=True)
cp=os.pathsep.join(map(str,[out,root/'build/rpg-step2',root/'build/rpg-step1',root/'build/tests',root/'build/subscription-store-verification/classes',root/'build/evolution-verification',root/'build/insight-verification/classes',root/'build/test-libs/json-20250517.jar']))
shared=root/'shared/src/main/java/dev/bennett/codexmeter';jdk=Path(os.environ['JAVA_HOME'])/'bin'
files=[shared/(n+'.java') for n in ['UsagePrecision','UsageEvidence','BillingEvidence','AccountProgress']]+[Path(__file__).with_name('PhaseASelfTest.java')]
subprocess.run([jdk/'javac','-encoding','UTF-8','-cp',cp,'-d',out,*files],check=True)
subprocess.run([jdk/'java','-ea','-Duser.language=en','-cp',cp,'dev.bennett.codexmeter.PhaseASelfTest'],check=True)
repo=root.parent
for relative in ['android/shared/src/main/java/dev/bennett/codexmeter/MonthlyLedger.java','android/app/src/main/java/dev/bennett/codexmeter/SubscriptionStore.java','android/app/src/main/java/dev/bennett/codexmeter/UsageLedgerDatabase.java','android/app/src/main/java/dev/bennett/codexmeter/AccountSession.java','android/app/src/main/java/dev/bennett/codexmeter/AppPreferences.java']:
    expected=subprocess.check_output(['git','show','43ed903:'+relative],cwd=repo).replace(b'\r\n',b'\n')
    from phase_b_review import strip_phase_b
    from collection_restart_review import strip_collection_restart
    current=strip_phase_b(relative,strip_collection_restart(relative,(repo/relative).read_text(encoding='utf-8'))).encode('utf-8')
    assert current==expected,relative
print('Monthly formula, SQLite schema, payment preferences and account/settings storage protected; exact Phase B clock-skew DAO patch reviewed separately.')
