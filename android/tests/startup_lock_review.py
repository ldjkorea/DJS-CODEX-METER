"""Exact startup hotfix adapter; historical fixtures and storage contracts stay unchanged."""
from pathlib import Path
import json

def strip_startup_lock(name, text):
    patches=json.loads(Path(__file__).with_name('startup-lock-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name, [])):
        assert text.count(after)==1,(name,'Unexpected startup lock patch')
        text=text.replace(after,before)
    return text
