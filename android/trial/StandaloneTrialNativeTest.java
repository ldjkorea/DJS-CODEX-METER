package dev.bennett.codexmeter;

import android.app.*;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;import java.util.*;import org.json.*;

/** Owned-emulator verification of the delivered standalone APK, not a live account test. */
public final class StandaloneTrialNativeTest extends Instrumentation {
    int checks;Throwable failure;String phase;
    void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable e){failure=e;}});if(failure!=null)throw new AssertionError(failure);}
    @Override public void onCreate(Bundle args){super.onCreate(args);phase=args==null?"ui":args.getString("phase","ui");start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.trial")&&Build.MODEL.contains("sdk"),"Only owned trial emulator");check(BuildConfig.KOREAN_UPDATES_ENABLED,"Beta update path enabled");
        PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_PERMISSIONS);check(Arrays.asList(info.requestedPermissions).contains("dev.bennett.codexmeter.trial.permission.INTERNAL"),"Separate internal permission");check(Arrays.asList(info.requestedPermissions).contains("android.permission.REQUEST_INSTALL_PACKAGES"),"User-confirmed installer permission");
        check(AppConstants.APP_LINK.equals("codexmetertrial://auth/complete"),"Separate OAuth return scheme");
        TrialWelcomeActivity welcome=(TrialWelcomeActivity)startActivitySync(new Intent(c,TrialWelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1000);waitForIdleSync();
        main(()->{check(welcome.findViewById(android.R.id.content)!=null,"Actual welcome screen opened");});
        check(UpdatePreferences.automaticChecks(c),"Automatic beta checks enabled");
        UpdatePreferences.setAutomaticChecks(c,false);BetaUpdatePolicy.initialize(c);check(!UpdatePreferences.automaticChecks(c),"Explicit OFF preserved after policy migration");UpdatePreferences.setAutomaticChecks(c,true);
        TrialDemo.prepare(c);check(TrialDemo.active(c),"Explicit synthetic demo account");JSONObject progress=RpgProgressStore.progress(c);check(progress.getJSONObject(MxpEconomy.KEY).getInt("level")>=30,"Actual MXP engine sample reached level 30");long total=progress.getJSONObject(MxpEconomy.KEY).getLong("total");
        TrialDemo.prepare(c);check(RpgProgressStore.progress(c).getJSONObject(MxpEconomy.KEY).getLong("total")==total,"Repeated demo entry does not add XP");check(UsageApi.refreshAndCache(c).planType.equals("free"),"Demo refresh served locally");boolean denied=false;try{UsageApi.usableTokens(c);}catch(Exception e){denied=true;}check(denied,"Synthetic tokens blocked before account API");
        android.content.SharedPreferences continuity=c.getSharedPreferences("trial_qa_continuity",0);
        String facts=progress.getJSONObject(MxpEconomy.KEY).getJSONObject("honors").toString()+progress.getJSONObject(MxpEconomy.KEY).getJSONObject("titles").toString()+progress.optString("equipped_title");
        if(phase.equals("seed")){continuity.edit().putLong("total",total).putString("facts",facts).putBoolean("marker",true).commit();}
        else {check(continuity.getBoolean("marker",false),"Upgrade setting marker retained");check(continuity.getLong("total",-1)==total,"Upgrade MXP balance retained");check(continuity.getString("facts","").equals(facts),"Upgrade honors titles equipment dates retained");}
        if(phase.equals("download")){
            java.util.List<GitHubRelease> releases=ReleaseUpdateClient.check(c);GitHubRelease next=UpdateChannel.selectUpdate(releases,info.versionName,"stable");check(next!=null&&next.version.equals("9.9.2"),"Public beta discovery to 9.9.2");
            UpdateInstaller.PreparedUpdate prepared=UpdateInstaller.prepare(c,next,null);check(prepared.versionCode==990002&&prepared.versionName.equals("9.9.2"),"Real APK download checksum package signing code verified");
            result.putString("stream","Beta download: "+checks+" checks passed; verified official installer candidate, no silent install.\n");finish(-1,result);return;
        }
        MainActivity home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(2500);waitForIdleSync();main(()->check(home.findViewById(android.R.id.content)!=null,"Actual trial home opened"));
        GrowthActivity growth=(GrowthActivity)startActivitySync(new Intent(c,GrowthActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1200);waitForIdleSync();int level=RpgProgressStore.progress(c).getJSONObject(MxpEconomy.KEY).getInt("level");main(()->check(text(growth.findViewById(android.R.id.content)).contains(Integer.toString(level)),"Growth shows sampled level"));
        if(ReleaseVersion.compare(info.versionName,"9.9.1")>=0){
            main(()->{
                View root=growth.findViewById(android.R.id.content);check(root.findViewWithTag("beta:hero")!=null,"Visual level hero");check(root.findViewWithTag("beta:challenges")!=null,"Daily weekly monthly progress cards");
                androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=growth.findViewById(R.id.dashboard_refresh);check(!refresh.isEnabled()&&!refresh.isRefreshing(),"Read-only growth cannot leave spinner running");
                check(!text(root).contains(growth.getString(R.string.reborn_baseline_pending)),"No obsolete token baseline notice in MXP overview");
                root.findViewWithTag("beta:honors").performClick();check(growth.detailDialog!=null&&growth.detailDialog.isShowing(),"Honor grid opens");
            });waitForIdleSync();capture(c,"honors");
            main(()->{View grid=growth.detailDialog.getWindow().getDecorView();check(grid.findViewWithTag("beta:medal:rpg3.first_day")!=null,"Earned medal tile");check(grid.findViewWithTag("beta:medal:rpg3.annual_gold_3")!=null,"Locked mythic medal tile");growth.detailDialog.dismiss();growth.findViewById(android.R.id.content).findViewWithTag("beta:wardrobe").performClick();});waitForIdleSync();capture(c,"titles");
            main(()->{check(growth.detailDialog.getWindow().getDecorView().findViewWithTag("beta:medal:rpg3.workbench")!=null,"Owned title wardrobe tile");growth.detailDialog.getWindow().getDecorView().findViewWithTag("beta:medal:rpg3.starter").performClick();check(growth.detailDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).getText().toString().equals("이 칭호 장착"),"Explicit owned-title equip action");growth.detailDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();});Thread.sleep(1200);waitForIdleSync();
            check(RpgProgressStore.progress(c).optString("equipped_title").equals("rpg3.starter"),"Actual selected title saved");
            main(()->growth.equip("rpg3.workbench"));Thread.sleep(1200);waitForIdleSync();check(RpgProgressStore.progress(c).optString("equipped_title").equals("rpg3.workbench"),"Original equipped title restored in synthetic fixture");Thread.sleep(2500);capture(c,"growth");
            check(RpgProgressStore.progress(c).getJSONObject(MxpEconomy.KEY).getLong("total")==total,"Browsing medals titles gives no XP");
        }
        check(SpicyQuotes.peek(c,SubscriptionStore.accountKey(c))!=null&&SpicyQuotes.peek(c,SubscriptionStore.accountKey(c)).tip!=null,"Actual spicy quote links a practice tip");
        main(()->{growth.finish();home.finish();welcome.finish();});
        result.putString("stream","Standalone beta: "+checks+" checks passed; separate package, isolated updater, synthetic local refresh, upgrade continuity, real welcome/home/growth; no live OAuth/device proof.\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
    static String text(View v){StringBuilder out=new StringBuilder();if(v instanceof TextView)out.append(((TextView)v).getText());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.append(text(((ViewGroup)v).getChildAt(i)));return out.toString();}
    void capture(Context c,String name)throws Exception {Thread.sleep(700);waitForIdleSync();java.io.File dir=new java.io.File(c.getExternalFilesDir(null),"beta991-captures");dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}}
}
