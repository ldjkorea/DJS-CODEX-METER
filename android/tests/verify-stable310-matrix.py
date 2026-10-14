from pathlib import Path
import os,subprocess
adb=Path(os.environ['ANDROID_HOME'])/'platform-tools/adb.exe';serial='emulator-5592';package='dev.bennett.codexmeter';out=Path(__file__).resolve().parents[1]/'build/stable310-matrix';out.mkdir(exist_ok=True)
def run(*a):return subprocess.check_output([str(adb),'-s',serial,*a],text=True,encoding='utf-8',errors='replace',timeout=90)
assert run('emu','avd','name').splitlines()[0]=='gpthudstable310'
try:
 for locale in ['ko','en']:
  run('shell','cmd','locale','set-app-locales',package,'--locales',locale)
  for font in ['1.0','1.3','1.6','2.0']:
   label=locale+'-'+font;run('shell','settings','put','system','font_scale',font)
   result=run('shell','am','instrument','-r','-w',package+'promotetest/dev.bennett.codexmeter.Stable310UiNativeTest');(out/(label+'.log')).write_text(result,encoding='utf-8');assert 'INSTRUMENTATION_CODE: -1' in result,result
   folder=out/label;folder.mkdir(exist_ok=True)
   for name in ['home','catalog']:run('pull','/sdcard/Android/data/'+package+'/files/beta996-ui-captures/'+name+'.png',str(folder/(name+'.png')))
   print(label+' PASS '+result.split('stream=')[1].splitlines()[0],flush=True)
  run('shell','wm','user-rotation','lock','1');label=locale+'-landscape';result=run('shell','am','instrument','-r','-w',package+'promotetest/dev.bennett.codexmeter.Stable310UiNativeTest');(out/(label+'.log')).write_text(result,encoding='utf-8');assert 'INSTRUMENTATION_CODE: -1' in result,result
  folder=out/label;folder.mkdir(exist_ok=True);run('pull','/sdcard/Android/data/'+package+'/files/beta996-ui-captures/home.png',str(folder/'home.png'));print(label+' PASS',flush=True);run('shell','wm','user-rotation','free')
finally:
 run('shell','settings','put','system','font_scale','1.0');run('shell','wm','user-rotation','free');run('shell','cmd','locale','set-app-locales',package,'--locales','ko')
