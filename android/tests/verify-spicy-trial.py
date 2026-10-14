from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];app=root/'app/src/main/java/dev/bennett/codexmeter';shared=root/'shared/src/main/java/dev/bennett/codexmeter';out=root/'build/spicy-trial';out.mkdir(parents=True,exist_ok=True)
jar=root/'build/test-libs/json-20250517.jar';jdk=Path(os.environ['JAVA_HOME'])/'bin';cp=str(out)+os.pathsep+str(jar)
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',str(jar),'-d',str(out),str(shared/'SpicyRotation.java'),str(shared/'TaskStatusData.java'),str(Path(__file__).with_name('SpicyTrialSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.SpicyTrialSelfTest',str(root/'app/src/main/assets/spicy-ko.txt')],check=True)
home=(app/'FunHome.java').read_text(encoding='utf-8');cards=(app/'LiveCards.java').read_text(encoding='utf-8');quotes=(app/'SpicyQuotes.java').read_text(encoding='utf-8');main=(app/'MainActivity.java').read_text(encoding='utf-8');lan=(app/'LanSync.java').read_text(encoding='utf-8')
worker_home=home[home.index('WORKER.execute'):]
assert worker_home.index('SpicyQuotes.select')<worker_home.index('home.render')
assert 'stamp.equals(cachedStamp)' in home and 'cachedLive' in home
assert 'now,quote)' in home and 'CoachUi.card' in cards
assert 'SpicyQuotes.select' not in home.split('WORKER.execute',1)[0], 'No catalog work in home fast path'
coach=(app/'CoachUi.java').read_text(encoding='utf-8')
assert 'tone.equals("off")' in coach and 'setPrimaryClip' in coach
assert 'getPrimaryClip' not in coach and 'UsageApi.refresh' not in coach and 'RpgProgressStore.reconcile' not in coach
assert 'getLanguage().equals("ko")' in quotes and 'if(!loaded)' in quotes and '.apply()' in quotes and '.commit()' not in quotes
assert 'isChangingConfigurations()' in main and 'SpicyQuotes.manual()' in main and 'SpicyQuotes.foreground()' in main
assert 'stamp.equals(LiveUsageStore.signature' in home and 'target.isAttachedToWindow()' in home
assert '!TaskStatusStore.enabled(app)||!BUSY.compareAndSet' in lan and '"task-status"' in lan and 'paired.equals(code(app,a))' in lan
assert 'UsageApi.refresh' not in (app/'TaskStatusActivity.java').read_text(encoding='utf-8')
task_request=lan.split('static void taskStatus(Context c,Done done)',1)[1].split('private static JSONObject payload',1)[0]
assert 'target.taskNetworkAllowed' in task_request and 'NetworkCapabilities.TRANSPORT_VPN' in task_request
assert 'LanSyncWire.exchange(target,a' in task_request
history_request=lan.split('static void schedule(Context c,Done done)',1)[1].split('static void taskStatus',1)[0]
assert 'TRANSPORT_WIFI' in history_request and 'TRANSPORT_ETHERNET' in history_request and 'TRANSPORT_VPN' not in history_request
assert 'handler.postDelayed(this,15000)' in (app/'TaskStatusActivity.java').read_text(encoding='utf-8')
assert 'LanSyncUi.show(this,dark)' in (app/'TaskStatusActivity.java').read_text(encoding='utf-8')
assert 'new ComponentName(c,TaskStatusWidget.class)' in (app/'TaskStatusWidget.java').read_text(encoding='utf-8')
for filename in ['TaskStatusActivity.java','TaskStatusWidget.java','TaskStatusStore.java']:
 assert 'approve' not in (app/filename).read_text(encoding='utf-8').lower() and 'ProcessBuilder' not in (app/filename).read_text(encoding='utf-8')
en={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/task_trial.xml').getroot()};ko={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/task_trial.xml').getroot()};assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
print('Actual home/worker/cache wiring, English fallback, snapshot guard, independent OFF-default provider and bilingual trial strings passed (source checks).')
