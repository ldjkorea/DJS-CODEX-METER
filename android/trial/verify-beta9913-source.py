"""9.9.13 authorized delta against published 9.9.12; all other production bytes retained.
Earlier 9911/9912 historical source fixtures stay unchanged; this gate validates the new approved UI/voice scope.
"""
from pathlib import Path
import subprocess,hashlib
root=Path(__file__).resolve().parents[2]
base='d2fee21b96b0335dc5e1d5ec41421c89d4f3e585'
pins={'android/app/src/main/java/dev/bennett/codexmeter/CoachUi.java': 'd1b6efacb0dad04c20a4265579a40ba7127bae831022d3edd5156de9a3589799', 'android/app/src/main/java/dev/bennett/codexmeter/FunHome.java': '3bdf42abd9a6bf1c9126751264b2dee0d0a414eceab7754174c9369ce532b9c2', 'android/app/src/main/java/dev/bennett/codexmeter/SpicyQuotes.java': '00c26f6c4188cc3ea2d831eb0bb2c6314833d9f9cb0cc7c10aa5bca1471be992', 'android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java': '9130af75b805c7f088b531b49f8f953c79225435be88b4a818cf1132906933a1', 'android/app/src/main/assets/coach-ko-v2.json': 'd113095f9649ee193e08aee7f43604bf0fe91a52c3ccd7ea38a42b5eb8228653', 'android/app/src/main/assets/coach-ko-medals-v1.json': '99e16f1ead62f15850a677ed613e1bee565bbb20353d508bfb1238f43c289fd8'}
app='android/app/src/main/java/dev/bennett/codexmeter/'
prefixes=('android/app/src/main/','android/shared/src/main/','android/wear/src/main/','windows-widget/src/','windows-widget/assets/')
special=('android/app/build.gradle.kts','android/wear/build.gradle.kts','android/settings.gradle.kts','android/build.sh')
paths=subprocess.check_output(['git','ls-tree','-r','--name-only',base],cwd=root,text=True).splitlines();changed=[];checked=0
for path in paths:
 if not path.startswith(prefixes) and path not in special:continue
 before=subprocess.check_output(['git','show',base+':'+path],cwd=root)
 current=(root/path).read_bytes()
 if Path(path).suffix in ['.java','.xml','.json','.cs','.kts','.sh','.txt']:before=before.replace(b'\r\n',b'\n');current=current.replace(b'\r\n',b'\n')
 checked+=1
 if before==current:continue
 changed.append(path)
 if path in pins:assert hashlib.sha256(current).hexdigest()==pins[path],path
 elif path in ['android/app/build.gradle.kts',app+'AppConstants.java']:assert current==before.replace(b'990012',b'990013').replace(b'9.9.12',b'9.9.13'),path
 elif path==app+'MainActivity.java':
  call=b'        FunHome.add(this,content,dark,this::restoreHomeScroll,crestSlot);\n';summary=b'        LinearLayout crestSlot=BetaHomeSummaryUi.add(this,content,snapshot,dark,quotaClockUpdates);\n'
  assert before.count(call)==before.count(summary)==1
  assert current==before.replace(call,b'').replace(summary,summary+call),'Only MainActivity home call moved; trial OAuth/permissions retained'
 elif path=='android/build.sh':assert hashlib.sha256(current).hexdigest()=='a59d05ccb52e2e5b05e565f5b161b4b505421eb9b04fdb94ba202ec745a20350','Reviewed beta phone-only build script'
 else:raise AssertionError(('Unreviewed production change',path))
present={p.relative_to(root).as_posix() for prefix in prefixes for p in (root/prefix).rglob('*') if p.is_file()}
assert not present-set(paths),'No extra production files'
assert len(changed)==10,changed
home=(root/(app+'MainActivity.java')).read_text(encoding='utf-8');assert home.index('BetaHomeSummaryUi.add(')<home.index('FunHome.add(')<home.index('BetaWeeklyUi.add(')
assert 'BetaExperienceUi.focus(' not in home and 'LedgerDashboard.addOverview(' not in home
coach=(root/(app+'CoachUi.java')).read_text(encoding='utf-8');assert 'setMaxLines(2)' not in coach and 'getPrimaryClip(' not in coach and 'UsageApi.' not in coach
assert 'quote.setTextAlignment(View.TEXT_ALIGNMENT_CENTER)' in coach
assert '문구 편집·추가·수동 교체는 제공하지 않습니다' in coach
assert 'dev.bennett.codexmeter.trial' in (root/'android/app/build.gradle.kts').read_text() and '9.9.13' in (root/'android/app/build.gradle.kts').read_text()
print(f'Beta9913: {checked} production files verified; exact10 approved UI/voice/version/build deltas; auth/storage/economy/quotas/tier/Wear/Windows/permissions unchanged. Historical fixtures retained.')
