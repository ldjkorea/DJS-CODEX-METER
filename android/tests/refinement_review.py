"""Exact authorized v25 billing restore and private backup entry hooks; immutable fixtures remain."""
from pathlib import Path
import json
def strip_refinement(name,text):
    if Path(name).name=='AndroidManifest.xml' and 'wear' not in Path(name).parts:
        hook='        <activity android:name="dev.bennett.codexmeter.RpgBackupActivity" android:exported="false"/>\n'
        assert text.count(hook)==1
        text=text.replace(hook,'')
    patches=json.loads(Path(__file__).with_name('refinement-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected v25 backup/billing hook')
        text=text.replace(after,before)
    return text
