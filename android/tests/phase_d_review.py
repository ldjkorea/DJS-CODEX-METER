"""Reverse exact Phase D patches only; retain all historical fixtures."""
from pathlib import Path
import json

def strip_phase_d(name,text):
    patches=json.loads(Path(__file__).with_name('phase-d-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unreviewed Phase D change')
        text=text.replace(after,before)
    return text
