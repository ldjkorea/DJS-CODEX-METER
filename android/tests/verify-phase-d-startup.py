"""Process-cold/resume launch timing, only an existing owned QA emulator package."""
from pathlib import Path
import os,subprocess,re,json,sys
root=Path(__file__).resolve().parents[1];adb=Path(os.environ['ANDROID_HOME'])/'platform-tools/adb.exe';serial=sys.argv[1];assert serial.startswith('emulator-')
base=[str(adb),'-s',serial];shell=lambda *a:subprocess.check_output(base+['shell',*a],text=True,encoding='utf-8');package='dev.bennett.codexmeter.rpg3qa'
assert shell('getprop','ro.kernel.qemu').strip()=='1' and 'package:'+package in shell('pm','list','packages',package)
results={}
for name,apk in [('before',root/'build/phase-c-qa/qa.apk'),('after',root/'build/phase-d-qa/qa.apk')]:
 subprocess.run(base+['install','-r',str(apk)],check=True);cold=[];warm=[]
 for _ in range(3):
  shell('am','force-stop',package);out=shell('am','start','-W','-n',package+'/dev.bennett.codexmeter.MainActivity')
  m=re.search(r'TotalTime: (\d+)',out);assert m,out;cold.append(int(m.group(1)))
  shell('input','keyevent','KEYCODE_HOME');out=shell('am','start','-W','-n',package+'/dev.bennett.codexmeter.MainActivity');m=re.search(r'(?:TotalTime|WaitTime): (\d+)',out);assert m,out;warm.append(int(m.group(1)))
 results[name]={'cold_am_total_ms':cold,'resume_am_ms':warm}
(root/'build/phase-d-startup.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8');print(json.dumps(results,indent=2))
