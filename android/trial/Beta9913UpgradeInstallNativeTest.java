package dev.bennett.codexmeter;

import android.app.*;import android.content.*;import android.content.pm.*;import android.os.*;import org.json.*;
/** Local signed candidate update continuity on a newly allocated, synthetic-only emulator sandbox. */
public final class Beta9913UpgradeInstallNativeTest extends Instrumentation {
    String phase;int checks;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase","verify");start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;try{
        Context c=getTargetContext();check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial"),"Owned beta emulator only");
        PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),0);SharedPreferences qa=c.getSharedPreferences("beta9913_upgrade_qa",0);
        if(phase.equals("seed")){
            check(info.versionName.equals("9.9.12")&&info.getLongVersionCode()==990012,"Published previous beta installed");
            check(SecureTokenStore.load(c)==null,"New synthetic sandbox has no real credentials");
            TrialDemo.prepare(c);BetaDemo995.prepare(c);String account=SubscriptionStore.accountKey(c);
            BetaIdentity.save(c,"업데이트 검수 Alex");BetaIdentity.prefs(c).edit().putBoolean("setup_done",true).commit();
            AppPreferences.setAppTheme(c,"dark");FunStore.tone(c,"off");BetaUpdatePolicy.initialize(c);UpdatePreferences.setAutomaticChecks(c,false);
            check(c.getSharedPreferences("trial_update_policy",0).edit().putBoolean("enabled_v1",true).commit()&&c.getSharedPreferences("codex_meter_updates_v1",0).edit().putBoolean("automatic",false).putBoolean("notify_updates",false).commit(),"Fixture first-run marker and OFF choice flushed to disk");
            check(c.getSharedPreferences("trial_update_policy",0).getBoolean("enabled_v1",false)&&!UpdatePreferences.automaticChecks(c),"User OFF choice follows completed first-run policy");
            JSONObject state=BetaGrowthStore.load(c,account);String title="rpg3.workbench";
            check(state.getJSONObject("titles").has(title)&&BetaGrowthStore.equip(c,account,title),"Owned title explicitly equipped");
            check(BetaGrowthStore.enabled(c,account,false),"Collection OFF fixture saved");state=BetaGrowthStore.load(c,account);
            check(state.getInt("level")>=30&&state.getLong("total")>0,"Nonempty level and XP fixture");
            check(state.getJSONObject("honors").length()>0&&state.getJSONObject("titles").length()>0,"Earned honors and titles with dates");
            JSONObject archive=BetaGrowthStore.archive(c,account);check(archive!=null&&archive.getJSONObject("rpg").length()>0,"Nonempty previous-growth archive");
            check(qa.edit().putString("state",state.toString()).putString("archive",archive.toString()).putString("name",BetaIdentity.name(c))
                .putString("account",account).putString("tokens",SecureTokenStore.load(c).toJson().toString()).putInt("uid",c.getApplicationInfo().uid)
                .putString("theme",AppPreferences.getAppTheme(c)).putString("tone",FunStore.tone(c)).putString("coach",c.getSharedPreferences("coach_v2",0).getString(account,""))
                .putBoolean("automatic",UpdatePreferences.automaticChecks(c)).putBoolean("refresh",AppPreferences.getAutomaticRefresh(c)).putBoolean("launch",AppPreferences.getRefreshOnLaunch(c))
                .putLong("first_install",info.firstInstallTime).putLong("before_update",info.lastUpdateTime).putBoolean("installed_from_ui",false).commit(),"Durable pre-update checkpoint");
            check(TrialDemo.active(c),"Synthetic encrypted credentials only");
        }else if(phase.equals("before")){
            check(info.versionName.equals("9.9.12")&&info.getLongVersionCode()==990012,"Still9.9.12 immediately before installing");
            check(c.getSharedPreferences("trial_update_policy",0).getBoolean("enabled_v1",false),"First-run policy completed before user settings checkpoint");
            check(!UpdatePreferences.automaticChecks(c)&&!qa.getBoolean("automatic",true),"OFF retained after actual9.9.12 UI launch");
            check(BetaGrowthStore.load(c,SubscriptionStore.accountKey(c)).getLong("total")==new JSONObject(qa.getString("state","")).getLong("total"),"Pre-install UI creates no XP");
        }else{
            check(info.versionName.equals("9.9.13")&&info.getLongVersionCode()==990013,"Actual installed package is9.9.13/code990013");
            check(qa.contains("state"),"Pre-update sandbox checkpoint survived");check(c.getApplicationInfo().uid==qa.getInt("uid",-1),"App UID retained");
            check(info.firstInstallTime==qa.getLong("first_install",-1),"Original first install time retained");
            check(info.lastUpdateTime>qa.getLong("before_update",Long.MAX_VALUE),"Actual APK replacement time advanced");
            check(TrialDemo.active(c)&&SecureTokenStore.load(c).toJson().toString().equals(qa.getString("tokens","")),"Encrypted credentials decrypt unchanged after actual update");
            String account=SubscriptionStore.accountKey(c);check(account.equals(qa.getString("account","")),"Account namespace unchanged");
            JSONObject before=new JSONObject(qa.getString("state","")),after=BetaGrowthStore.load(c,account);
            for(String key:new String[]{"rules","namespace","account","cutover","total","level","progress","next_cost","last_response","days","weeks","months","rewards","honors","titles","equipped","enabled","catalog_revision"})
                check(before.get(key).toString().equals(after.get(key).toString()),"Public update preserves "+key);
            check(after.optLong("seen_total")>=before.optLong("seen_total")&&after.optLong("seen_total")<=after.getLong("total"),"Acknowledgement never creates XP");
            check(BetaGrowthStore.archive(c,account).toString().equals(qa.getString("archive","")),"Full archive retained");
            check(BetaIdentity.name(c).equals(qa.getString("name",""))&&BetaIdentity.done(c),"Name and completed personalization retained");
            check(AppPreferences.getAppTheme(c).equals(qa.getString("theme","")),"Theme retained");check(FunStore.tone(c).equals(qa.getString("tone","")),"Coach OFF retained");
            JSONObject coachBefore=new JSONObject(qa.getString("coach","")),coachAfter=new JSONObject(c.getSharedPreferences("coach_v2",0).getString(account,""));
            check(coachBefore.getJSONArray("hidden").toString().equals(coachAfter.getJSONArray("hidden").toString()),"Hidden quote IDs retained");
            check(UpdatePreferences.automaticChecks(c)==qa.getBoolean("automatic",true),"Automatic-update OFF choice retained; actual="+UpdatePreferences.automaticChecks(c)+" checkpoint="+qa.getBoolean("automatic",true)+" initialized="+c.getSharedPreferences("trial_update_policy",0).getBoolean("enabled_v1",false));
            check(AppPreferences.getAutomaticRefresh(c)==qa.getBoolean("refresh",true)&&AppPreferences.getRefreshOnLaunch(c)==qa.getBoolean("launch",true),"Usage refresh preferences retained");
            try(BetaGrowthStore db=new BetaGrowthStore(c);android.database.Cursor row=db.getReadableDatabase().rawQuery("PRAGMA integrity_check",null)){
                check(row.moveToFirst()&&row.getString(0).equals("ok"),"Actual growth SQLite integrity_check");
            }
            check(BetaGrowthStore.export(c,account).getJSONObject("state").getLong("total")==before.getLong("total"),"Backup export reads retained XP");
            java.util.List<GitHubRelease> releases=ReleaseUpdateClient.check(c);
            check(UpdateChannel.selectUpdate(releases,"9.9.13","stable")==null,"Installed9.9.13 is not reoffered");
            check(UpdatePreferences.installError(c).isEmpty(),"No persisted installer failure");
            qa.edit().putBoolean("installed_from_ui",true).commit();
        }
        JSONObject facts=BetaGrowthStore.load(c,SubscriptionStore.accountKey(c));
        result.putString("stream","Beta9913 existing-install upgrade "+phase+": "+checks+" checks passed; Lv."+facts.getInt("level")+" / XP "+facts.getLong("total")+" / honors "+facts.getJSONObject("honors").length()+" / titles "+facts.getJSONObject("titles").length()+" / equipped "+facts.getString("equipped")+"; synthetic sandbox only.\n");code=-1;
    }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}finish(code,result);}
}
