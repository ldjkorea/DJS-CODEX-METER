"""Exact additive step 2 hooks; historical source fixtures stay immutable."""
from pathlib import Path
import json
def strip_rpg2(name,text):
    patches=json.loads(Path(__file__).with_name('rpg2-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected RPG step 2 hook')
        text=text.replace(after,before)
    return text
