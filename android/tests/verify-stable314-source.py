"""Reviewed 3.1.4 delta against published 3.1.3; old release fixtures stay frozen."""
from pathlib import Path
import json
import re
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
base = '2e99535f1497e0ce8644ab73de75f2c05a523a4b'
coach = 'eeaa51a'
app = 'android/app/src/main/java/dev/bennett/codexmeter/'
shared = 'android/shared/src/main/java/dev/bennett/codexmeter/'
coach_paths = {'android/app/src/main/assets/' + n for n in ['coach-ko-v2.json', 'coach-ko-medals-v1.json']} | {
    app + 'CoachUi.java', app + 'SpicyQuotes.java', shared + 'CoachCatalog.java'}
changed_allowed = coach_paths | {app + n for n in ['AppConstants.java', 'MainActivity.java',
    'ReleaseUpdateClient.java', 'ReleaseUpdateJobService.java', 'UpdatePreferences.java']} | {
    'android/app/build.gradle.kts', 'android/build.sh', 'android/app/src/main/assets/release-history.json',
    'android/app/src/main/res/values/strings.xml', 'android/app/src/main/res/values-ko/strings.xml'}
added_allowed = {app + 'ForegroundUpdateNotice.java', shared + 'ForegroundUpdatePolicy.java'} | {
    'android/app/src/main/res/' + locale + '/foreground_update_strings.xml' for locale in ['values', 'values-ko']}
prefixes = ('android/app/src/main/', 'android/shared/src/main/', 'android/wear/src/main/',
            'windows-widget/src/', 'windows-widget/assets/')
special = ('android/app/build.gradle.kts', 'android/wear/build.gradle.kts', 'android/settings.gradle.kts', 'android/build.sh')
norm = lambda b: b.replace(b'\r\n', b'\n')


def old(path, revision=base):
    return norm(subprocess.check_output(['git', 'show', revision + ':' + path], cwd=root))


paths = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', base], cwd=root, text=True).splitlines()
changed = set()
checked = 0
with subprocess.Popen(['git', 'cat-file', '--batch'], cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE) as git:
    for path in paths:
        if not path.startswith(prefixes) and path not in special:
            continue
        git.stdin.write((base + ':' + path + '\n').encode()); git.stdin.flush()
        header = git.stdout.readline().split(); assert header[1] == b'blob', path
        previous = git.stdout.read(int(header[2])); assert git.stdout.read(1) == b'\n'
        checked += 1
        if norm(previous) != norm((root / path).read_bytes()):
            changed.add(path)
    git.stdin.close()
assert changed == changed_allowed, (changed - changed_allowed, changed_allowed - changed)
present = {p.relative_to(root).as_posix() for prefix in prefixes for p in (root / prefix).rglob('*') if p.is_file()}
assert present - set(paths) == added_allowed, present - set(paths)
for path in coach_paths:
    assert norm((root / path).read_bytes()) == old(path, coach), 'Completed coach content changed: ' + path
for path, replacements in {
    'android/app/build.gradle.kts': [('versionCode = 90', 'versionCode = 91'), ('versionName = "3.1.3"', 'versionName = "3.1.4"')],
    'android/build.sh': [('VERSION_NAME="3.1.3"', 'VERSION_NAME="3.1.4"')],
    app + 'AppConstants.java': [('VERSION_CODE = 90', 'VERSION_CODE = 91'), ('VERSION_NAME = "3.1.3"', 'VERSION_NAME = "3.1.4"')],
}.items():
    expected = old(path).decode('utf-8')
    for a, b in replacements: expected = expected.replace(a, b)
    assert (root / path).read_text('utf-8') == expected, path
history = json.loads((root / 'android/app/src/main/assets/release-history.json').read_text('utf-8'))
assert history[0]['version'] == '3.1.4'
assert history[1:] == json.loads(old('android/app/src/main/assets/release-history.json'))
client = (root / app / 'ReleaseUpdateClient.java').read_text('utf-8')
previous_client = old(app + 'ReleaseUpdateClient.java').decode('utf-8')
assert client[client.index('    public static List<GitHubRelease> check('):] == previous_client[previous_client.index('    public static List<GitHubRelease> check('):]
assert 'synchronized (LOCK)' in client.split('checkIfStale')[1].split('public static List')[0]
notice = (root / app / 'ForegroundUpdateNotice.java').read_text('utf-8')
for token in ['PENDING.compareAndSet(false, true)', 'ReleaseUpdateClient.checkIfStale(app)',
              'UpdatePreferences.automaticChecks(app)', 'UpdatePreferences.foregroundVersionShown',
              'UpdatePreferences.markForegroundVersionShown', 'active.clear()', 'MAIN.post(',
              'channel.equals(UpdatePreferences.channel(app))', 'AccountSession.signInActive()',
              'AppPreferences.isOAuthPending(app)']:
    assert token in notice, token
assert notice.index('notice.show();') < notice.index('UpdatePreferences.markForegroundVersionShown')
assert not re.search(r'EXTRA_START_INSTALL|beginInstall|UpdateInstaller|Toast\.|POST_NOTIFICATIONS|SecureTokenStore|UsageApi', notice)
assert 'setNegativeButton' in notice and 'setPositiveButton' in notice
main = (root / app / 'MainActivity.java').read_text('utf-8')
expected = old(app + 'MainActivity.java').decode('utf-8')
expected = expected.replace('MainActivity.this.rebuild();\n            }\n        }\n    };', 'MainActivity.this.rebuild();\n                ForegroundUpdateNotice.metadataChanged(MainActivity.this);\n            }\n        }\n    };', 1)
expected = expected.replace('protected void onResume() {\n        super.onResume();', 'protected void onResume() {\n        super.onResume();\n        if(content!=null)ForegroundUpdateNotice.resume(this);')
expected = expected.replace('protected void onPause() {', 'protected void onPause() {\n        ForegroundUpdateNotice.pause(this);')
assert main == expected, 'Only three lifecycle hooks allowed in home'
prefs = (root / app / 'UpdatePreferences.java').read_text('utf-8')
assert 'new HashSet<>(prefs(context).getStringSet(KEY_FOREGROUND_SHOWN' in prefs
assert '.putStringSet(KEY_FOREGROUND_SHOWN, shown).apply()' in prefs
for locale in ['values', 'values-ko']:
    new = ET.parse(root / ('android/app/src/main/res/' + locale + '/foreground_update_strings.xml')).getroot()
    assert len(new) == 4
    assert '%1$s' in new.find("string[@name='foreground_update_title']").text
    current = ET.parse(root / ('android/app/src/main/res/' + locale + '/strings.xml')).getroot()
    previous = ET.fromstring(old('android/app/src/main/res/' + locale + '/strings.xml'))
    as_dict = lambda tree: {e.get('name'): ET.tostring(e, encoding='unicode') for e in tree}
    a, b = as_dict(previous), as_dict(current)
    assert a.keys() == b.keys()
    assert {k for k in a if a[k] != b[k]} == {'ui_notify_when_an_update_is_available_d59712', 'ui_check_signed_github_releases_1_s_584964'}
print(f'Stable314 source: {checked} existing production files checked; exact{len(changed)} edits/{len(added_allowed)} additions; completed coach preserved; auth/account-growth storage, permissions, installer, trust, Wear/Windows unchanged.')
