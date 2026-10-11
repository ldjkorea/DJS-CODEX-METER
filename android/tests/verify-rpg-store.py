"""Production account DAO + real SQLite; auth/ledger/Android context are synthetic fixtures.
No user's database, key, session or network is accessed.
"""
from pathlib import Path
import os, subprocess, sys
os.environ['PYTHONUTF8']='1' # Java bridge sends UTF-8 Korean catalog metadata on Windows.

root=Path(__file__).resolve().parents[1];out=root/'build/rpg-store';out.mkdir(parents=True,exist_ok=True)
source=root/'build/subscription-store-verification/fixtures'
sql=root/'build/sqlite-verification/fixtures'
fixtures={}
for name in ['android/annotation/SuppressLint.java','android/content/SharedPreferences.java','android/content/Context.java','dev/bennett/codexmeter/SecureTokenStore.java','dev/bennett/codexmeter/AppPreferences.java','dev/bennett/codexmeter/UsageApi.java']:
    fixtures[name]=(source/name).read_text(encoding='utf-8')
fixtures['android/content/SharedPreferences.java']=fixtures['android/content/SharedPreferences.java'].replace('Editor edit();','java.util.Map<String,?> getAll();Editor edit();')
fixtures['android/content/Context.java']=fixtures['android/content/Context.java'].replace('public SharedPreferences getSharedPreferences', '''public java.io.File getFilesDir(){return directory.toFile();}
 public java.io.File getDatabasePath(String name){return directory.resolve(name).toFile();}
 public SharedPreferences getSharedPreferences''').replace('public Editor edit(){','public java.util.Map<String,?> getAll(){return read().toMap();}public Editor edit(){')
fixtures['dev/bennett/codexmeter/AppPreferences.java']=fixtures['dev/bennett/codexmeter/AppPreferences.java'].replace('static UsageSnapshot current;','static String error="";static String getLastError(android.content.Context c){return error;}static UsageSnapshot current;')
for name in ['android/content/ContentValues.java','android/database/Cursor.java','android/database/sqlite/SQLiteDatabase.java','android/database/sqlite/SQLiteOpenHelper.java']:
    text=(sql/name).read_text(encoding='utf-8')
    if name.endswith('SQLiteOpenHelper.java'):
        text=text.replace('public abstract class SQLiteOpenHelper {','public abstract class SQLiteOpenHelper implements AutoCloseable {').replace("AND name='observations'","AND name NOT LIKE 'sqlite_%'")
    fixtures[name]=text
fixtures['dev/bennett/codexmeter/UsageLedgerDatabase.java']='''package dev.bennett.codexmeter;
import java.util.*;import java.io.*;import java.nio.file.*;
class UsageLedgerDatabase {static long floor;
 static long lanFloor(android.content.Context c){return floor;}
 static void recoveryCopy(android.content.Context c,File folder)throws Exception {File f=c.getDatabasePath("original-fixture.db");if(f.exists())Files.copy(f.toPath(),new File(folder,f.getName()).toPath());}
 static class Data {final List<LedgerRecord> records=new ArrayList<>();final List<LedgerAggregation.Day> days=new ArrayList<>();final List<Event> events=new ArrayList<>();}
 static class Event {String type,origin;long at;}}
'''
fixtures['dev/bennett/codexmeter/TierTheme.java']='package dev.bennett.codexmeter; class TierTheme {static boolean effects(android.content.Context c){return true;}}'
fixtures['android/content/SharedPreferences.java']=fixtures['android/content/SharedPreferences.java'].replace('Editor edit();','boolean getBoolean(String k,boolean f);boolean contains(String k);Editor edit();').replace('boolean commit();','Editor putBoolean(String k,boolean v);Editor remove(String k);boolean commit();')
fixtures['android/content/Context.java']=fixtures['android/content/Context.java'].replace('public Editor edit(){','public boolean getBoolean(String k,boolean f){return read().optBoolean(k,f);}public boolean contains(String k){return read().has(k);}public Editor edit(){').replace('public boolean commit(){','public Editor putBoolean(String k,boolean v){try{values.put(k,v);}catch(Exception e){throw new RuntimeException(e);}return this;}public Editor remove(String k){values.remove(k);return this;}public boolean commit(){')
files=[]
for name,text in fixtures.items():
    path=out/'fixtures'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8');files.append(str(path))
app=root/'app/src/main/java/dev/bennett/codexmeter';shared=root/'shared/src/main/java/dev/bennett/codexmeter'
files += [str(app/(name+'.java')) for name in ['AuthTokens','SubscriptionStore','RpgProgressStore','RpgShowcase']]
files += [str(shared/(name+'.java')) for name in ['RpgTokens','RpgTier','RpgCatalog','RpgTitleCatalog','RpgGrowthFacts','RpgArchive','RpgBackupCodec','RpgEngine','MonthlyLedger','AccountTier','AccountProgress','FunInsights','TierEvolution','SubscriptionCost','LedgerPeriods']]
files += [str(Path(__file__).with_name(n)) for n in ['RpgStoreSelfTest.java','RpgRefinementSelfTest.java']]+[str(shared/'RpgCollectionFacts.java')]
cp=os.pathsep.join(map(str,[out,root/'build/rpg-step1',root/'build/insight-verification/classes',root/'build/evolution-verification',root/'build/test-libs/json-20250517.jar']))
jdk=Path(os.environ['JAVA_HOME'])/'bin'
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(out),*files],check=True)
subprocess.run([str(jdk/'java'),'-ea','-Dledger.python='+sys.executable,'-Dledger.bridge='+str(root/'build/sqlite-verification/sqlite_bridge.py'),'-cp',cp,'dev.bennett.codexmeter.RpgStoreSelfTest'],check=True,timeout=90)

subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.RpgRefinementSelfTest'],check=True,timeout=90)

import xml.etree.ElementTree as ET,re
translations=[]
for language in ['values','values-ko']:
    translations.append({e.get('name'):e.text for e in ET.parse(root/'app/src/main/res'/language/'refinement_strings.xml').getroot()})
assert translations[0].keys()==translations[1].keys() and all(translations[0].values()) and all(translations[1].values())
for key in translations[0]:assert sorted(re.findall(r'%\d+\$[sd]',translations[0][key]))==sorted(re.findall(r'%\d+\$[sd]',translations[1][key])),key
print(f'Encrypted backup/demotion/independent title tests and {len(translations[0])} bilingual refinement strings passed.')
