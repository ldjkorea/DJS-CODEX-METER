"""Compare a published APK and language-trimmed candidate without touching user data."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from zipfile import ZipFile

before, after = map(Path, sys.argv[1:3])
aapt = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0' / ('aapt2.exe' if os.name == 'nt' else 'aapt2')

def resources(apk):
    dump = subprocess.check_output([str(aapt), 'dump', 'resources', str(apk)], encoding='utf-8')
    entries = {}
    resource = key = None
    for line in dump.splitlines():
        if line.startswith('    resource '):
            resource, key = line.strip(), None
        elif line.startswith('      ('):
            key = resource, line.split(')', 1)[0][7:]
            entries[key] = line
        elif key and line.startswith('        '):
            entries[key] += '\n' + line
    return entries

old, new = resources(before), resources(after)
assert old and new
assert all(key in old and old[key] == value for key, value in new.items()), 'Resource value changed'
assert {key[0] for key in old} == {key[0] for key in new}, 'Resource ID removed'
removed = old.keys() - new.keys()
assert removed
# Locale-specific dependency entries may fall back to default English or Korean.
# Every default, Korean and plain English entry must remain byte-for-byte equivalent.
assert all(re.match(r'^(?:[a-z]{2,3}|b\+[a-z]{2,3})(?:-|\+|$)', key[1])
           and key[1] not in ('', 'en', 'ko') for key in removed)
with ZipFile(before) as a, ZipFile(after) as b:
    assert set(b.namelist()) <= set(a.namelist())
    unchanged = [name for name in b.namelist()
                 if name != 'resources.arsc' and not name.startswith('META-INF/')]
    assert all(a.read(name) == b.read(name) for name in unchanged), 'Code, manifest or asset changed'
    assert all(name.startswith(('res/', 'META-INF/'))
               for name in set(a.namelist()) - set(b.namelist()))
assert after.stat().st_size < before.stat().st_size
print(json.dumps({'before_bytes': before.stat().st_size, 'after_bytes': after.stat().st_size,
                  'saved_bytes': before.stat().st_size - after.stat().st_size,
                  'unchanged_zip_entries': len(unchanged), 'retained_resource_values': len(new),
                  'removed_locale_values': len(removed), 'resource_ids_preserved': True,
                  'sha256': hashlib.sha256(after.read_bytes()).hexdigest()}, indent=2))
