from pathlib import Path
import os,subprocess
from phase_d_review import strip_phase_d
from collection_restart_review import strip_collection_restart
root=Path(__file__).resolve().parents[1];app=root/'app/src/main/java/dev/bennett/codexmeter';shared=root/'shared/src/main/java/dev/bennett/codexmeter';out=root/'build/hud-tests';out.mkdir(parents=True,exist_ok=True)
jar=root/'build/test-libs/json-20250517.jar';jdk=Path(os.environ['JAVA_HOME'])/'bin';cp=os.pathsep.join(map(str,[out,root/'build/tests',jar]))
sources=[shared/n for n in ['UsageWindow.java','UsagePrecision.java','TaskStatusData.java','LedgerRecord.java']]+[app/'LedgerCapture.java',Path(__file__).with_name('HudSelfTest.java')]
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(out),*map(str,sources)],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.HudSelfTest'],check=True)
main=(app/'MainActivity.java').read_text(encoding='utf-8');analytics=(app/'LedgerAnalyticsActivity.java').read_text(encoding='utf-8');settings=(app/'RecordSettings.java').read_text(encoding='utf-8');home=(app/'LiveCards.java').read_text(encoding='utf-8')
assert main.index('content.addView(crestSlot,new LinearLayout.LayoutParams(-1,-2))')<main.index('LedgerDashboard.addOverview')<main.index('TaskStatusStore.enabled(this)){')<main.index('FunHome.add')
assert 'BetaHomeUi.card(a,dark)' in home
hero=(app/'BetaHomeUi.java').read_text(encoding='utf-8')
assert 'Gravity.CENTER_HORIZONTAL' in hero and 'TierPresentation.NAMES[rank]' in hero and 'setEllipsize' not in hero
assert 'Ui.addSpacer(body,12);addRecords(today);' in analytics and '}else addTrends(today);' in analytics and 'RecordSettings.add(this,body,dark)' in analytics and 'addEvents(rows)' in analytics
assert 'hud_month_pattern' in analytics and 'setContentDescription(getString(R.string.next_previous_month))' in analytics
for value in ['hud_pc_settings','hud_data_settings','hud_privacy','next_export_csv','next_export_json']:assert 'R.string.'+value in settings
assert 'toggle(a,' not in settings.split('static void add(',1)[1].split('static void legacyHub(',1)[0]
job=strip_phase_d('TaskStatusJobService.java',strip_collection_restart('TaskStatusJobService.java',(app/'TaskStatusJobService.java').read_text()));assert '15*60000L' in job and 'JobInfo.NETWORK_TYPE_ANY' in job and 'TaskStatusWidget.class' in job and '!TaskStatusStore.enabled(c)' in job
assert 'UsageApi.' not in job and 'ForegroundService' not in job
print('HUD navigation, records/settings, opt-in widget job and unchanged quota-request path passed (source checks).')
