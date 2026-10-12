"""Same owned emulator, same synthetic stored data, install -r only. Raw timings, not battery claims."""
from pathlib import Path
import os, subprocess, re, json, statistics
root=Path(__file__).resolve().parents[1]
adb=Path(os.environ['ANDROID_HOME'])/'platform-tools/adb.exe'
serial='emulator-5554'; package='dev.bennett.codexmeter.rpg3qa'
def shell(*a): return subprocess.check_output([str(adb),'-s',serial,'shell',*a],text=True,encoding='utf-8')
assert shell('getprop','ro.kernel.qemu').strip()=='1'
results={'scope':'isolated emulator / synthetic account / same configuration / no physical battery measurement'}
for label,apk in [('before',root/'build/collection-restart-qa/qa.apk'),('after',root/'build/stability-qa/qa.apk')]:
    assert apk.is_file()
    subprocess.run([str(adb),'-s',serial,'install','-r',str(apk)],check=True)
    measurements={}
    # Secondary activities are non-exported; never weaken the manifest to time them.
    for activity in ['MainActivity']:
        raw=[]
        for i in range(5):
            shell('am','force-stop',package)
            output=shell('am','start','-W','-n',package+'/dev.bennett.codexmeter.'+activity)
            found=re.search(r'TotalTime:\s+(\d+)',output);assert found,output;raw.append(int(found.group(1)))
        measurements[activity]={'am_start_total_ms':raw,'median_ms':statistics.median(raw)}
    memory=shell('dumpsys','meminfo',package)
    (root/'build'/('stability-memory-'+label+'.txt')).write_text(memory,encoding='utf-8')
    results[label]=measurements
(root/'build/stability-performance.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
print(json.dumps(results))
