"""Freeze ALL existing production files, allowing only the reviewed migration delta.

This is the current release contract. Historical UI snapshot scripts remain
available separately and are not reported as passing this candidate.
"""
from pathlib import Path
import json
import subprocess
import xml.etree.ElementTree as ET
from canonical312_review import SOURCE_PINS,reviewed_pin,windows_delta,reverse_version_delta
from stable311_review import reviewed_update_cache_guard

root=Path(__file__).resolve().parents[2]
base='6638a50b3771d717bad9c6704f6c9924bda418fa'
paths=subprocess.check_output(['git','ls-tree','-r','--name-only',base],cwd=root,text=True).splitlines()
prefixes=('android/app/src/main/','android/shared/src/main/','android/wear/src/main/','windows-widget/src/','windows-widget/assets/')
special=('android/app/build.gradle.kts','android/wear/build.gradle.kts','android/settings.gradle.kts','android/build.sh')
app='android/app/src/main/java/dev/bennett/codexmeter/'
history='android/app/src/main/assets/release-history.json'
changed=[];checked=0
objects=subprocess.Popen(['git','cat-file','--batch'],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
def original(path):
 objects.stdin.write((base+':'+path+'\n').encode());objects.stdin.flush()
 header=objects.stdout.readline().split();assert len(header)==3 and header[1]==b'blob',(path,header)
 data=objects.stdout.read(int(header[2]));assert objects.stdout.read(1)==b'\n'
 return data
for path in paths:
 if not path.startswith(prefixes) and path not in special:continue
 current=(root/path).read_bytes();prior=original(path)
 if Path(path).suffix in ['.java','.xml','.json','.cs','.kts','.sh']:
  current=current.replace(b'\r\n',b'\n');prior=prior.replace(b'\r\n',b'\n')
 checked+=1
 if current==prior:continue
 changed.append(path)
 if path in SOURCE_PINS:reviewed_pin(path,current)
 elif path in ['android/app/build.gradle.kts',app+'AppConstants.java']:
  assert reverse_version_delta(path,current.decode()).encode()==prior,path
 elif path==app+'UpdatePreferences.java':assert current==reviewed_update_cache_guard(prior.decode()).encode(),path
 elif path.startswith('windows-widget/src/'):
  assert path in ['windows-widget/src/Widget.cs','windows-widget/src/AssemblyInfo.cs','windows-widget/src/WidgetUpdate.cs'],path
  assert current==windows_delta(path,prior.decode()).encode(),path
 elif path=='android/build.sh':assert current==prior.replace(b'VERSION_NAME="3.1.1"',b'VERSION_NAME="3.1.2"'),path
 elif path==history:
  before=json.loads(prior);after=json.loads(current)
  assert after[1:]==before and after[0]['version']=='3.1.2', 'All published history retained'
  assert after[0]['source']=='https://github.com/ldjkorea/DJS-GPT-HUD/releases/tag/v3.1.2'
 else:raise AssertionError(('Unreviewed production change',path))
extra={'android/app/src/main/res/values/update_contract.xml','android/app/src/main/res/values-ko/update_contract.xml'}
present={p.relative_to(root).as_posix() for prefix in prefixes for p in (root/prefix).rglob('*') if p.is_file()}
assert present-set(paths)==extra,('Unexpected added production file',present-set(paths))
for path in extra:
 nodes=list(ET.fromstring((root/path).read_bytes()));assert len(nodes)==1 and nodes[0].attrib=={'name':'update_metadata_incompatible'} and nodes[0].text.strip(),path
objects.stdin.close();assert objects.wait()==0
assert len(changed)==11,('Reviewed production delta count',changed)
print(json.dumps({'published_base':base,'production_files_checked':checked,'reviewed_changes':changed,'new_resources':sorted(extra),'all_other_production_files':'byte-preserved; CRLF normalized for text only','device_install':'not performed'},ensure_ascii=False))
