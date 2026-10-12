"""Run existing release checks once, with explicit failures and scoped evidence."""
from pathlib import Path
import argparse, json, os, shutil, subprocess, sys, time

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--bash', default=shutil.which('bash'))
parser.add_argument('--apk', action='append', default=[])
args = parser.parse_args()
assert args.bash, 'Bash is required for the existing regression suite'
out = root/'android/build/release-validation'; out.mkdir(parents=True, exist_ok=True)
checks = [
    ('regression', 'JVM, source guards, SQLite fixtures; Windows fixtures on Windows', [args.bash, str(root/'android/run-tests.sh')]),
    ('localization', 'Resource/source contracts and JVM formatter fixtures', [sys.executable, str(root/'android/tests/verify-localization.py')]),
    ('publication', 'Offline provenance/fonts; APK archives if provided', [sys.executable, str(root/'android/tests/verify-publication.py'), *args.apk]),
    ('public_source', 'Git index private/runtime files and known credential patterns', [sys.executable, str(root/'android/tests/verify-public-source.py')]),
]
results = []
for name, scope, command in checks:
    started = time.monotonic()
    with (out/(name+'.log')).open('w', encoding='utf-8') as log:
        result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT, env=os.environ)
    results.append(dict(name=name, scope=scope, exit_code=result.returncode,
                        passed=result.returncode == 0, seconds=round(time.monotonic()-started, 3)))
    (out/'RESULTS.json').write_text(json.dumps(dict(checks=results,
        all_requested_passed=len(results)==len(checks) and all(r['passed'] for r in results),
        android_native='separate', physical_device='not verified'), indent=2)+'\n', encoding='utf-8')
    print(name, 'PASS' if result.returncode == 0 else 'FAIL', str(out/(name+'.log')), flush=True)
    if result.returncode:
        # Make cold-run failures inspectable in CI; retain the complete local log.
        print('\n'.join((out/(name+'.log')).read_text(encoding='utf-8', errors='replace').splitlines()[-60:]), flush=True)
        sys.exit(result.returncode)
