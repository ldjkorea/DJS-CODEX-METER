"""Exact reviewed time-display/version delta; legacy test fixtures remain unchanged."""
from pathlib import Path
import hashlib,subprocess
root=Path(__file__).resolve().parents[2]
base="e052f8845b6fdfa6543e9b4896a1231ccc535e18"
pins={'android/app/build.gradle.kts': '3fbb136516275203bbbf449c1d19e9868ea8d05e86848ae09f73cfabe93efc9c', 'android/app/src/main/assets/release-history.json': '8ea4f8bb0917dd5e2916e38788f11ca938a3daa407dd04ad9b7b460a7adf7951', 'android/app/src/main/java/dev/bennett/codexmeter/AppConstants.java': 'ee434947a900755280fdf210521e2d5f157639cfd9405623b2f2ec05a80b63fd', 'android/app/src/main/java/dev/bennett/codexmeter/BetaQuotaUi.java': '7dbf67573bc19afd1ee2fe2008f68dc2871ce898777291e9b3cc2458cf2de840', 'android/app/src/main/java/dev/bennett/codexmeter/MainActivity.java': 'fcd751b62bad2de2bef03638f7d58c66169f5e42d0b8d97b97bd63f86d07d41b', 'android/app/src/main/java/dev/bennett/codexmeter/HomeInsightClock.java': '77f7c1ba9aa2a1a1544fa941795aaa4307f24a76ad5f7d811ea8bce693da189c', 'android/app/src/main/res/values/home_quota_strings.xml': '5bcfa09271ac628bf9a4e243f0b228e04fb5c17561e9c077908deadc48a323c3', 'android/app/src/main/res/values-ko/home_quota_strings.xml': '7b33f9c8dc4dfcb1a6e83e4bf68d328a343fdabc7cfa68f3ed7c93f80c546ee4', 'android/build.sh': 'e45f30b329dba19f2297bab28b3dcabe65c60b71bad9715877e79d5d6fedf44e', 'android/app/src/main/assets/coach-ko-v2.json': 'd113095f9649ee193e08aee7f43604bf0fe91a52c3ccd7ea38a42b5eb8228653', 'android/app/src/main/assets/coach-ko-medals-v1.json': '99e16f1ead62f15850a677ed613e1bee565bbb20353d508bfb1238f43c289fd8', 'android/app/src/main/java/dev/bennett/codexmeter/CoachUi.java': 'd1b6efacb0dad04c20a4265579a40ba7127bae831022d3edd5156de9a3589799', 'android/app/src/main/java/dev/bennett/codexmeter/FunHome.java': '3bdf42abd9a6bf1c9126751264b2dee0d0a414eceab7754174c9369ce532b9c2', 'android/app/src/main/java/dev/bennett/codexmeter/SpicyQuotes.java': '00c26f6c4188cc3ea2d831eb0bb2c6314833d9f9cb0cc7c10aa5bca1471be992', 'android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java': 'a8a6d54ab38ef996747135c7fb6ed7646e978257676386e25fe1f3a370fe0258'}
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
assert len(changed)==12,changed
print(f'Stable313: {checked} production files checked; exact12 existing changes and3 added time-display files; package/auth/storage/economy/Wear/Windows unchanged; reviewed UI/voice included.')
