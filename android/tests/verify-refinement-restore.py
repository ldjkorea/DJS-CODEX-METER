"""Destructive reinstall ONLY on this task's single-account synthetic fixture AVD.
Never accepts a phone serial. Public APKs, real accounts and other AVDs are excluded.
Run after verify-refinement-native.py has populated and checked the production fixture.
"""
from pathlib import Path
import os,subprocess,hashlib,json,sys
root=Path(__file__).resolve().parents[1];out=root/'build/refinement/native';adb=Path(os.environ['ANDROID_HOME'])/'platform-tools/adb.exe'
serial='emulator-5556';pkg='dev.bennett.codexmeter';target=pkg+'.refinementtest/dev.bennett.codexmeter.RpgRefinementNativeTest'
call=lambda *a:subprocess.check_output([str(adb),'-s',serial,*map(str,a)],text=True,encoding='utf-8')
assert call('shell','getprop','ro.kernel.qemu').strip()=='1'
assert 'gpt-hud-refinement-v25' in call('emu','avd','name')
assert (out/'SEED.txt').exists() and (out/'MATRIX_FINAL.json').exists()
call('shell','wm','size','411x1000');call('shell','wm','density','160');call('shell','settings','put','system','font_scale','1');call('shell','cmd','locale','set-app-locales',pkg,'--locales','ko-KR')
old_animation=call('shell','settings','get','global','animator_duration_scale').strip()
def instrument(mode,**args):
    for p in [pkg,'com.google.android.documentsui']:call('shell','am','force-stop',p)
    cmd=['shell','am','instrument','-r','-w','-e','mode',mode]
    for k,v in args.items():cmd+=['-e',k,v]
    text=call(*cmd,target);(out/(mode.upper()+'.txt')).write_text(text,encoding='utf-8');print(text,flush=True)
    assert 'checks passed' in text and 'INSTRUMENTATION_CODE: -1' in text
try:
    call('shell','settings','put','global','animator_duration_scale','0')
    resume_reinstall='--resume-after-reinstall' in sys.argv
    if resume_reinstall:
        assert 'fresh: 4 checks passed' in (out/'FRESH.txt').read_text(encoding='utf-8')
        assert (out/'refinement-reinstall-expected.json').exists()
    elif '--resume-after-interaction' in sys.argv:
        assert 'interaction: 12 checks passed' in (out/'INTERACTION.txt').read_text(encoding='utf-8')
    else:instrument('interaction')
    if not resume_reinstall:
        instrument('saf')
        for name in ['refinement-reinstall-expected.json','refinement-reinstall-name.txt']:
            call('pull','/sdcard/Android/data/'+pkg+'/files/'+name,out/name)
        call('pull','/sdcard/Android/data/'+pkg+'/files/refinement-captures',out/'captures')
    expected=json.loads((out/'refinement-reinstall-expected.json').read_text(encoding='utf-8'))
    assert expected['account']==hashlib.sha256(b'refinement-production-fixture-account').hexdigest(),'Never remove a real account installation'
    name=(out/'refinement-reinstall-name.txt').read_text(encoding='utf-8');assert name.startswith('GPT-HUD-refinement-') and name.endswith('.gpthud')
    assert name in call('shell','ls','/sdcard/Download')
    # Test fixture source/captures/archive have been saved; no user's app is involved.
    if not resume_reinstall:
        assert call('uninstall',pkg).strip()=='Success'
        call('install',root/'app/build/outputs/apk/release/app-release.apk');instrument('fresh')
        call('shell','mkdir','-p','/sdcard/Android/data/'+pkg+'/files')
        call('push',out/'refinement-reinstall-expected.json','/sdcard/Android/data/'+pkg+'/files/refinement-reinstall-expected.json')
    instrument('importfile',name=name);instrument('reinstalled')
    call('pull','/sdcard/Android/data/'+pkg+'/files/refinement-captures',out/'captures')
    (out/'REINSTALL.json').write_text(json.dumps(dict(physical_phone=False,synthetic_account=True,real_android_uninstall_and_reinstall=True,actual_saf_import=True,account_hash_reproduced=True,xp_and_all_53_facts_preserved=True,showcase_title_billing_restored=True,apk_sha256=hashlib.sha256((root/'app/build/outputs/apk/release/app-release.apk').read_bytes()).hexdigest()),indent=2),encoding='utf-8')
finally:call('shell','settings','put','global','animator_duration_scale',old_animation)
