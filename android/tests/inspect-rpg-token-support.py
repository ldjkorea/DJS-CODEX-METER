"""Explicit read-only investigation. Whitelist token metadata; never print/store transcripts/auth."""
import json, os, hashlib, subprocess, threading, queue, time
from pathlib import Path
from datetime import datetime
root=Path(__file__).resolve().parents[1]
out=root/'build/rpg2-evidence';out.mkdir(parents=True,exist_ok=True)
home=Path(os.environ.get('CODEX_HOME',str(Path.home()/'.codex')))
files=sorted((home/'sessions').rglob('rollout-*.jsonl'),key=lambda p:p.stat().st_mtime,reverse=True)[:24]
types={}; fields=set(); session_fields=set(); totals=[]; observed_dates=set(); first=None; last=None
for path in files:
    maximum=0
    with path.open(encoding='utf-8') as stream:
        for line in stream:
            try:e=json.loads(line)
            except (ValueError,UnicodeError):continue
            p=e.get('payload') or {}
            if e.get('type')=='session_meta':session_fields.update(p.keys())
            if e.get('type')!='event_msg':continue
            kind=p.get('type','');types[kind]=types.get(kind,0)+1
            if kind=='token_count' and isinstance(p.get('info'),dict):
                info=p['info']; t=info.get('total_token_usage') or {};fields.update(t.keys())
                maximum=max(maximum,int(t.get('total_tokens',0)))
                at=e.get('timestamp','');observed_dates.add(at[:10]);first=min(first or at,at);last=max(last or at,at)
    totals.append(maximum)
report={'inspected_files':len(files),'token_count_events':types.get('token_count',0),'token_usage_record_events':types.get('token_usage_record',0),'token_fields':sorted(fields),'session_metadata_fields':sorted(session_fields),'account_attribution':'unverified; not awarded','local_session_highwater_total':sum(totals),'utc_days_with_events':len(observed_dates),'first_event':first,'last_event':last,'scope':'24 recent local files; high-water estimate; not account lifetime/billed tokens'}
# Launch a separate read-only protocol client. No turn/start or auth update calls.
cmd=Path(os.environ['APPDATA'])/'npm/codex.cmd'
proc=subprocess.Popen([str(cmd),'app-server'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True,encoding='utf-8')
messages=queue.Queue()
def read():
    for line in proc.stdout:
        try:messages.put(json.loads(line))
        except ValueError:pass
threading.Thread(target=read,daemon=True).start()
def call(method,params=None,rid=1):
    request={'id':rid,'method':method}
    if params is not None:request['params']=params
    proc.stdin.write(json.dumps(request)+'\n');proc.stdin.flush();deadline=time.monotonic()+25
    while time.monotonic()<deadline:
        try:r=messages.get(timeout=1)
        except queue.Empty:continue
        if r.get('id')==rid:return r
    return {'error':{'code':'timeout'}}
try:
    call('initialize',{'clientInfo':{'name':'gpt_hud_rpg_readonly','version':'1'},'capabilities':{'experimentalApi':True}},1)
    proc.stdin.write('{"method":"initialized"}\n');proc.stdin.flush()
    account=call('account/read',{'refreshToken':False},2)
    result=call('account/usage/read',rid=3)
    a=(account.get('result') or {}).get('account') or {}
    report['account_mode']=a.get('type');report['account_identity_fields']=sorted(a.keys())
    # No email/account id/auth material leaves this process. Fields only.
    if 'error' in result:report['official_usage']={'status':'unavailable','error_code':result['error'].get('code')}
    else:
        r=result.get('result') or {};summary=r.get('summary') or {};days=r.get('dailyUsageBuckets')
        safe={k:summary.get(k) for k in ['lifetimeTokens','peakDailyTokens','longestRunningTurnSec','currentStreakDays','longestStreakDays']}
        buckets=[{'startDate':d.get('startDate'),'tokens':d.get('tokens')} for d in (days or [])]
        report['official_usage']={'status':'observed','summary':safe,'dailyUsageBuckets':buckets,'daily_zone':'service startDate; timezone unconfirmed'}
finally:
    proc.terminate()
    try:proc.wait(timeout=5)
    except subprocess.TimeoutExpired:proc.kill()
(out/'token-support.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
