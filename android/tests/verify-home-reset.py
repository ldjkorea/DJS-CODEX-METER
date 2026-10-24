"""Focused no-download formatter tests; native layout is verified separately."""
from pathlib import Path
import json,os,re,subprocess,xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
out=root/'build/stable313/unit';out.mkdir(parents=True,exist_ok=True)
app=root/'app/src/main/java/dev/bennett/codexmeter'
names=['UsageFormat','WidgetOptions','FiveHourResetState','FiveHourResetDisplay']
keys=sorted(set(re.findall(r'R.string.(\w+)', ''.join((app/(n+'.java')).read_text(encoding='utf-8') for n in names)))|{'home_quota_reset_schedule'})
def strings(folder):
    result={}
    for p in folder.glob('*.xml'):
        for node in ET.parse(p).getroot().findall('string'):
            value=node.text or ''
            if value.startswith('"') and value.endswith('"'):value=value[1:-1]
            result[node.attrib['name']]=value.replace("\\'","'").replace('\\"','"').replace('\\n','\n')
    return result
en=strings(root/'app/src/main/res/values');ko=strings(root/'app/src/main/res/values-ko')
q=lambda s:json.dumps(s,ensure_ascii=False)
sources={
 'dev/bennett/codexmeter/R.java':'package dev.bennett.codexmeter; public class R {public static class string {'+''.join('public static final int '+k+'='+str(i)+';' for i,k in enumerate(keys))+'}}',
 'android/content/Context.java':'package android.content; public class Context {public final java.util.Locale locale; public boolean hour24; public Context(java.util.Locale l){locale=l;} public String getString(int id){return (locale.getLanguage().equals("ko")?KO:EN)[id];}public String getString(int id,Object... args){return String.format(locale,getString(id),args);} static final String[] KO={'+','.join(q(ko[k]) for k in keys)+'},EN={'+','.join(q(en[k]) for k in keys)+'};}',
 'android/text/format/DateFormat.java':'package android.text.format;public class DateFormat {public static boolean is24HourFormat(android.content.Context c){return c.hour24;}}',
 'dev/bennett/codexmeter/AppPreferences.java':'package dev.bennett.codexmeter;class AppPreferences {static String error="";static String getLastError(android.content.Context c){return error;}}',
 'dev/bennett/codexmeter/RefreshScheduler.java':'package dev.bennett.codexmeter;class RefreshScheduler {static int effectiveRefreshMinutes(android.content.Context c){return 5;}}',
}
files=[]
for name,text in sources.items():
    p=out/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8');files.append(p)
cp=os.pathsep.join(map(str,[out,root/'shared/build/classes/java/main',root/'build/test-libs/json-20250517.jar']))
jdk=Path(os.environ['JAVA_HOME'])/'bin'
files += [app/(n+'.java') for n in names]+[Path(__file__).with_name('HomeResetFormatSelfTest.java')]
subprocess.run([jdk/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',out,*files],check=True)
result=subprocess.check_output([jdk/'java.exe','-ea','-cp',cp,'dev.bennett.codexmeter.HomeResetFormatSelfTest'],text=True)
(out/'result.txt').write_text(result,encoding='utf-8');print(result.strip())
ui=(app/'BetaQuotaUi.java').read_text(encoding='utf-8')
assert 'if(compact)reset.setVisibility' not in ui
assert ui.count('WidgetOptions.RESET_BOTH')==2
assert 'screenWidthDp < 360' in ui
assert 'R.string.home_quota_reset_schedule' in ui
print('Home reset visibility/both-mode/narrow-width source contracts passed; no native UI claim.')
