"""Exact reviewed time-display/version delta; legacy test fixtures remain unchanged."""
from pathlib import Path
import hashlib,subprocess
root=Path(__file__).resolve().parents[2]
base="e052f8845b6fdfa6543e9b4896a1231ccc535e18"
pins={'android/app/build.gradle.kts': '3fbb136516275203bbbf449c1d19e9868ea8d05e86848ae09f73cfabe93efc9c', 'android/app/src/main/assets/release-history.json': '5c031596029a6c9ae2f83c70898be94aa4fc6e4a1644f75f6151cccb66e2b36b', 'android/app/src/main/java/dev/bennett/codexmeter/AppConstants.java': 'ee434947a900755280fdf210521e2d5f157639cfd9405623b2f2ec05a80b63fd', 'android/app/src/main/java/dev/bennett/codexmeter/BetaQuotaUi.java': '7dbf67573bc19afd1ee2fe2008f68dc2871ce898777291e9b3cc2458cf2de840', 'android/app/src/main/java/dev/bennett/codexmeter/MainActivity.java': 'a7e07bbd31c75da9b1b6f6d7b6a7ad090ca6f95d21c71af45893783ad6f3fee1', 'android/app/src/main/java/dev/bennett/codexmeter/HomeInsightClock.java': '77f7c1ba9aa2a1a1544fa941795aaa4307f24a76ad5f7d811ea8bce693da189c', 'android/app/src/main/res/values/home_quota_strings.xml': '5bcfa09271ac628bf9a4e243f0b228e04fb5c17561e9c077908deadc48a323c3', 'android/app/src/main/res/values-ko/home_quota_strings.xml': '7b33f9c8dc4dfcb1a6e83e4bf68d328a343fdabc7cfa68f3ed7c93f80c546ee4', 'android/build.sh': 'e45f30b329dba19f2297bab28b3dcabe65c60b71bad9715877e79d5d6fedf44e'}
prefixes=('android/app/src/main/','android/shared/src/main/','android/wear/src/main/','windows-widget/src/','windows-widget/assets/')
special=('android/app/build.gradle.kts','android/wear/build.gradle.kts','android/settings.gradle.kts','android/build.sh')
paths=subprocess.check_output(['git','ls-tree','-r','--name-only',base],cwd=root,text=True).splitlines()
norm=lambda b:b.replace(b'\r\n',b'\n')
changed=[];checked=0
with subprocess.Popen(['git','cat-file','--batch'],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE) as git:
 for path in paths:
  if not path.startswith(prefixes) and path not in special:continue
  git.stdin.write((base+':'+path+'\n').encode());git.stdin.flush()
  header=git.stdout.readline().split();assert header[1]==b'blob',path
  before=git.stdout.read(int(header[2]));assert git.stdout.read(1)==b'\n'
  current=(root/path).read_bytes();checked+=1
  if norm(before)==norm(current):continue
  changed.append(path);assert path in pins and hashlib.sha256(norm(current)).hexdigest()==pins[path],path
 git.stdin.close()
present={p.relative_to(root).as_posix() for prefix in prefixes for p in (root/prefix).rglob('*') if p.is_file()}
extra=present-set(paths)
assert extra=={p for p in pins if p not in paths},extra
for path,expected in pins.items():assert hashlib.sha256(norm((root/path).read_bytes())).hexdigest()==expected,path
assert len(changed)==6,changed
print(f'Stable313: {checked} production files checked; exact6 existing changes and3 added time-display files; package/auth/storage/economy/voice/Wear/Windows unchanged.')
