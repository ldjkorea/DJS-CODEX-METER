"""Exact STEP 3 presentation/surface hooks. Historical fixtures remain immutable.
The new live source behavior is separately tested by verify-rpg-step3/native.
"""
from pathlib import Path
import json
def strip_rpg3(name,text):
    if Path(name).name=='AndroidManifest.xml' and 'wear' in Path(name).parts:return text
    patches=json.loads(Path(__file__).with_name('rpg3-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected STEP 3 hook')
        text=text.replace(after,before)
    return text
