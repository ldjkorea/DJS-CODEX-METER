"""Reverse exact reviewed Phase B DAO edits; immutable historical fixtures stay protected."""
from pathlib import Path
import json

def strip_phase_b(name,text):
    patches=json.loads(Path(__file__).with_name('phase-b-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected Phase B DAO change')
        text=text.replace(after,before)
    return text
