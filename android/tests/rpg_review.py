"""Exact user-authorized Step 1 hooks; immutable historical fixtures remain untouched."""
from pathlib import Path
import json
def strip_rpg(name,text):
    from precision_review import strip_precision
    text=strip_precision(name,text)
    from refinement_review import strip_refinement
    text=strip_refinement(name,text)
    # Additive private collection activity; no existing exported/auth activity changes.
    if Path(name).name=="AndroidManifest.xml" and "wear" not in Path(name).parts:
        hook='        <activity android:name="dev.bennett.codexmeter.RpgCollectionActivity" android:exported="false"/>\n'
        assert text.count(hook)==1
        text=text.replace(hook,"")
    from growth_review import strip_growth
    text=strip_growth(name,text)
    from rpg3_review import strip_rpg3
    text=strip_rpg3(name,text)
    from rpg2_review import strip_rpg2
    text=strip_rpg2(name,text)
    patches=json.loads(Path(__file__).with_name('rpg-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected RPG hook')
        text=text.replace(after,before)
    return text
