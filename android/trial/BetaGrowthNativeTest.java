package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;

/** Actual beta Views on an owned synthetic emulator. Never run on a user device/account. */
public final class BetaGrowthNativeTest extends Instrumentation {
    int checks;Throwable failure;GrowthActivity activity;LinearLayout host;
    void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    void main(Runnable run){runOnMainSync(()->{try{run.run();}catch(Throwable e){failure=e;}});if(failure!=null)throw new AssertionError(failure);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.trial")&&Build.MODEL.contains("sdk"),"Owned beta emulator only");
        TrialDemo.prepare(c);String account=SubscriptionStore.accountKey(c);JSONObject before=RpgProgressStore.export(c,account);String ledger=before.getJSONObject("rpg").toString();
        activity=(GrowthActivity)startActivitySync(new Intent(c,GrowthActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1400);waitForIdleSync();
        main(()->{View hero=activity.findViewById(android.R.id.content).findViewWithTag("beta:hero");check(hero!=null,"New beta overview mounted");host=(LinearLayout)hero.getParent();});
        JSONObject r=before.getJSONObject("rpg"),m=MxpEconomy.state(r);
        main(()->{
            check(text(host).contains("가상 데모"),"Synthetic state explicit");
            check(!text(host).contains("배치 중"),"Unknown tier is not a loading promise");
            check(host.findViewWithTag("beta:goal")!=null,"Next honor goal");
            host.findViewWithTag("beta:honors").performClick();check(activity.detailDialog.isShowing(),"Honor catalog remains reachable");activity.detailDialog.dismiss();
            host.findViewWithTag("beta:wardrobe").performClick();check(activity.detailDialog.isShowing(),"Wardrobe remains reachable");activity.detailDialog.dismiss();
            host.findViewWithTag("beta:manage").performClick();check(text(host).contains("전체 칭호 모음"),"Legacy collections preserved");
            BetaGrowthUi.catalog(activity,r,true,false,0,"wrong-account");check(!activity.detailDialog.isShowing(),"Stale account action refused");
        });capture(c,"accumulated");
        JSONObject first=new JSONObject(before.toString()),firstR=first.getJSONObject("rpg"),firstM=MxpEconomy.state(firstR);
        first.put("evidence",new JSONObject());firstR.remove("quota_observed_at");firstR.put("equipped_title","");firstM.put("level",1).put("next_cost",5).put("progress_numerator","0").put("progress_denominator","1");
        for(String key:new String[]{"days","weeks","months","honors","titles"})firstM.put(key,new JSONObject());
        render(first,false,false,false);main(()->{
            String text=text(host);check(text.contains("활동 기준 확인 전"),"First observation pending");check(!text.contains("칭호를 골라보세요"),"No empty wardrobe primary action");check(text.contains("첫 발자국"),"First reward explained");check(text.contains("다음 보상까지 3일"),"First weekly checkpoint visible");
        });capture(c,"first");
        main(()->activity.findViewById(R.id.dashboard_scroll).scrollTo(0,host.findViewWithTag("beta:goal").getTop()));waitForIdleSync();
        main(()->{Button action=button(host.findViewWithTag("beta:goal"));android.graphics.Rect rect=new android.graphics.Rect();check(action!=null&&action.getGlobalVisibleRect(rect)&&rect.height()>=Ui.dp(activity,48),"Primary action reachable by scrolling at current font scale");});capture(c,"first-goal");
        main(()->activity.findViewById(R.id.dashboard_scroll).scrollTo(0,host.getHeight()));waitForIdleSync();
        main(()->{View menu=host.findViewWithTag("beta:manage");android.graphics.Rect rect=new android.graphics.Rect();check(menu.getGlobalVisibleRect(rect)&&rect.height()>=Ui.dp(activity,48),"Management action reachable above navigation");});capture(c,"bottom");
        JSONObject invalid=new JSONObject(first.toString());MxpEconomy.state(invalid.getJSONObject("rpg")).remove("weeks");render(invalid,false,false,false);
        main(()->{String text=text(host);check(text.contains("레벨 확인 불가"),"Failed summary not fake level");check(text.contains("확인 불가"),"Unavailable counters explicit");check(!text.contains("0 / 5일"),"Failure not fake zero");});capture(c,"error");
        render(before,true,true,false);main(()->check(text(host).contains("확인 오류"),"Fetch error retains labelled saved growth"));
        render(null,true,false,false);main(()->check(text(host).contains("성장 기록 확인 전"),"No journal gets an actionable pending screen"));
        AuthTokens originalTokens=SecureTokenStore.load(c);
        try {
            SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_QA","SYNTHETIC_QA","",System.currentTimeMillis()+86400000,"beta-growth-other-account",""));
            main(()->{activity.load(true);check(activity.findViewById(android.R.id.content).findViewWithTag("beta:hero")==null,"Account switch immediately clears previous hero");});
            Thread.sleep(700);waitForIdleSync();
        }finally{SecureTokenStore.save(c,originalTokens);main(()->activity.load(true));Thread.sleep(700);waitForIdleSync();}
        check(ledger.equals(RpgProgressStore.export(c,account).getJSONObject("rpg").toString()),"View/catalog/status rendering changed no XP awards or equipment");
        // Wide text scale and narrow width are system settings of this owned emulator only; snapshots below verify actual layout.
        main(()->activity.finish());result.putString("stream","Beta growth UI: "+checks+" checks passed; actual views, synthetic states, catalogs, account guards, unchanged SQLite ledger.\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
    void render(JSONObject journal,boolean automatic,boolean error,boolean demo){String account=SubscriptionStore.accountKey(getTargetContext());BetaGrowthModel model=BetaGrowthModel.create(journal,account,System.currentTimeMillis(),automatic,error,demo);
        main(()->{activity.betaGrowthModel=model;host.removeAllViews();check(BetaGrowthUi.render(activity,host,journal,true),"Detached state rendered");View scroll=activity.findViewById(R.id.dashboard_scroll);scroll.scrollTo(0,0);});waitForIdleSync();}
    static String text(View v){StringBuilder out=new StringBuilder();if(v instanceof TextView)out.append(((TextView)v).getText());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.append(text(((ViewGroup)v).getChildAt(i)));return out.toString();}
    static Button button(View v){if(v instanceof Button)return (Button)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button b=button(((ViewGroup)v).getChildAt(i));if(b!=null)return b;}return null;}
    void capture(Context c,String name)throws Exception{Thread.sleep(300);waitForIdleSync();java.io.File dir=new java.io.File(c.getExternalFilesDir(null),"beta994-captures");dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}}
}
