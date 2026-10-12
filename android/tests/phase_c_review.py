"""Reverse only exact reviewed Phase C edits. Historical source fixtures are immutable."""
from pathlib import Path
import json
def strip_phase_c(name,text):
    patches=json.loads(Path(__file__).with_name('phase-c-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unreviewed Phase C change')
        text=text.replace(after,before)
    return text
