from pathlib import Path
import json

def strip_collection_restart(name,text):
    if str(name).replace("\\","/").endswith("wear/build.gradle.kts"):return text
    if Path(name).name=="AndroidManifest.xml" and "wear" in Path(name).parts:return text
    patches=json.loads(Path(__file__).with_name("collection-restart-reviewed-patches.json").read_text(encoding="utf-8"))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,"Unreviewed collection restart edit")
        text=text.replace(after,before)
    return text
