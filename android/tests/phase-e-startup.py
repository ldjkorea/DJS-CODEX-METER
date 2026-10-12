"""Measure only the owned emulator QA package. Never stop the production app."""
from pathlib import Path
import json, os, re, statistics, subprocess, sys, time

root = Path(__file__).resolve().parents[1]
adb = str(Path(os.environ['ANDROID_HOME'])/'platform-tools/adb.exe')
serial = sys.argv[1]
assert serial.startswith('emulator-')
package = 'dev.bennett.codexmeter.rpg3qa'
def shell(*args):
    return subprocess.check_output([adb, '-s', serial, 'shell', *args], text=True, encoding='utf-8')
assert shell('getprop', 'ro.kernel.qemu').strip() == '1'
version = re.search(r'versionName=(\S+)', shell('dumpsys', 'package', package)).group(1)
assert version in ['2.8.30', '2.8.50']
component = package+'/dev.bennett.codexmeter.MainActivity'
results = {'version': version, 'scope': 'Owned QA emulator / synthetic account', 'metrics': {'cold': 'am start TotalTime', 'warm': 'am start WaitTime (task foreground, not first-frame time)'}, 'cold': [], 'warm': []}
for _ in range(5):
    shell('am', 'force-stop', package)
    for kind in ['cold', 'warm']:
        if kind == 'warm':
            shell('input', 'keyevent', 'KEYCODE_HOME')
            time.sleep(.2)
        value = shell('am', 'start', '-W', '-n', component)
        match = re.search(('TotalTime' if kind == 'cold' else 'WaitTime')+r':\s+(\d+)', value)
        assert match, value
        results[kind].append(int(match.group(1)))
        time.sleep(.3)
for kind in ['cold', 'warm']:
    results[kind+'_median_ms'] = statistics.median(results[kind])
out = root/'build'/('phase-e-startup-'+version+'.json')
out.write_text(json.dumps(results, indent=2)+'\n', encoding='utf-8')
print(json.dumps(results))
