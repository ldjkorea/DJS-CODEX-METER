"""Actual beta JVM contracts; the normal release fixtures remain unchanged."""
from pathlib import Path
import os, shutil, subprocess, urllib.request

root = Path(__file__).resolve().parents[1]
out = root / 'build/beta-contracts'
out.mkdir(parents=True, exist_ok=True)
jar = root / 'build/test-libs/json-20250517.jar'
if not jar.exists():
    jar.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve('https://repo1.maven.org/maven2/org/json/json/20250517/json-20250517.jar', jar)
java = Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else Path(shutil.which('java')).parent
suffix = '.exe' if os.name == 'nt' else ''
sources = list((root / 'shared/src/main/java').rglob('*.java'))
app = root / 'app/src/main/java/dev/bennett/codexmeter'
sources += [app / (name + '.java') for name in ['GitHubReleaseParser', 'GitHubRelease', 'GitHubReleaseSource', 'UpdateChannel', 'ReleaseVersion', 'BetaGrowthModel','BetaV4','BetaCatalog','BetaLegacy','OAuthAttempt','BetaNextGoal','BetaExperience']]
sources += [root / 'trial/Beta998SelfTest.java', root / 'trial/Beta997LifecycleSelfTest.java', root / 'trial/Beta996SelfTest.java', root / 'trial/BetaReleaseSelfTest.java', root / 'tests/MxpEconomySelfTest.java', root / 'tests/CoachV2SelfTest.java']
args = out / 'javac.args'
quoted = lambda p: '"' + str(p).replace('\\', '/') + '"'
args.write_text('-encoding UTF-8\n-cp ' + quoted(jar) + '\n-d ' + quoted(out) + '\n' + '\n'.join(map(quoted, sources)), encoding='utf-8')
subprocess.run([str(java / ('javac' + suffix)), '@' + str(args)], check=True)
cp = os.pathsep.join(map(str, [out, jar]))
for name, extra in [('Beta998SelfTest', [str(root/'app/src/main/assets/coach-ko-v2.json')]), ('Beta997LifecycleSelfTest', []), ('Beta996SelfTest', [str(root/'trial/legacy995-30.json'),str(root/'trial/legacy995-400.json')]), ('BetaReleaseSelfTest', ['beta']), ('MxpEconomySelfTest', []), ('CoachV2SelfTest', [str(root / 'app/src/main/assets/coach-ko-v2.json')])]:
    subprocess.run([str(java / ('java' + suffix)), '-Xmx768m', '-ea', '-cp', cp, 'dev.bennett.codexmeter.' + name, *extra], check=True)
