"""Narrow release discovery tests; does not install an app or certify the full legacy suite."""
from pathlib import Path
import os,subprocess,sys
r=Path(__file__).resolve().parents[1];out=r/'build/stable313/release-tests';out.mkdir(parents=True,exist_ok=True)
app=r/'app/src/main/java/dev/bennett/codexmeter';shared=r/'shared/src/main/java/dev/bennett/codexmeter'
jar=r/'build/test-libs/json-20250517.jar'
if not jar.exists():
 import urllib.request
 jar.parent.mkdir(parents=True,exist_ok=True);urllib.request.urlretrieve('https://repo1.maven.org/maven2/org/json/json/20250517/json-20250517.jar',jar)
files=[app/(n+'.java') for n in ['GitHubReleaseParser','GitHubRelease','GitHubReleaseSource','UpdateChannel','ReleaseVersion']]+[shared/'KoreanUpdateTrust.java',Path(__file__).with_name('Stable313ReleaseSelfTest.java')]
jdk=Path(os.environ['JAVA_HOME'])/'bin';suffix='.exe' if os.name=='nt' else ''
subprocess.run([jdk/('javac'+suffix),'-encoding','UTF-8','-cp',jar,'-d',out,*files],check=True)
subprocess.run([jdk/('java'+suffix),'-ea','-cp',os.pathsep.join(map(str,[out,jar])),'dev.bennett.codexmeter.Stable313ReleaseSelfTest',*sys.argv[1:]],check=True)
