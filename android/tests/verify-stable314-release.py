"""Focused foreground policy, extracted cache/storage methods and release parser tests."""
from pathlib import Path
import os
import subprocess
import sys
import urllib.request

root = Path(__file__).resolve().parents[2]
android = root / 'android'
out = android / 'build/stable314/release-tests'
out.mkdir(parents=True, exist_ok=True)
app = android / 'app/src/main/java/dev/bennett/codexmeter'
shared = android / 'shared/src/main/java/dev/bennett/codexmeter'
jar = android / 'build/test-libs/json-20250517.jar'
if not jar.exists():
    jar.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve('https://repo1.maven.org/maven2/org/json/json/20250517/json-20250517.jar', jar)
jdk = Path(os.environ['JAVA_HOME']) / 'bin'
suffix = '.exe' if os.name == 'nt' else ''
client = (app / 'ReleaseUpdateClient.java').read_text('utf-8')
prefs = (app / 'UpdatePreferences.java').read_text('utf-8')
cache_method = client[client.index('    public static List<GitHubRelease> checkIfStale('):client.index('    public static List<GitHubRelease> check(')]
storage_methods = prefs[prefs.index('    public static boolean foregroundVersionShown('):prefs.index('    public static String etag(')]
probe = out / 'ForegroundUpdateMethodsProbe.java'
probe.write_text('''package dev.bennett.codexmeter;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class ForegroundUpdateMethodsProbe {
 static final Object LOCK=new Object();static final String KEY_FOREGROUND_SHOWN="foreground_shown_versions";
 static int n;static void check(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
 static class Context {final FakePrefs disk;Context(FakePrefs d){disk=d;}}
 static class FakePrefs {
  final Map<String,Set<String>> data=new HashMap<>();
  Set<String> getStringSet(String k,Set<String> fallback){return data.getOrDefault(k,fallback);}
  Editor edit(){return new Editor();}
  class Editor {String key;Set<String> value;Editor putStringSet(String k,Set<String> v){key=k;value=new HashSet<>(v);return this;}void apply(){data.put(key,value);}}
 }
 static FakePrefs prefs(Context c){return c.disk;}
 static class GitHubRelease {}
 static class UpdatePreferences {
  static long at;static long lastCheckMillis(Context c){return at;}
  static List<GitHubRelease> releases(Context c){return Collections.emptyList();}
 }
 static final AtomicInteger calls=new AtomicInteger();static boolean fail;
 static List<GitHubRelease> check(Context c)throws Exception {
  calls.incrementAndGet();Thread.sleep(10);UpdatePreferences.at=System.currentTimeMillis();
  if(fail)throw new Exception("synthetic lookup failure");return Collections.emptyList();
 }
''' + cache_method + storage_methods + '''
 public static void main(String[] args)throws Exception {
  FakePrefs disk=new FakePrefs();Context c=new Context(disk);
  check(!foregroundVersionShown(c,"3.1.5"),"Unseen version");
  markForegroundVersionShown(c,"3.1.5");check(foregroundVersionShown(c,"3.1.5"),"Recorded after display");
  Set<String> original=disk.getStringSet(KEY_FOREGROUND_SHOWN,Collections.emptySet());
  markForegroundVersionShown(c,"3.1.6");check(original.size()==1,"Do not mutate SharedPreferences set in place");
  c=new Context(disk);check(foregroundVersionShown(c,"3.1.5")&&foregroundVersionShown(c,"3.1.6"),"Recreated context keeps both versions");
  markForegroundVersionShown(c,"bad version");markForegroundVersionShown(c,null);
  check(disk.getStringSet(KEY_FOREGROUND_SHOWN,Collections.emptySet()).size()==2,"Invalid versions do not corrupt storage");
  ExecutorService workers=Executors.newFixedThreadPool(16);final Context ctx=c;
  try {
   List<Future<?>> jobs=new ArrayList<>();CountDownLatch start=new CountDownLatch(1);
   for(int i=0;i<64;i++)jobs.add(workers.submit(()->{try{start.await();checkIfStale(ctx);}catch(Exception e){throw new RuntimeException(e);}}));
   start.countDown();for(Future<?> job:jobs)job.get();check(calls.get()==1,"Concurrent foreground/background cache methods make one lookup");
   UpdatePreferences.at=System.currentTimeMillis()-ForegroundUpdatePolicy.CACHE_MILLIS-1;checkIfStale(c);check(calls.get()==2,"Expired cache queries again");
   UpdatePreferences.at=0;fail=true;boolean failed=false;try{checkIfStale(c);}catch(Exception e){failed=true;}
   check(failed&&calls.get()==3,"Failed query propagates for silent foreground handling");
   for(int i=0;i<64;i++)checkIfStale(c);check(calls.get()==3,"Failure timestamp prevents immediate retry burst");
   UpdatePreferences.at=System.currentTimeMillis()+1000;fail=false;checkIfStale(c);check(calls.get()==4,"Future wall timestamp can recover");
  }finally{workers.shutdownNow();}
  System.out.println("Foreground extracted methods: "+n+" checks passed; synthetic cache/storage adapters, not Android persistence/UI proof.");
 }
}
''', encoding='utf-8')
files = [app / (n + '.java') for n in ['GitHubReleaseParser', 'GitHubRelease', 'GitHubReleaseSource', 'UpdateChannel', 'ReleaseVersion']]
files += [shared / 'KoreanUpdateTrust.java', shared / 'ForegroundUpdatePolicy.java',
          Path(__file__).with_name('Stable314ReleaseSelfTest.java'),
          Path(__file__).with_name('ForegroundUpdatePolicySelfTest.java'), probe]
quote = lambda p: '"' + str(p).replace('\\', '/') + '"'
argfile = out / 'javac.args'
argfile.write_text('-encoding UTF-8\n-cp ' + quote(jar) + '\n-d ' + quote(out) + '\n' + '\n'.join(map(quote, files)), 'utf-8')
subprocess.run([jdk / ('javac' + suffix), '@' + str(argfile)], check=True)
for name, args in [('ForegroundUpdatePolicySelfTest', []), ('ForegroundUpdateMethodsProbe', []), ('Stable314ReleaseSelfTest', sys.argv[1:])]:
    subprocess.run([jdk / ('java' + suffix), '-ea', '-cp', os.pathsep.join(map(str, [out, jar])),
                    'dev.bennett.codexmeter.' + name, *args], check=True)
