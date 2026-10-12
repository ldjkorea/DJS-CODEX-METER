from pathlib import Path
import os,subprocess,xml.etree.ElementTree as ET,re
root=Path(__file__).resolve().parents[1];out=root/'build/growth-ux-tests';out.mkdir(parents=True,exist_ok=True)
shared=root/'shared/src/main/java/dev/bennett/codexmeter';jdk=Path(os.environ['JAVA_HOME'])/'bin'
cp=os.pathsep.join(map(str,[out,root/'build/rpg-step2',root/'build/rpg-step1',root/'build/subscription-store-verification/classes',root/'build/evolution-verification',root/'build/insight-verification/classes',root/'build/test-libs/json-20250517.jar']))
subprocess.run([jdk/'javac','-encoding','UTF-8','-cp',cp,'-d',out,shared/'RpgGrowthFacts.java',Path(__file__).with_name('RpgGrowthUxSelfTest.java')],check=True)
subprocess.run([jdk/'java','-ea','-cp',cp,'dev.bennett.codexmeter.RpgGrowthUxSelfTest'],check=True)
en={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values/growth_strings.xml').getroot()}
ko={e.get('name'):e.text for e in ET.parse(root/'app/src/main/res/values-ko/growth_strings.xml').getroot()}
assert en.keys()==ko.keys() and all(en.values()) and all(ko.values())
for key in en:assert sorted(re.findall(r'%\d+\$[sd]',en[key]))==sorted(re.findall(r'%\d+\$[sd]',ko[key])),key
# The original 41 definitions, thresholds, bonuses and bilingual titles are unchanged.
old=subprocess.check_output(['git','show','75be530:android/shared/src/main/java/dev/bennett/codexmeter/RpgCatalog.java'],cwd=root.parent).decode('utf-8')
new=(shared/'RpgCatalog.java').read_text(encoding='utf-8');prefix=new.split('        add(list,"tokens_500m"')[0].replace('    public static final String REVISION="growth-ux-2";\n','')
assert prefix==old.split('        ALL=')[0]
app=root/'app/src/main/java/dev/bennett/codexmeter'
ui=(app/'RpgGrowthUi.java').read_text(encoding='utf-8')
assert 'if(mode.equals("titles"))' in ui and 'RpgShowcase.selected' in ui and 'new RpgAchievementMark' in ui
assert 'TierTheme.frame(card,TierTheme.tier(a),dark)' in ui
wave=(app/'UsageWaveView.java').read_text(encoding='utf-8');assert 'tierAccent' in wave and 'acceleratedWarning' in wave and 'ui_accelerated_usage_warning' in wave and 'fillPaint.setColor(warning ?' not in wave
assert 'codex_rpg_showcase_v1' in (app/'MainActivity.java').read_text(encoding='utf-8')
print(f'Growth UI/source contracts and {len(en)} Korean/English resource keys passed; native interactions are tested separately.')
