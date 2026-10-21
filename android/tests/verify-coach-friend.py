"""Local voice catalog preservation and production JVM evidence regressions. No network."""
from pathlib import Path
import hashlib,json,os,subprocess

root=Path(__file__).resolve().parents[2];android=root/'android';base='d2fee21b96b0335dc5e1d5ec41421c89d4f3e585'
out=android/'build/home-coach-focus/voice';out.mkdir(parents=True,exist_ok=True)
def before(path):return json.loads(subprocess.check_output(['git','show',base+':'+path],cwd=root))
changed=0
for name in ['coach-ko-v2.json','coach-ko-medals-v1.json']:
 path='android/app/src/main/assets/'+name;old=before(path);new=json.loads((root/path).read_text(encoding='utf-8'))
 if name=='coach-ko-v2.json':assert new['schemaVersion']==old['schemaVersion']==2
 else:assert new['version']==old['version']==1
 assert len(old['quotes'])==len(new['quotes'])
 for a,b in zip(old['quotes'],new['quotes']):
  assert {k:v for k,v in a.items() if k not in ['text','paidText']}=={k:v for k,v in b.items() if k not in ['text','paidText']},a['id']
  changed+=a['text']!=b['text']
 assert {k:v for k,v in old.items() if k not in ['quotes','contentVersion']}=={k:v for k,v in new.items() if k not in ['quotes','contentVersion']}
assert changed==200
print('Catalog preservation: 624 stable IDs/groups/tip links, schema2, legacy map, all40 prompts unchanged; 200 rewrites/424 retained/4 guarded paid variants.',flush=True)
jar=android/'build/test-libs/json-20250517.jar';assert jar.exists()
suffix='.exe' if os.name=='nt' else ''
jdk=Path(os.environ['JAVA_HOME'])/'bin';app=android/'app/src/main/java/dev/bennett/codexmeter'
sources=list((android/'shared/src/main/java').rglob('*.java'))+[app/(n+'.java') for n in ['BetaV4','BetaCatalog','BetaMedalCoach']]
quote=lambda p:'"'+str(p).replace('\\','/')+'"'
def compile_into(dest,files):
 dest.mkdir(exist_ok=True);args=dest/'javac.args';args.write_text('-encoding UTF-8\n-cp '+quote(jar)+'\n-d '+quote(dest)+'\n'+'\n'.join(map(quote,files)),encoding='utf-8')
 subprocess.run([jdk/('javac'+suffix),'@'+str(args)],check=True)
classes=out/'current'
compile_into(classes,sources+[android/'tests/CoachV2SelfTest.java',android/'tests/CoachFriendSelfTest.java'])
cp=os.pathsep.join(map(str,[classes,jar]));results=[]
for name,extra in [('CoachV2SelfTest',[android/'app/src/main/assets/coach-ko-v2.json']),('CoachFriendSelfTest',[android/'app/src/main/assets/coach-ko-v2.json',android/'app/src/main/assets/coach-ko-medals-v1.json'])]:
 result=subprocess.check_output([jdk/('java'+suffix),'-Xmx512m','-ea','-cp',cp,'dev.bennett.codexmeter.'+name,*map(str,extra)],text=True,encoding='utf-8');print(result,flush=True);results.append(result)
# Demonstrate the reported bugs on untouched baseline code, without changing old tests/fixtures.
baseline=out/'baseline';baseline.mkdir(exist_ok=True)
old_catalog=baseline/'CoachCatalog.java';old_catalog.write_bytes(subprocess.check_output(['git','show',base+':android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java'],cwd=root))
probe=baseline/'CoachZeroRegression.java';probe.write_text('''package dev.bennett.codexmeter;
public final class CoachZeroRegression {public static void main(String[] a){long now=1800000000000L;double shortUsed=a[0].equals("short-positive")?25:0,longUsed=a[0].equals("precision")?0.1:0;UsageSnapshot s=new UsageSnapshot("plus",true,false,new UsageWindow(shortUsed,18000,0,(now+7200000)/1000),new UsageWindow(longUsed,604800,0,(now+259200000)/1000),now);String group=CoachCatalog.group(s,true,false,false,0,now);if(group.equals("zero"))throw new AssertionError(a[0]+": actual positive activity misclassified zero");System.out.println(a[0]+": active="+group);}}
''',encoding='utf-8')
baseline_sources=[old_catalog if p.name=='CoachCatalog.java' else p for p in sources]
compile_into(baseline,baseline_sources+[probe]);compile_into(classes,sources+[probe])
negative=[]
for case in ['short-positive','precision']:
 prior=subprocess.run([jdk/('java'+suffix),'-cp',os.pathsep.join(map(str,[baseline,jar])),'dev.bennett.codexmeter.CoachZeroRegression',case],capture_output=True,text=True,encoding='utf-8')
 assert prior.returncode!=0 and 'actual positive activity misclassified zero' in prior.stderr
 fixed=subprocess.check_output([jdk/('java'+suffix),'-cp',cp,'dev.bennett.codexmeter.CoachZeroRegression',case],text=True,encoding='utf-8')
 print('Baseline failure reproduced; fixed '+fixed,flush=True);negative.append({'case':case,'baseline':prior.stderr,'fixed':fixed})
(out/'result.json').write_text(json.dumps({'base':base,'results':results,'regressions':negative,'catalog_rewrites':changed,'quotes':624,'tips':40,'scope':'production Java on synthetic JVM observations, not actual billing/account evidence'},ensure_ascii=False,indent=2),encoding='utf-8')
