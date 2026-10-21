"""JVM tests of exact extracted production redirect policies, not Android installation."""
from pathlib import Path
import os
import shutil
import subprocess

root = Path(__file__).resolve().parents[2]
app = root / 'android/app/src/main/java/dev/bennett/codexmeter'
out = root / 'android/build/update-download-policy'
out.mkdir(parents=True, exist_ok=True)
installer = (app / 'UpdateInstaller.java').read_text(encoding='utf-8')
client = (app / 'ReleaseUpdateClient.java').read_text(encoding='utf-8')
trust = root / 'android/shared/src/main/java/dev/bennett/codexmeter/KoreanUpdateTrust.java'
# Exact published transport/trust code: this follow-up must not enlarge any allowlist.
for file in [app / 'UpdateInstaller.java', app / 'ReleaseUpdateClient.java', trust]:
    old = subprocess.check_output(['git', 'show', '6638a50:' + file.relative_to(root).as_posix()], cwd=root).replace(b'\r\n', b'\n')
    assert file.read_bytes().replace(b'\r\n', b'\n') == old, ('Transport/trust changed', file.name)
download = installer[installer.index('    private static int requireOk('):installer.index('    private static void copy(')]
api = client[client.index('    private static boolean trustedApiUrl('):client.index('    private static String read(')]
source = '''package dev.bennett.codexmeter;
import java.net.*;import java.util.*;
public class DownloadPolicySelfTest {
static int n;static void check(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
static class BuildConfig{static final boolean DEBUG=false;}
static class Fake extends HttpURLConnection {
final int response;Fake(String url,int status)throws Exception{super(new URL(url));response=status;}
public int getResponseCode(){return response;}public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}}
''' + download + api + '''
public static void main(String[] args)throws Exception {
String old="https://github.com/ldjkorea/codex-meter-ko/releases/download/v3.1.1/CodexMeter-3.1.1-ko.apk";
String renamed=old.replace("codex-meter-ko","DJS-CODEX-METER");
check(KoreanUpdateTrust.official(old),"Original metadata path trusted");
check(!KoreanUpdateTrust.official(renamed),"Renamed metadata path remains untrusted");
for(String url:new String[]{old,renamed,"https://release-assets.githubusercontent.com/synthetic/asset?synthetic=1"})
 check(trustedDownloadUrl(new URL(url)),"Existing downloader host policy "+new URL(url).getHost());
for(String url:new String[]{"https://github.com.evil.example/asset","http://github.com/asset","https://evil.example/github.com/asset"})
 check(!trustedDownloadUrl(new URL(url)),"Host/protocol rejected");
check(requireOk(new Fake("https://release-assets.githubusercontent.com/synthetic",200))==200,"Allowed final asset host");
boolean denied=false;try{requireOk(new Fake("https://evil.example/asset",200));}catch(SecurityException expected){denied=true;}
check(denied,"Final redirect host enforced");
denied=false;try{requireOk(new Fake(old,403));}catch(IllegalStateException expected){denied=true;}
check(denied,"HTTP failure rejected");
check(trustedApiUrl(new URL("https://api.github.com/repositories/1411531926/releases?per_page=30"),false),"Canonical ID API redirect allowed");
check(!trustedApiUrl(new URL("https://api.github.com.evil.example/repos/a"),false),"API lookalike denied");
check(!trustedApiUrl(new URL("http://api.github.com/repos/a"),false),"Insecure API denied");
System.out.println("Update redirect policy: "+n+" checks passed; exact extracted methods, no sockets/APK/install.");}}
'''
test = out / 'DownloadPolicySelfTest.java'
test.write_text(source, encoding='utf-8')
java = Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else Path(shutil.which('java')).parent
suffix = '.exe' if os.name == 'nt' else ''
subprocess.run([str(java / ('javac' + suffix)), '-encoding', 'UTF-8', '-d', str(out), str(test), str(trust)], check=True)
subprocess.run([str(java / ('java' + suffix)), '-Xmx64m', '-ea', '-cp', str(out), 'dev.bennett.codexmeter.DownloadPolicySelfTest'], check=True)
