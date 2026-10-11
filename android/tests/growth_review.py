"""Exact growth presentation hooks against 75be530; original fixtures stay unchanged."""
from pathlib import Path
import json

def strip_growth(name,text):
    patches=json.loads(Path(__file__).with_name('growth-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected growth display hook')
        text=text.replace(after,before)
    return text
