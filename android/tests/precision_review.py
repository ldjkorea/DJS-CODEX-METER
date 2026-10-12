"""Reverse exact display preference/formatting edits; keep historical core fixtures immutable."""
from pathlib import Path
import json

def strip_precision(name, text):
    from phase_b_review import strip_phase_b
    text=strip_phase_b(name,text)
    from phase_a_review import strip_phase_a
    text=strip_phase_a(name,text)
    from widget_review import strip_widget
    text=strip_widget(name,text)
    patches=json.loads(Path(__file__).with_name('precision-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected precision preference/display hook')
        text=text.replace(after,before)
    return text
