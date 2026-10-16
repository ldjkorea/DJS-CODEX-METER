"""Reviewed beta boundaries; complements execution tests, not a runtime claim."""
from pathlib import Path
import json,subprocess
root=Path(__file__).resolve().parents[2]
app=root/'android/app/src/main/java/dev/bennett/codexmeter'
ui=(app/'CoachUi.java').read_text(encoding='utf-8')
assert 'SpicyQuotes.rotate(' not in ui and 'coach_interests' not in ui and 'EditText' not in ui
assert 'copyPrompt(' in ui and 'CoachPolicy.tone(a)' in ui
for file in ['BetaExperience.java','BetaExperienceUi.java','BetaHubActivity.java']:
    s=(app/file).read_text(encoding='utf-8')
    assert 'BetaV4.confirm(' not in s and 'UsageApi.refresh' not in s
assert 'AccountSession' not in (app/'BetaBundle.java').read_text(encoding='utf-8')
old=json.loads(subprocess.check_output(['git','show','8daf3f7:android/app/src/main/assets/coach-ko-v2.json'],cwd=root))
new=json.loads((root/'android/app/src/main/assets/coach-ko-v2.json').read_text(encoding='utf-8'))
assert [q['id'] for q in old['quotes']]==[q['id'] for q in new['quotes']]
assert old['legacyIds']==new['legacyIds'] and old['tips']==new['tips']
assert sum(a['text']!=b['text'] for a,b in zip(old['quotes'],new['quotes']))==150
for q in new['quotes']:
    assert q['tone']=='spicy'
    assert not any(x in q['text'].lower() for x in ['dick','29000','29,000'])
for name in ['BetaV4.java','OAuthClient.java','SecureTokenStore.java','UpdateInstaller.java','AccountSession.java','OAuthService.java']:
    current=(app/name).read_bytes().replace(b'\r\n',b'\n')
    before=subprocess.check_output(['git','show','8daf3f7:android/app/src/main/java/dev/bennett/codexmeter/'+name],cwd=root).replace(b'\r\n',b'\n')
    assert current==before,name
assert '2 * 1024 * 1024' in (app/'ReleaseUpdateClient.java').read_text()
home=(app/'MainActivity.java').read_text(encoding='utf-8')
assert home.index('BetaQuotaUi.add(')<home.index('LinearLayout crestSlot=')<home.index('BetaExperienceUi.focus(')
quota=(app/'BetaQuotaUi.java').read_text(encoding='utf-8')
assert 'BetaPreferences.preset' not in quota and 'UsagePrecision.remaining(' in quota
assert 'UsageApi.' not in quota and 'UsageLedgerDatabase' not in quota and 'BetaV4.confirm' not in quota
print('Beta998 source: stable 600 IDs/40 tips,150 revised quotes; locked controls; protected economy/auth/installer unchanged')
