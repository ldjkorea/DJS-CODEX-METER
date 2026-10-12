"""Scan the Git index, not ignored runtime/signing files; never print secret values."""
from pathlib import Path
import json, re, subprocess

root = Path(__file__).resolve().parents[2]
paths = subprocess.check_output(['git', 'ls-files', '-s', '-z'], cwd=root).split(b'\0')
forbidden = {'.p12', '.pfx', '.jks', '.keystore', '.key', '.pem', '.apk', '.db', '.sqlite', '.sqlite3'}
patterns = [rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
            rb'gh[pousr]_[A-Za-z0-9]{30,}', rb'github_pat_[A-Za-z0-9_]{40,}',
            rb'\bsk-(?:proj-)?[A-Za-z0-9_-]{40,}']
errors = []
count = 0
with subprocess.Popen(['git', 'cat-file', '--batch'], cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE) as blobs:
    for raw in paths:
        if not raw:
            continue
        metadata, name = raw.split(b'\t', 1)
        mode, oid, stage = metadata.split()
        assert stage == b'0', 'Unresolved Git index'
        name = name.decode('utf-8')
        path = Path(name)
        if path.suffix.lower() in forbidden or '.local-signing' in path.parts or path.name in ['auth.json', 'password']:
            errors.append((name, 'private/runtime file is tracked'))
        blobs.stdin.write(oid+b'\n'); blobs.stdin.flush()
        header = blobs.stdout.readline().split()
        assert header[1] == b'blob'
        data = blobs.stdout.read(int(header[2])); assert blobs.stdout.read(1) == b'\n'
        if b'\0' not in data:
            for pattern in patterns:
                if re.search(pattern, data):
                    errors.append((name, 'credential/private key pattern'))
        count += 1
    blobs.stdin.close()
assert not errors, errors
print(json.dumps({'indexed_files_scanned': count, 'known_secret_patterns': 'absent',
                  'runtime_and_private_keys': 'not tracked', 'scope': 'index, not a complete historical security audit'}))
