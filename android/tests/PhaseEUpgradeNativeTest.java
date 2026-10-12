package dev.bennett.codexmeter;

import android.app.*;
import android.app.job.JobScheduler;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import java.util.*;
import org.json.*;

/** Rebuilt public sources, one isolated package, install -r only. Never a user account. */
public final class PhaseEUpgradeNativeTest extends Instrumentation {
    Context c; Bundle args; int checks;
    void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    SharedPreferences saved(){return c.getSharedPreferences("phase_e_checkpoint",0);}
    String account(){return SubscriptionStore.accountKey(c);}
    JSONObject state()throws Exception{return RpgProgressStore.export(c,account()).getJSONObject("rpg");}
    void sign(String id)throws Exception{SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_E_ACCESS","SYNTHETIC_E_REFRESH","",System.currentTimeMillis()+86400000,id,""));}
    void tokens(long n)throws Exception{RpgProgressStore.tokens(c,account(),new JSONObject().put("account",account()).put("source","codex_account_usage_v1").put("at",System.currentTimeMillis()).put("summary",new JSONObject().put("lifetimeTokens",n)));}
    @Override public void onCreate(Bundle b){args=b;super.onCreate(b);start();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.phaseeqa")&&Build.MODEL.contains("sdk"),"Only isolated emulator fixture");
        ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
        String phase=args.getString("phase");if(phase.equals("legacy"))legacy();else if(phase.equals("checkpoint"))checkpoint();else verify();
        out.putString("stream","Phase E upgrade "+phase+": "+checks+" checks passed; rebuilt public code / isolated package / synthetic records.\n");finish(-1,out);
    }catch(Throwable e){out.putString("stream",android.util.Log.getStackTraceString(e));finish(0,out);}}
    void legacy()throws Exception{
        check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("2.8.28"),"Actual older code installed");
        check(!saved().contains("account"),"Initial fixture must be empty");sign("PHASE_E_ACCOUNT_A");
        AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);UpdatePreferences.setAutomaticChecks(c,false);
        long now=System.currentTimeMillis();UsageSnapshot snapshot=new UsageSnapshot("pro",true,false,null,new UsageWindow(20,604800,0,(now+86400000)/1000),now-3600000);
        AppPreferences.saveSnapshot(c,snapshot);check(UsageLedgerDatabase.record(c,snapshot,"{}",false),"Older production ledger records first observation");
        UsageSnapshot next=new UsageSnapshot("pro",true,false,null,new UsageWindow(80,604800,0,(now+86400000)/1000),now);
        AppPreferences.saveSnapshot(c,next);check(UsageLedgerDatabase.record(c,next,"{}",true),"Older production ledger records measured delta");
        SubscriptionCost cost=new SubscriptionCost("100","USD",java.time.LocalDate.now().minusDays(10),java.time.LocalDate.now().plusDays(20));
        check(SubscriptionStore.save(c,next,SubscriptionStore.key(c,next),cost),"Older payment information saved");
        RpgProgressStore.reconcile(c,next,UsageLedgerDatabase.load(c),account());tokens(100000000);
        JSONObject s=state();check(!RpgGrowthFacts.earned(s).isEmpty(),"Older automatic medals exist");
        saved().edit().putString("account",account()).putLong("legacy_xp",s.getLong("xp")).putString("auth",c.getSharedPreferences("secure_auth_v1",0).getString("blob","")).commit();
        ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
    }
    void checkpoint()throws Exception{
        check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("2.8.30"),"Normal install -r reached public v30 source");
        check(account().equals(saved().getString("account","")),"Keystore login survives older-to-v30 update");
        JSONObject s=state();check(s.getLong("xp")>=saved().getLong("legacy_xp",0),"Legacy XP retained");
        check(AchievementJourney.journey(s).getJSONObject("legacy").getJSONObject("achievements").length()==53,"53 old definitions archived");
        if("true".equals(args.getString("resume")))check(AchievementJourney.enabled(s)&&!TitleJourney.enabled(s)&&RpgGrowthFacts.earned(s).size()>=3&&RpgShowcase.selected(c,s).size()==3,"Resume only known partial synthetic checkpoint after widget permission failure");
        else if(!saved().getBoolean("preparing",false))check(!AchievementJourney.enabled(s)&&!TitleJourney.enabled(s),"Journeys remain explicitly opt-in");
        saved().edit().putBoolean("preparing",true).commit();
        RpgProgressStore.setTitlesEnabled(c,account(),true);tokens(101000000);
        RpgProgressStore.setAchievementsEnabled(c,account(),true);tokens(201000000);s=state();
        List<String> medals=RpgGrowthFacts.earned(s);check(medals.size()>=3,"Three acquired new medals available");medals=new ArrayList<>(medals.subList(0,3));
        check(RpgShowcase.save(c,account(),medals,s),"Representative order saved before update");
        String title=RpgTitleCatalog.owned(s).get(0);check(RpgProgressStore.equip(c,account(),title),"Title equipped before update");
        RpgProgressStore.setTitlesEnabled(c,account(),false);s=state();
        AppWidgetHost host=new AppWidgetHost(c,2850);int widget=saved().getInt("allocated_widget",-1);
        if(widget<0){widget=host.allocateAppWidgetId();saved().edit().putInt("allocated_widget",widget).commit();}
        check(AppWidgetManager.getInstance(c).bindAppWidgetIdIfAllowed(widget,new ComponentName(c,CodexUsageWidget.class)),"Bind widget before update");
        AppPreferences.saveWidgetOptions(c,widget,WidgetOptions.defaults().withVisibleMeters("weekly,next_reset"));
        String code="cm1|100.64.0.1|43195|"+"a".repeat(64)+"|"+"b".repeat(64);LanSync.pair(c,code);
        JSONObject archive=RpgProgressStore.archive(c,account());
        saved().edit().putLong("xp",s.getLong("xp")).putString("journey",AchievementJourney.journey(s).toString()).putString("titles",TitleJourney.journey(s).toString())
                .putString("medals",new JSONArray(medals).toString()).putString("title",s.optString("equipped_title"))
                .putString("billing",archive.getJSONObject("document").getJSONObject("billing").toString())
                .putString("settlements",archive.getJSONObject("document").getJSONObject("settlements").toString())
                .putInt("rows",UsageLedgerDatabase.load(c).records.size()).putInt("widget",widget)
                .putString("lan",c.getSharedPreferences("codex_lan_sync",0).getString(account(),""))
                .putString("auth",c.getSharedPreferences("secure_auth_v1",0).getString("blob","")).commit();
        sign("PHASE_E_ACCOUNT_B");UsageSnapshot b=new UsageSnapshot("free",true,false,null,new UsageWindow(0,604800,0,(System.currentTimeMillis()+86400000)/1000),System.currentTimeMillis());
        AppPreferences.saveSnapshot(c,b);RpgProgressStore.reconcile(c,b,new UsageLedgerDatabase.Data(),account());tokens(2000000);
        saved().edit().putString("b_account",account()).putLong("b_xp",state().getLong("xp")).commit();
        sign("PHASE_E_ACCOUNT_A");saved().edit().putString("auth",c.getSharedPreferences("secure_auth_v1",0).getString("blob","")).commit();
        ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
    }
    void verify()throws Exception{
        check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("2.8.50"),"Integrated version installed without deletion");
        check(account().equals(saved().getString("account","")),"A stable account hash retained");
        check(c.getSharedPreferences("secure_auth_v1",0).getString("blob","").equals(saved().getString("auth","")),"Encrypted authentication bytes preserved");
        JSONObject s=state();check(s.getLong("xp")==saved().getLong("xp",-1),"Update does not change XP");
        check(AchievementJourney.journey(s).toString().equals(saved().getString("journey","")),"Achievement start, segments, facts and ON flag preserved");
        check(TitleJourney.journey(s).toString().equals(saved().getString("titles","")),"Title start, segments, facts and OFF flag preserved");
        check(new JSONArray(RpgShowcase.selected(c,s)).toString().equals(saved().getString("medals","")),"Three representative medals and order preserved");
        check(s.optString("equipped_title").equals(saved().getString("title","")),"Equipped title preserved");
        JSONObject archive=RpgProgressStore.archive(c,account());
        check(archive.getJSONObject("document").getJSONObject("billing").toString().equals(saved().getString("billing","")),"Payment history retained");
        check(archive.getJSONObject("document").getJSONObject("settlements").toString().equals(saved().getString("settlements","")),"Past settlements retained");
        check(UsageLedgerDatabase.load(c).records.size()==saved().getInt("rows",-1),"SQLite observations retained");
        int widget=saved().getInt("widget",-1);check(AppWidgetManager.getInstance(c).getAppWidgetInfo(widget)!=null,"Original widget binding retained");
        check(AppPreferences.loadWidgetOptions(c,widget).visibleMeters.equals("weekly,next_reset"),"Per-widget settings retained");
        check(c.getSharedPreferences("codex_lan_sync",0).getString(account(),"").equals(saved().getString("lan","")),"Encrypted PC/Tailscale pairing preserved");
        char[] pass="phase-e-SYNTHETIC-password".toCharArray();byte[] file=RpgBackupCodec.encrypt(archive,pass);
        check(RpgProgressStore.restoreArchive(c,account(),RpgBackupCodec.decrypt(file,pass)),"Encrypted archive restores through Android SQLite");
        check(RpgProgressStore.restoreArchive(c,account(),RpgBackupCodec.decrypt(file,pass))&&state().getLong("xp")==saved().getLong("xp",-1),"Repeated restore does not duplicate XP");
        String preserved=RpgProgressStore.archive(c,account()).getJSONObject("document").toString();boolean wrong=false;
        try{RpgBackupCodec.decrypt(file,"wrong-password".toCharArray());}catch(Exception expected){wrong=true;}check(wrong,"Wrong password rejected");
        file[file.length/2]^=1;wrong=false;try{RpgBackupCodec.decrypt(file,pass);}catch(Exception expected){wrong=true;}check(wrong,"Damaged encrypted file rejected");
        check(RpgProgressStore.archive(c,account()).getJSONObject("document").toString().equals(preserved),"Rejected files do not alter database");
        JSONObject foreign=new JSONObject(archive.toString());sign("PHASE_E_ACCOUNT_B");
        check(account().equals(saved().getString("b_account",""))&&state().getLong("xp")==saved().getLong("b_xp",-1),"B sees only B progress");
        String bBefore=RpgProgressStore.archive(c,account()).getJSONObject("document").toString();wrong=false;
        try{RpgProgressStore.restoreArchive(c,account(),foreign);}catch(Exception expected){wrong=true;}
        check(wrong&&RpgProgressStore.archive(c,account()).getJSONObject("document").toString().equals(bBefore),"Cross-account restoration rejected atomically");
        sign("PHASE_E_ACCOUNT_A");check(state().getLong("xp")==saved().getLong("xp",-1),"Return to A keeps A XP");Arrays.fill(pass,'\0');
        ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
    }
}
