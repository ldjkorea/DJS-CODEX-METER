from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET,json
r=Path(__file__).resolve().parents[2];a=r/'android';out=a/'build/widget-simplification';out.mkdir(parents=True,exist_ok=True)
jar=a/'build/selftest/json.jar'
if not jar.exists():
 jars=list(a.glob('build/**/json*.jar'));assert jars,'Run existing self tests first';jar=jars[0]
jdk=Path(os.environ['JAVA_HOME'])/'bin';shared=a/'shared/src/main/java/dev/bennett/codexmeter'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',str(jar),'-d',str(out),str(shared/'TaskStatusData.java'),str(Path(__file__).with_name('WidgetSimplificationSelfTest.java'))],check=True)
subprocess.run([str(jdk/'java'),'-cp',os.pathsep.join(map(str,[out,jar])),'dev.bennett.codexmeter.WidgetSimplificationSelfTest'],check=True)
ns='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(a/'app/src/main/AndroidManifest.xml');receivers={n.get(ns+'name') for n in manifest.findall('.//receiver')}
for name in ['CodexUsageWidget','EvolutionWidget','TaskStatusWidget','SamsungLockSquareWidget','SamsungLockWideWidget','SamsungLockRingsSquareWidget','SamsungLockRingsWideWidget','SamsungLockDialsSquareWidget','SamsungLockDialsWideWidget','SamsungLockBarsSquareWidget','SamsungLockBarsWideWidget','SamsungLockFiveHourWidget','SamsungLockWeeklyWidget']:
 assert 'dev.bennett.codexmeter.'+name in receivers,name
for folder in ['values','values-ko']:
 keys={n.get('name') for n in ET.parse(a/f'app/src/main/res/{folder}/widget_simplification.xml').getroot()}
 if folder=='values':english=keys
 else:assert keys==english
for layout in ['widget_rings','widget_rings_four','widget_battery_list']:
 root=ET.parse(a/f'app/src/main/res/layout/{layout}.xml').getroot();status=next(n for n in root.iter() if n.get(ns+'id')=='@+id/updated_label');assert status.get(ns+'layout_width')=='match_parent' and status.get(ns+'layout_height')=='12dp'
assert ET.parse(a/'app/src/main/res/xml/evolution_widget_info.xml').getroot().get(ns+'updatePeriodMillis')=='0'
print('13 provider identities preserved, bilingual guide and visible compact freshness labels passed. Native layout/update testing is separate.')
