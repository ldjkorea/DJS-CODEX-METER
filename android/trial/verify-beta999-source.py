"""Beta999 reviewed source boundaries; runtime proof is separate."""
from pathlib import Path
import json, subprocess
root=Path(__file__).resolve().parents[2];app=root/'android/app/src/main/java/dev/bennett/codexmeter'
read=lambda n:(app/n).read_text(encoding='utf-8')
baseline='b9fa5b513a5af83379aaf65dd570acf88b65dd10'
for name in ['BetaV4.java','OAuthClient.java','SecureTokenStore.java','UpdateInstaller.java','AccountSession.java','OAuthService.java','CoachUi.java','SpicyQuotes.java','BetaGrowthStore.java','UsageLedgerDatabase.java']:
 old=subprocess.check_output(['git','show',baseline+':android/app/src/main/java/dev/bennett/codexmeter/'+name],cwd=root).replace(b'\r\n',b'\n')
 assert (app/name).read_bytes().replace(b'\r\n',b'\n')==old,name
assert subprocess.check_output(['git','show',baseline+':android/app/src/main/assets/coach-ko-v2.json'],cwd=root).replace(b'\r\n',b'\n')==(root/'android/app/src/main/assets/coach-ko-v2.json').read_bytes().replace(b'\r\n',b'\n')
home=read('MainActivity.java');assert home.index('BetaHomeSummaryUi.add(')<home.index('BetaWeeklyUi.add(')<home.index('FunHome.add(')
assert 'BetaExperienceUi.focus(' not in home and 'LedgerDashboard.addOverview(' not in home
summary=read('BetaHomeSummaryUi.java');assert 'BetaQuotaUi.addContent(' in summary and 'BetaHomeUi.card(activity,dark,false)' in summary
for name in ['BetaHomeUi.java','BetaHomeSummaryUi.java','BetaWeeklyUi.java','BetaQuotaUi.java']:
 s=read(name)
 for blocked in ['UsageApi.','UsageLedgerDatabase.load(', 'BetaV4.confirm(', 'synchronized(AccountSession.NETWORK_LOCK)']:assert blocked not in s,(name,blocked)
reader=read('BetaWeeklyReader.java');assert 'OPEN_READONLY' in reader and 'LIMIT ' in reader and 'MAX_ROWS=20000' in reader and 'Looper.getMainLooper()' in reader
for blocked in ['execSQL','getWritableDatabase','migrate','UsageApi.']:assert blocked not in reader,blocked
assert 'stamp.equals(stamp(' in read('BetaWeeklyUi.java') and 'isAttachedToWindow()' in read('BetaWeeklyUi.java')
print('Beta999 source: one combined surface, immediately adjacent weekly section; preserved auth/economy/content; bounded worker-only SELECT')

# Regression boundaries for the weekly display follow-up.
assert 'registerOnSharedPreferenceChangeListener(snapshotListener)' in home
assert 'unregisterOnSharedPreferenceChangeListener(snapshotListener)' in home
assert 'UsageSnapshot snapshot=current;' in home
service=read('UsageRefreshJobService.java');start=service[service.index('public boolean onStartJob'):service.index('public boolean onStopJob')]
assert 'TrialDemo.active(this)' in start and 'NETWORK_LOCK' not in start
run=service[service.index('public void run()'):];assert run.index('synchronized(UsageApi.NETWORK_LOCK)')<run.index('TrialDemo.active(')<run.index('UsageApi.refreshAndCache(')
assert 'RefreshScheduler.cancelAll(c)' in read('TrialDemo.java')
print('Weekly follow-up: snapshot observer lifecycle, single snapshot render, demo job guards, worker account-transition fence')
