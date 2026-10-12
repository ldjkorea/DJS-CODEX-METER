"""Reverse the exact isolated QA build option; no historical fixture regeneration."""
from pathlib import Path
import json

def strip_phase_e(name, text):
    if str(name).replace('\\', '/').endswith('wear/build.gradle.kts'):
        return text
    patches = json.loads(Path(__file__).with_name('phase-e-reviewed-patches.json').read_text(encoding='utf-8'))
    for before, after in reversed(patches.get(Path(name).name, [])):
        assert text.count(after) == 1, (name, 'Unreviewed Phase E edit')
        text = text.replace(after, before)
    return text
