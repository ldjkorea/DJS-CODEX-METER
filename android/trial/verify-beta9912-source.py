"""Freeze the beta production tree against published 9.9.11, with exact approved URL/version/cache deltas."""
from pathlib import Path
import subprocess,hashlib,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
base='5a2d7b77c23683435f45c1d4f13eb0b35e4f4fe9'
pins={'android/app/src/main/java/dev/bennett/codexmeter/GitHubReleaseParser.java': '5ce7d5664d95882f205ca84b6dd786b313e35ad1e951067b39c14ef313a2702f', 'android/app/src/main/java/dev/bennett/codexmeter/GitHubReleaseSource.java': 'b8908b85d45bbbc2da2be3e5281ce195d75bdc931475317a7327f889374f82d2', 'android/shared/src/main/java/dev/bennett/codexmeter/KoreanUpdateTrust.java': '2f6b5f117da554e5e9409c470e47e9671392805279cbb3eaec846804881c0780'}
paths=subprocess.check_output(['git','ls-tree','-r','--name-only',base],cwd=root,text=True).splitlines()
prefixes=('android/app/src/main/','android/shared/src/main/','android/wear/src/main/','windows-widget/src/','windows-widget/assets/')
special=('android/app/build.gradle.kts','android/wear/build.gradle.kts','android/settings.gradle.kts','android/build.sh')
app='android/app/src/main/java/dev/bennett/codexmeter/'
changed=[];checked=0
objects=subprocess.Popen(['git','cat-file','--batch'],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE)
for path in paths:
 if not path.startswith(prefixes) and path not in special:continue
 objects.stdin.write((base+':'+path+'\n').encode());objects.stdin.flush()
 header=objects.stdout.readline().split();assert len(header)==3 and header[1]==b'blob'
 prior=objects.stdout.read(int(header[2]));assert objects.stdout.read(1)==b'\n'
 current=(root/path).read_bytes()
 if Path(path).suffix in ['.java','.xml','.json','.cs','.kts','.sh','.txt']:current=current.replace(b'\r\n',b'\n');prior=prior.replace(b'\r\n',b'\n')
 checked+=1
 if current==prior:continue
 changed.append(path)
 if path in pins:assert hashlib.sha256(current).hexdigest()==pins[path],path
 elif path=='android/app/build.gradle.kts':assert current==prior.replace(b'versionCode = 990011',b'versionCode = 990012').replace(b'versionName = "9.9.11"',b'versionName = "9.9.12"').replace(b'ldjkorea/codex-meter-ko/releases?',b'ldjkorea/DJS-GPT-HUD/releases?'),path
 elif path==app+'AppConstants.java':assert current==prior.replace(b'VERSION_CODE = 990011;',b'VERSION_CODE = 990012;').replace(b'VERSION_NAME = "9.9.11"',b'VERSION_NAME = "9.9.12"'),path
 elif path==app+'UpdatePreferences.java':
  anchor=b'List<GitHubRelease> parsed = GitHubReleaseParser.parse(json, BuildConfig.DEBUG);\n        SharedPreferences.Editor editor'
  assert prior.count(anchor)==1
  guard=b'        // A populated but incompatible feed is not a successful empty update list.\n        // Keep the prior cache/ETag; the caller records the failure for the existing UI.\n        if (parsed.isEmpty() && new org.json.JSONArray(json).length() > 0) {\n            throw new IllegalStateException(context.getString(R.string.update_metadata_incompatible));\n        }\n'
  assert current==prior.replace(anchor,anchor.split(b'\n')[0]+b'\n'+guard+b'        SharedPreferences.Editor editor'),path
 else:raise AssertionError(('Unreviewed beta production change',path))
extra={'android/app/src/main/res/values/update_contract.xml','android/app/src/main/res/values-ko/update_contract.xml'}
present={p.relative_to(root).as_posix() for prefix in prefixes for p in (root/prefix).rglob('*') if p.is_file()}
assert present-set(paths)==extra
for path in extra:
 nodes=list(ET.fromstring((root/path).read_bytes()));assert len(nodes)==1 and nodes[0].attrib=={'name':'update_metadata_incompatible'} and nodes[0].text.strip()
objects.stdin.close();assert objects.wait()==0
assert len(changed)==6,changed
print(f'Beta 9.9.12: {checked} production files checked; six reviewed files and two new messages; all other production bytes preserved (CRLF normalized). No device install proof.')
