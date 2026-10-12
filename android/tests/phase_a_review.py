"""Reverse only the exact calculation-history hook; historical fixtures remain immutable."""
from pathlib import Path
import json

def strip_phase_a(name,text):
    patches=json.loads(Path(__file__).with_name('phase-a-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected Phase A calculation history hook')
        text=text.replace(after,before)
    return text
