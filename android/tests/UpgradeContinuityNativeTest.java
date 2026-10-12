package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import org.json.*;

/** Existing isolated upgrade fixture only. No user data or production package access. */
public final class UpgradeContinuityNativeTest extends Instrumentation {
    Bundle args;Context c;int checks;
    void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    @Override public void onCreate(Bundle b){args=b;super.onCreate(b);start();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.phaseeqa")&&Build.MODEL.contains("sdk"),"Isolated upgrade fixture only");
        SharedPreferences checkpoint=c.getSharedPreferences("phase_e_checkpoint",0);
        String account=SubscriptionStore.accountKey(c);check(account.equals(checkpoint.getString("account","")),"Synthetic account retained");
        JSONObject doc=RpgProgressStore.archive(c,account).getJSONObject("document");
        String stage=args.getString("continuity","probe");
        SharedPreferences kept=c.getSharedPreferences("stability_upgrade_checkpoint",0);
        if(stage.equals("baseline")){
            check(!kept.contains("document"),"New checkpoint only; do not replace existing fixture");
            JSONObject state=RpgEngine.state(doc);
            if("true".equals(args.getString("resume")))check(AchievementJourney.enabled(state)&&TitleJourney.enabled(state)&&RpgGrowthFacts.earned(state).isEmpty()&&state.getLong("xp")>=checkpoint.getLong("xp",-1),"Resume only the known partial empty, opted-in synthetic fixture");
            else{
                check(state.getLong("xp")==checkpoint.getLong("xp",-1),"v51 keeps lifetime XP");
                check(!AchievementJourney.enabled(state)&&!TitleJourney.enabled(state)&&RpgGrowthFacts.earned(state).isEmpty()&&TitleJourney.owned(state).isEmpty(),"v51 once-only collection restart is OFF and empty");
            }
            check(doc.getJSONObject("future").getJSONObject("collection_archives").length()>=2,"Prior opt-in collections archived");
            RpgProgressStore.setAchievementsEnabled(c,account,true);RpgProgressStore.setTitlesEnabled(c,account,true);
            // The first post-reset official summary establishes a baseline; only a later increase counts.
            for(long value:new long[]{401000000,501000000}){Thread.sleep(5);RpgProgressStore.tokens(c,account,new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",System.currentTimeMillis()).put("summary",new JSONObject().put("lifetimeTokens",value)));}
            doc=RpgProgressStore.archive(c,account).getJSONObject("document");state=RpgEngine.state(doc);
            java.util.List<String> medals=RpgGrowthFacts.earned(state);check(!medals.isEmpty(),"Real post-start collection has earned medals");
            check(RpgShowcase.save(c,account,medals.subList(0,Math.min(3,medals.size())),state),"Chosen medals saved");
            java.util.List<String> titles=RpgTitleCatalog.owned(state);check(!titles.isEmpty()&&RpgProgressStore.equip(c,account,titles.get(0)),"Earned title equipped");
            doc=RpgProgressStore.archive(c,account).getJSONObject("document");
            check(kept.edit().putString("document",doc.toString()).putString("showcase",new JSONArray(RpgShowcase.selected(c,RpgEngine.state(doc))).toString()).putString("auth",c.getSharedPreferences("secure_auth_v1",0).getString("blob","")).commit(),"Persist pre-update checkpoint");
        }else if(stage.equals("candidate")){
            check(kept.contains("document"),"Require prior v51 checkpoint");
            File debug=new File(c.getExternalFilesDir(null),"upgrade-continuity");debug.mkdirs();
            try(FileWriter file=new FileWriter(new File(debug,"candidate.json"))){file.write(new JSONObject().put("expected",new JSONObject(kept.getString("document","{}"))).put("actual",doc).toString());}
            JSONObject previous=new JSONObject(kept.getString("document","{}")),comparable=new JSONObject(doc.toString());
            // Freshness naturally expires while the upgrade runs. These two diagnostic
            // fields may change; the complete tier state, awards and every persisted fact may not.
            for(JSONObject item:new JSONObject[]{previous,comparable}){
                JSONObject state=RpgEngine.state(item);check(state.optLong("demotion_checked_at")>0&&state.optString("demotion_input").length()>0,"Demotion freshness diagnostics present");
                state.remove("demotion_checked_at");state.remove("demotion_input");
            }
            check(previous.toString().equals(comparable.toString()),"All RPG, XP, billing, evidence and collection state unchanged after install-r");
            check(new JSONArray(RpgShowcase.selected(c,RpgEngine.state(doc))).toString().equals(kept.getString("showcase","")),"Chosen medals and order survive update");
            check(c.getSharedPreferences("secure_auth_v1",0).getString("blob","").equals(kept.getString("auth","")),"Encrypted authentication bytes unchanged");
            int widget=checkpoint.getInt("widget",-1);
            check(android.appwidget.AppWidgetManager.getInstance(c).getAppWidgetInfo(widget)!=null&&AppPreferences.loadWidgetOptions(c,widget).visibleMeters.equals("weekly,next_reset"),"Original widget binding and settings survive");
            check(c.getSharedPreferences("codex_lan_sync",0).getString(account,"").equals(checkpoint.getString("lan","")),"Encrypted PC pairing retained");
            check(UsageLedgerDatabase.load(c).records.size()==checkpoint.getInt("rows",-1),"Original SQLite observations retained");
            c.getPackageManager().getActivityInfo(new ComponentName(c,ProfileShareActivity.class),0);check(true,"Candidate private profile Activity registered");
        }
        File folder=new File(c.getExternalFilesDir(null),"upgrade-continuity");folder.mkdirs();
        try(FileWriter file=new FileWriter(new File(folder,"comparison.json"))){file.write(new JSONObject().put("expected",new JSONObject(checkpoint.getString("settlements","{}"))).put("actual",doc.getJSONObject("settlements")).toString());}
        out.putString("stream","Upgrade continuity: "+checks+" checks passed; synthetic settlement comparison captured\n");finish(-1,out);
    }catch(Throwable e){out.putString("stream",android.util.Log.getStackTraceString(e));finish(0,out);}}
}
