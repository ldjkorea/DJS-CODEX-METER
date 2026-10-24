"""Focused local revision check, pinned to published 3.1.3. No network/device I/O."""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
ANDROID = ROOT / 'android'
BASE = '2e99535f1497e0ce8644ab73de75f2c05a523a4b'
OUT = ANDROID / 'build/coach-banter-20261024/verification'
OUT.mkdir(parents=True, exist_ok=True)
ASSETS = 'android/app/src/main/assets/'


def before(path):
    return subprocess.check_output(['git', 'show', BASE + ':' + path], cwd=ROOT)


changed = marked = paid = total = 0
catalogs = []
for name in ['coach-ko-v2.json', 'coach-ko-medals-v1.json']:
    old = json.loads(before(ASSETS + name))
    new = json.loads((ROOT / ASSETS / name).read_text('utf-8'))
    assert len(old['quotes']) == len(new['quotes'])
    assert {k: v for k, v in old.items() if k not in ['quotes', 'contentVersion']} == {
        k: v for k, v in new.items() if k not in ['quotes', 'contentVersion']}
    for a, b in zip(old['quotes'], new['quotes']):
        assert {k: v for k, v in a.items() if k != 'text'} == {
            k: v for k, v in b.items() if k not in ['text', 'selectionPriority']}, a['id']
        assert b.get('selectionPriority', 0) in [0, 1]
        changed += a['text'] != b['text']
        marked += b.get('selectionPriority', 0)
        paid += bool(b.get('paidText'))
        total += 1
        # Retain the existing "cannot promise zero cost" warning; it makes no fee claim.
        assert not re.search(r'DICK|29,?000|[1-9][0-9,]*\s*원|씨발|병신', b['text'], re.I), b['id']
    catalogs.append(new)
assert catalogs[0]['schemaVersion'] == 2 and catalogs[0]['contentVersion'] == 5
assert catalogs[1]['version'] == 1
assert len(catalogs[0]['tips']) == 40 and total == 624 and paid == 4
assert changed == 131 and marked == 325
app = ANDROID / 'app/src/main/java/dev/bennett/codexmeter'
shared = ANDROID / 'shared/src/main/java/dev/bennett/codexmeter'
catalog_source = (shared / 'CoachCatalog.java').read_text('utf-8')
published_source = before('android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java').decode('utf-8')
# No changes to group evidence, paid display, schema migration or history primitives.
start = '    public static String group('
end = '    public Quote select('
assert catalog_source.split(start)[1].split(end)[0] == published_source.split(start)[1].split(end)[0]
ui = (app / 'CoachUi.java').read_text('utf-8')
for token in ['setTextSize(18)', 'setMinimumHeight(Ui.dp(a,48))', 'setSingleLine(false)',
              'setIncludeFontPadding(true)', 'setLineSpacing(0,1.3f)', 'Gravity.CENTER',
              'View.TEXT_ALIGNMENT_CENTER', 'LineBreaker.BREAK_STRATEGY_BALANCED',
              'Layout.HYPHENATION_FREQUENCY_NONE', 'new LinearLayout.LayoutParams(-1,-2)']:
    assert token in ui, token
assert not re.search(r'setMaxLines|setEllipsize|setAutoSize|setTextScaleX|setHeight\(', ui)
assert 'if(android.os.Build.VERSION.SDK_INT>=29)quote.setBreakStrategy' in ui
assert 'setPrimaryClip' in ui and 'getPrimaryClip' not in ui
assert not re.search(r'HttpURLConnection|ReleaseUpdateClient|RpgGrowthEngine|reconcile\(', ui)
quotes = (app / 'SpicyQuotes.java').read_text('utf-8')
assert before('android/app/src/main/java/dev/bennett/codexmeter/SpicyQuotes.java').decode('utf-8').replace(
    ':20261021-friend', ':20261024-banter') == quotes
allowed = {ASSETS + x for x in ['coach-ko-v2.json', 'coach-ko-medals-v1.json']} | {
    'android/app/src/main/java/dev/bennett/codexmeter/' + x for x in ['CoachUi.java', 'SpicyQuotes.java']} | {
    'android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java'}
