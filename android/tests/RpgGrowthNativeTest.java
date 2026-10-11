package dev.bennett.codexmeter;

import android.app.*;import android.content.*;import android.content.res.Configuration;import android.graphics.*;import android.os.Bundle;import android.view.*;import android.widget.*;
import java.io.*;import java.time.*;import java.util.*;import org.json.*;

/** Synthetic account in a distinct QA application ID. Never opens the installed production app. */
public final class RpgGrowthNativeTest extends Instrumentation {
    int checks;long renderMs;
    void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();check(c.getPackageName().endsWith(".rpg3qa"),"Isolated application required");
        AppPreferences.setAutomaticRefresh(c,false);UpdatePreferences.setAutomaticChecks(c,false);
        long now=System.currentTimeMillis();SecureTokenStore.save(c,new AuthTokens("RPG_QA_FAKE_ACCESS","RPG_QA_FAKE_REFRESH","",now+86400000,"rpg3-synthetic-account",""));
        LocalDate start=LedgerAggregation.day(now).minusDays(2);long reset=now+3*86400000L;
        UsageSnapshot snapshot=new UsageSnapshot("pro",true,false,null,new UsageWindow(40,604800,0,reset/1000),now);AppPreferences.saveSnapshot(c,snapshot);
        String account=SubscriptionStore.accountKey(c);check(SubscriptionStore.save(c,snapshot,SubscriptionStore.key(c,snapshot),new SubscriptionCost("100","USD",start,start.plusDays(30))),"Registered payment");
        c.getSharedPreferences("codex_fun_v1",0).edit().putString(SubscriptionStore.key(c,snapshot),new JSONObject().put("rating",new JSONObject().put("tier",7).put("end",now-1000)).toString()).commit();
        UsageLedgerDatabase.Data data=new UsageLedgerDatabase.Data();data.records.add(new LedgerRecord("weekly","pro",now-2000,80,"80",reset,604800,"api_precise",false));data.records.add(new LedgerRecord("weekly","pro",now-1000,0,"0",reset+86400000,604800,"api_precise",false));data.records.add(new LedgerRecord("weekly","pro",now,40,"40",reset+86400000,604800,"api_precise",false));
        RpgProgressStore.reconcile(c,snapshot,data,account);check(RpgProgressStore.tier(c)==7,"Reset-safe common Korea tier");
        JSONObject envelope=new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",now).put("summary",new JSONObject().put("lifetimeTokens",100000000L)).put("dailyUsageBuckets",new JSONArray().put(new JSONObject().put("startDate",LedgerAggregation.day(now).toString()).put("tokens",100000000L)));
        RpgProgressStore.tokens(c,account,envelope);long xp=RpgProgressStore.progress(c).getLong("xp");String title=RpgProgressStore.progress(c).getString("recommended_title");check(!title.isEmpty(),"Earned recommendation");
        GrowthActivity activity=(GrowthActivity)startActivitySync(new Intent(c,GrowthActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitLoaded(activity);
        JSONObject journal=RpgProgressStore.export(c,account);Throwable[] error={null};
        runOnMainSync(()->{try{save(activity,activity.getWindow().getDecorView(),411,"growth-screen.png");}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        runOnMainSync(()->{try{render(activity,journal);}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        runOnMainSync(()->activity.open("titles"));waitLoaded(activity);
        runOnMainSync(()->{View button=activity.findViewById(R.id.dashboard_content).findViewWithTag("growth:title:"+title);check(button!=null&&button.isEnabled(),"Separate title screen exposes eligible equip action");button.performClick();});waitLoaded(activity);check(RpgProgressStore.progress(c).getString("equipped_title").equals(title),"UI title action persists");
        runOnMainSync(()->activity.equip(""));waitLoaded(activity);check(RpgProgressStore.progress(c).optString("equipped_title").isEmpty(),"UI title removal persists");
        check(RpgProgressStore.progress(c).getLong("xp")==xp,"Title actions do not grant XP");
        java.util.List<String> badges=RpgGrowthFacts.earned(journal.getJSONObject("rpg")).subList(0,3);
        check(RpgShowcase.save(c,account,badges,journal.getJSONObject("rpg")),"Representative selection persists");
        check(RpgShowcase.selected(c,RpgProgressStore.progress(c)).equals(badges),"Home cache and growth selection agree after reopen");
        check(!RpgShowcase.save(c,"other-account",badges,journal.getJSONObject("rpg")),"Foreign account cannot replace showcase");
        check(!RpgShowcase.save(c,account,RpgGrowthFacts.earned(journal.getJSONObject("rpg")).subList(0,4),journal.getJSONObject("rpg")),"More than three rejected without replacing settings");
        check(RpgShowcase.save(c,account,List.of(),journal.getJSONObject("rpg"))&&RpgShowcase.selected(c,RpgProgressStore.progress(c)).isEmpty(),"Explicit unequip all remains empty");
        check(RpgShowcase.save(c,account,badges,journal.getJSONObject("rpg")),"Selections restored without modifying achievements");
        JSONObject sample=new JSONObject().put("achievements",new JSONObject().put("days_1",new JSONObject().put("unlocked",true)));
        c.getSharedPreferences("codex_rpg_showcase_v1",0).edit().remove(account+":seen").commit();
        check(RpgShowcase.observe(c,account,sample).isEmpty(),"First display does not replay historical unlocks");
        sample.getJSONObject("achievements").put("days_7",new JSONObject().put("unlocked",true));
        c.getSharedPreferences("codex_tier_effects",0).edit().putBoolean("enabled",true).commit();
        check(RpgShowcase.observe(c,account,sample).equals(List.of("days_7")),"New unlock highlight emitted once");
        check(RpgShowcase.observe(c,account,sample).isEmpty(),"Repeated rendering does not repeat reward highlight");
        c.getSharedPreferences("codex_tier_effects",0).edit().putBoolean("enabled",false).commit();sample.getJSONObject("achievements").put("days_30",new JSONObject().put("unlocked",true));
        check(RpgShowcase.observe(c,account,sample).isEmpty(),"Effects off suppresses unlock highlight");
        c.getSharedPreferences("codex_tier_effects",0).edit().putBoolean("enabled",true).commit();check(RpgShowcase.observe(c,account,sample).isEmpty(),"Enabling effects does not replay suppressed highlight");
        RpgShowcase.observe(c,account,journal.getJSONObject("rpg"));
        check(RpgProgressStore.progress(c).getLong("xp")==xp,"Showcase and highlight never change XP");
        c.getSharedPreferences("codex_rpg_display_v1",0).edit().remove(account).commit();check(RpgProgressStore.progress(c).getLong("xp")==xp,"SQLite reopen restores growth");
        RpgProgressStore.tokens(c,account,envelope);check(RpgProgressStore.progress(c).getLong("xp")==xp,"Token replay is idempotent");
        BillingHistoryActivity billing=(BillingHistoryActivity)startActivitySync(new Intent(c,BillingHistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(600);waitForIdleSync();
        runOnMainSync(()->{try{View view=billing.findViewById(R.id.dashboard_content);check(text(view).contains("USD"),"Billing history uses registered currency");save(billing,view,411,"billing.png");billing.finish();activity.finish();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw error[0];
        result.putString("stream","RPG growth native: "+checks+" checks passed; render total ms="+renderMs+". Synthetic isolated emulator, not real phone/account migration.\n");finish(-1,result);
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
    void waitLoaded(Activity a)throws Exception{Thread.sleep(800);waitForIdleSync();}
    void render(GrowthActivity a,JSONObject journal)throws Exception{
        Configuration original=new Configuration(a.getResources().getConfiguration());
        try{for(int width:new int[]{320,411,700})for(boolean dark:new boolean[]{false,true}){
            Configuration conf=new Configuration(original);conf.setLocale(Locale.KOREAN);conf.fontScale=width==320?1.5f:1;conf.screenWidthDp=width;a.getResources().updateConfiguration(conf,a.getResources().getDisplayMetrics());
            LinearLayout root=new LinearLayout(a);root.setOrientation(1);root.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),0);long begin=System.nanoTime();RpgGrowthUi.render(a,root,journal,dark,false);save(a,root,width,"growth-"+width+"-"+dark+".png");renderMs+=(System.nanoTime()-begin)/1000000;
            String copy=text(root);for(String required:new String[]{"대한민국","세계","MASTER","Lv.","XP","현재 요금제 인장","전체 보기","칭호"})check(copy.contains(required),"Growth required: "+required);noEllipsis(root);
            if(width==411&&dark){LinearLayout catalog=new LinearLayout(a);catalog.setOrientation(1);RpgGrowthUi.render(a,catalog,journal,true,true);save(a,catalog,width,"catalog.png");check(text(catalog).contains("미획득"),"Locked achievements remain visible");check(!text(catalog).contains(a.getString(R.string.growth_title_pick)),"Achievement catalog is separate from title chooser");
                LinearLayout titles=new LinearLayout(a);titles.setOrientation(1);RpgGrowthUi.render(a,titles,journal,dark,"titles");save(a,titles,width,"titles.png");check(!text(titles).contains(a.getString(R.string.rpg3_achievements)),"Titles do not duplicate achievement catalog");
                for(String kind:new String[]{"KR","WORLD","level","plan"}){View card=root.findViewWithTag("growth:"+kind);check(card!=null&&card.isClickable()&&card.isFocusable(),"Growth card is accessible and actionable: "+kind);card.performClick();check(a.detailDialog!=null&&a.detailDialog.isShowing(),"Actual card tap opens dialog: "+kind);LinearLayout detail=RpgDetails.body(a,kind,journal.getJSONObject("rpg"),dark);save(a,detail,width,"detail-"+kind+".png");noEllipsis(detail);a.detailDialog.dismiss();}
                check(root.findViewWithTag("growth:KR").getMeasuredHeight()==root.findViewWithTag("growth:WORLD").getMeasuredHeight(),"Tier row has equal card heights");
                check(root.findViewWithTag("growth:level").getMeasuredHeight()==root.findViewWithTag("growth:plan").getMeasuredHeight(),"Level / plan row has equal heights");
                int representatives=0;for(int i=0;i<root.getChildCount();i++)if("growth:representative".equals(root.getChildAt(i).getTag()))representatives++;check(representatives<=3,"Overview shows no more than three representative achievements");}
            LinearLayout home=new LinearLayout(a);home.setOrientation(1);home.setPadding(Ui.dp(a,16),0,Ui.dp(a,16),0);home.addView(LiveCards.crest(a,dark,null,true));
            long now=System.currentTimeMillis();UsageSnapshot demo=new UsageSnapshot("pro",true,false,null,new UsageWindow(13.42736,604800,0,(now+259200000L)/1000),now);
            java.lang.reflect.Method overview=LedgerDashboard.class.getDeclaredMethod("addOverview",androidx.appcompat.app.AppCompatActivity.class,LinearLayout.class,java.util.concurrent.ExecutorService.class,UsageSnapshot.class,boolean.class);overview.setAccessible(true);
            java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();try{overview.invoke(null,a,home,worker,demo,dark);}finally{worker.shutdown();}
            home.addView(SubscriptionValueUi.card(a,dark,AppPreferences.loadSnapshot(a),new LiveUtilization.Result(28,40,0,0,1,true,false,true),true));save(a,home,width,"home-"+width+"-"+dark+".png");
            String homeCopy=text(home);for(String required:new String[]{"MASTER","86.573%","13.427%",a.getString(R.string.ui_ledger_today_usage),a.getString(R.string.v3_limit_details),a.getString(R.string.ui_refresh_56e3ba),"USD"}){check(homeCopy.contains(required),"Home required: "+required);}check(TierTheme.tier(a)==RpgProgressStore.progress(a).getJSONObject("KR").getInt("tier"),"Home and widgets share growth Korea tier");noEllipsis(home);
        }
        Configuration large=new Configuration(original);large.setLocale(Locale.ENGLISH);large.fontScale=1.5f;large.screenWidthDp=411;a.getResources().updateConfiguration(large,a.getResources().getDisplayMetrics());
        LinearLayout enlarged=new LinearLayout(a);enlarged.setOrientation(1);RpgGrowthUi.render(a,enlarged,journal,true,false);save(a,enlarged,411,"growth-large-english.png");noEllipsis(enlarged);check(text(enlarged).contains("Choose up to 3"),"English enlarged-text actions visible");
        Configuration emptyConfig=new Configuration(original);emptyConfig.setLocale(Locale.KOREAN);a.getResources().updateConfiguration(emptyConfig,a.getResources().getDisplayMetrics());
        LinearLayout empty=new LinearLayout(a);empty.setOrientation(1);RpgGrowthUi.render(a,empty,null,true,false);save(a,empty,320,"empty.png");check(text(empty).contains("자료 부족"),"Missing records are not fake usage");
        Set<Integer> images=new HashSet<>();for(String plan:new String[]{"free","go","plus","pro5x","pro20x","unknown"})images.add(RpgPlanSeals.image(plan));check(images.size()==6,"Five distinct plan seals and neutral seal");
        }finally{a.getResources().updateConfiguration(original,a.getResources().getDisplayMetrics());}
    }
    void noEllipsis(View v){if(v instanceof TextView){TextView t=(TextView)v;if(t.getLayout()!=null)for(int i=0;i<t.getLayout().getLineCount();i++)check(t.getLayout().getEllipsisCount(i)==0,"Required text truncation");}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)noEllipsis(((ViewGroup)v).getChildAt(i));}
    static String text(View v){StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s.append(text(((ViewGroup)v).getChildAt(i)));return s.toString();}
    static void save(Context c,View v,int dp,String name)throws Exception{int w=Ui.dp(c,dp);v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));int h=Math.min(5200,Math.max(Ui.dp(c,120),v.getMeasuredHeight()));v.layout(0,0,w,v.getMeasuredHeight());Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xff0d0e10);v.draw(canvas);File dir=new File(c.getExternalFilesDir(null),"rpg3-fixtures");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
