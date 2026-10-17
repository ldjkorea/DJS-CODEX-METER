"""Reviewed beta9911 scope; runtime and visual proof are separate."""
from pathlib import Path
import subprocess,json
root=Path(__file__).resolve().parents[2];app=root/'android/app/src/main/java/dev/bennett/codexmeter'
base='b69cfbbf2e6d712ffb98471b35a942ed4f15fa6d'
def old(name):return subprocess.check_output(['git','show',base+':'+name],cwd=root).replace(b'\r\n',b'\n')
def read(name):return (app/name).read_text(encoding='utf-8')
for name in ['BetaV4.java','OAuthClient.java','OAuthService.java','SecureTokenStore.java','UpdateInstaller.java','AccountSession.java','UsageLedgerDatabase.java','TierTheme.java','RefreshScheduler.java','UsageRefreshJobService.java','TrialDemo.java']:
    assert (app/name).read_bytes().replace(b'\r\n',b'\n')==old('android/app/src/main/java/dev/bennett/codexmeter/'+name),name
for name in ['MxpEconomy.java','RpgEngine.java','TierEvolution.java','CoachCatalog.java']:
    path='android/shared/src/main/java/dev/bennett/codexmeter/'+name
    assert (root/path).read_bytes().replace(b'\r\n',b'\n')==old(path),name
path='android/app/src/main/java/dev/bennett/codexmeter/UsageApi.java'
expected=old(path).decode().replace('BetaGrowthStore.response(context, SubscriptionStore.accountKey(context), usageSnapshot);','BetaGrowthStore.response(context, SubscriptionStore.accountKey(context), usageSnapshot);\n                BetaMedalStore.response(context, SubscriptionStore.accountKey(context), usageSnapshot);')
assert read('UsageApi.java')==expected,'Usage API contract changed beyond the additive recording hook'
path='android/app/src/main/java/dev/bennett/codexmeter/BetaGrowthStore.java'
assert read('BetaGrowthStore.java')==old(path).decode().replace('BetaExperience.goal(state,now,BetaPreferences.pinned(c,account))','BetaNextGoal.choose(state,now)'), 'Existing DB/economy contract'
home=read('MainActivity.java');assert home.index('BetaHomeSummaryUi.add(')<home.index('BetaWeeklyUi.add(')<home.index('FunHome.add(')
assert 'BetaExperienceUi.focus(' not in home and 'LedgerDashboard.addOverview(' not in home
assert 'registerOnSharedPreferenceChangeListener(snapshotListener)' in home and 'unregisterOnSharedPreferenceChangeListener(snapshotListener)' in home
assert 'UsageSnapshot snapshot=current;' in home and 'getSharedPreferences(BetaMedalStore.CACHE,0)' in home
assert 'getSharedPreferences(BetaMedalStore.CACHE,0)' in read('GrowthActivity.java')
assert 'SubscriptionValueUi.card(' not in read('FunHome.java') and 'target.addView(value)' not in read('FunHome.java')
for name in ['BetaCompactGrowthUi.java','BetaMedalUi.java','BetaHomeDetailsUi.java','BetaHomeUi.java','BetaQuotaUi.java']:
    text=read(name)
    for blocked in ['getWritableDatabase(', 'UsageApi.refreshAndCache(', 'synchronized(UsageApi.NETWORK_LOCK)', 'BetaV4.confirm(']:assert blocked not in text,(name,blocked)
assert 'workerOnly()' in read('BetaMedalStore.java')
assert 'BetaGrowthStore' not in read('BetaMedalModel.java') and 'xp' not in read('BetaMedalModel.java').lower().replace('no xp','')
original=json.loads((root/'android/app/src/main/assets/coach-ko-v2.json').read_text(encoding='utf-8'))
assert original==json.loads(old('android/app/src/main/assets/coach-ko-v2.json'))
extra=json.loads((root/'android/app/src/main/assets/coach-ko-medals-v1.json').read_text(encoding='utf-8'))
assert len(original['quotes'])==600 and len(extra['quotes'])==24 and len(original['tips'])==40
all_ids=[q['id'] for q in original['quotes']+extra['quotes']];assert len(set(all_ids))==624
tip_ids={t['id'] for t in original['tips']}
for q in extra['quotes']:
    assert q['tipId'] in tip_ids and q['tone']=='spicy'
    assert not any(word in q['text'].lower() for word in ['dick','29,000','29000','대화 내용을 읽'])
assert 'getPrimaryClip(' not in read('CoachUi.java') and 'UsageApi.' not in read('CoachUi.java')
assert 'setMaxLines(2)' in read('CoachUi.java')
print('Beta9911 source: existing auth/economy/tier/worker protections retained; compact roots;624 stable quote IDs/40 tips; separate medal DB')