production_changes = set(subprocess.check_output(['git', 'diff', '--name-only', BASE, '--',
    'android/app/src', 'android/shared/src', 'android/wear', 'windows-widget/src', 'windows-widget/assets',
    'android/app/build.gradle.kts', 'android/settings.gradle.kts', 'android/build.sh'], cwd=ROOT, text=True).splitlines())
assert production_changes == allowed, production_changes

jar = ANDROID / 'build/test-libs/json-20250517.jar'
jdk = Path(os.environ['JAVA_HOME']) / 'bin'
suffix = '.exe' if os.name == 'nt' else ''
sources = list((ANDROID / 'shared/src/main/java').rglob('*.java')) + [app / (n + '.java') for n in ['BetaV4', 'BetaCatalog', 'BetaMedalCoach']]


def compile_into(dest, files):
    dest.mkdir(exist_ok=True)
    quote = lambda p: '"' + str(p).replace('\\', '/') + '"'
    argfile = dest / 'javac.args'
    argfile.write_text('-encoding UTF-8\n-cp ' + quote(jar) + '\n-d ' + quote(dest) + '\n' + '\n'.join(map(quote, files)), 'utf-8')
    subprocess.run([jdk / ('javac' + suffix), '@' + str(argfile)], check=True)


classes = OUT / 'current'
tests = ['CoachV2SelfTest', 'CoachReliabilitySelfTest', 'CoachBanterSelfTest', 'CoachBanterDistribution']
compile_into(classes, sources + [ANDROID / 'tests' / (name + '.java') for name in tests])


def run(dest, name, args):
    cp = os.pathsep.join(map(str, [dest, jar]))
    return subprocess.check_output([jdk / ('java' + suffix), '-Xmx512m', '-ea', '-cp', cp,
        'dev.bennett.codexmeter.' + name, *map(str, args)], encoding='utf-8')


base_path = ROOT / ASSETS / 'coach-ko-v2.json'
medal_path = ROOT / ASSETS / 'coach-ko-medals-v1.json'
results = []
for name, args in [('CoachV2SelfTest', [base_path]), ('CoachReliabilitySelfTest', [base_path]),
                   ('CoachBanterSelfTest', [base_path, medal_path])]:
    result = run(classes, name, args)
    print(result.strip(), flush=True)
    results.append(result)
baseline = OUT / 'published'
baseline.mkdir(exist_ok=True)
old_catalog = baseline / 'CoachCatalog.java'
old_catalog.write_bytes(before('android/shared/src/main/java/dev/bennett/codexmeter/CoachCatalog.java'))
old_json = baseline / 'coach-ko-v2.json'
old_json.write_bytes(before(ASSETS + 'coach-ko-v2.json'))
compile_into(baseline, [old_catalog if p.name == 'CoachCatalog.java' else p for p in sources] + [ANDROID / 'tests/CoachBanterDistribution.java'])
distribution = {}
for label, dest, asset in [('published', baseline, old_json), ('candidate', classes, base_path)]:
    distribution[label] = json.loads(run(dest, 'CoachBanterDistribution', [asset, base_path]))
for group in ['tiny', 'low', 'moderate', 'mid', 'high', 'heavy', 'burst', 'reset', 'imminent', 'missing', 'stale', 'network']:
    a, b = distribution['published'][group], distribution['candidate'][group]
    assert b['curated'] > a['curated'], (group, a, b)
    assert a['distinct'] == b['distinct'], (group, a, b)
    print(f"{group}: curated {a['curated'] / 30:.1f}% -> {b['curated'] / 30:.1f}%; all {b['distinct']} rows reachable", flush=True)
assert distribution['published']['critical'] == distribution['candidate']['critical'], 'Urgent selection unchanged'
receipt = {'base': BASE, 'scope': 'local source/JVM only; no Android/account/device evidence',
    'quotes': total, 'tips': 40, 'text_edits': changed, 'retained': total - changed,
    'curated_rows': marked, 'paid_variants_unchanged': paid, 'results': results, 'distribution': distribution,
    'production_changes': sorted(production_changes),
    'asset_sha256': {name: hashlib.sha256((ROOT / ASSETS / name).read_bytes()).hexdigest()
                     for name in ['coach-ko-v2.json', 'coach-ko-medals-v1.json']}}
(OUT / 'result.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2), 'utf-8')
print(f'Preservation: {total} IDs, 40 tips and legacy maps retained; {changed} edits / {total - changed} retained; {marked} curated. Five production files only.', flush=True)
