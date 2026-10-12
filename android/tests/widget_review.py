"""Reverse only reviewed v30 widget presentation hooks; historical fixtures stay immutable."""
from pathlib import Path
import json

def strip_widget(name, text):
    if Path(name).name=='AndroidManifest.xml' and 'wear' in Path(name).parts:
        return text
    patches=json.loads(Path(__file__).with_name('widget-reviewed-patches.json').read_text(encoding='utf-8'))
    for before, after in reversed(patches.get(Path(name).name, [])):
        assert text.count(after)==1, (name, 'Unexpected v30 widget presentation hook')
        text=text.replace(after, before)
    return text
