"""Gate reviewed promotion; all source/JVM failures are blocking (no known-failure bypass)."""
from pathlib import Path
import argparse,os,subprocess,sys,json,shutil
p=argparse.ArgumentParser();p.add_argument('--bash',default=shutil.which('bash'));p.add_argument('--apk',action='append',default=[]);p.add_argument('--release-json',type=Path);p.add_argument('--focused',action='store_true',help='Only this follow-up source/JVM scope; does not certify the full historical suite');a=p.parse_args()
root=Path(__file__).resolve().parents[2];out=root/('android/build/stable311-validation-focused' if a.focused else 'android/build/stable311-validation');out.mkdir(parents=True,exist_ok=True);results=[]
def summary(completed=False):
 (out/'RESULTS.json').write_text(json.dumps({'checks':results,'focused':a.focused,'completed':completed,'required_gates_passed':completed and all(x['passed'] for x in results),'all_legacy_checks_passed':None if a.focused else completed and all(x['passed'] for x in results)},indent=2),encoding='utf-8')
summary()
def run(name,cmd):
 with (out/(name+'.log')).open('w',encoding='utf-8') as log:r=subprocess.run(cmd,cwd=root,stdout=log,stderr=subprocess.STDOUT)
 text=(out/(name+'.log')).read_text(encoding='utf-8',errors='replace')
 results.append({'name':name,'exit_code':r.returncode,'passed':r.returncode==0})
 summary()
 print(name,'PASS' if r.returncode==0 else 'FAIL',flush=True)
 if r.returncode:print(text[-7000:],flush=True);raise SystemExit(r.returncode)
assert a.bash
run('current-contracts',[sys.executable,'android/tests/run-stable311-checks.py'])
run('canonical-production',[sys.executable,'android/tests/verify-canonical312-production.py'])
run('promotion-scope',[sys.executable,'android/tests/verify-stable311-source.py'])
run('release-validation-negative',[sys.executable,'android/tests/test_release_validation.py'])
run('response-bounds',[sys.executable,'android/tests/verify-release-response-bounds.py',*(['--release-json',str(a.release_json)] if a.release_json else [])])
run('download-policy',[sys.executable,'android/tests/verify-update-download-policy.py'])
run('release-catalog',[sys.executable,'android/tests/verify-ui-polish.py','--catalog-only'])
if a.focused:
 run('insights',[sys.executable,'android/tests/verify-insights.py'])
else:
 run('legacy-regression',[a.bash,'android/run-tests.sh'])
run('legacy-localization',[sys.executable,'android/tests/verify-localization.py'])
run('publication',[sys.executable,'android/tests/verify-publication.py',*a.apk])
run('public-source',[sys.executable,'android/tests/verify-public-source.py'])
summary(completed=True)
print(('Focused source/JVM gates passed; full historical suite NOT certified.' if a.focused else 'Stable311 required gates passed; all legacy source/JVM checks passed.')+' Public budget requires --release-json.',flush=True)
