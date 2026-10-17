package dev.bennett.codexmeter;

import android.app.*;import android.content.*;import android.database.Cursor;import android.graphics.Bitmap;
import android.os.*;import android.view.*;import android.widget.*;import org.json.*;
import java.time.*;import java.util.*;import java.util.concurrent.*;import java.lang.reflect.Proxy;

/** Owned synthetic AVD only: actual additive SQLite, restore, compact roots and explicit clipboard action. */
public final class Stable311MedalNativeTest extends Instrumentation {
    int checks;Throwable failure;String locale,theme;final List<Activity> screens=new ArrayList<>();
    void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    void main(Runnable work){runOnMainSync(()->{try{work.run();}catch(Throwable error){failure=error;}});if(failure!=null){Throwable error=failure;failure=null;throw new AssertionError(error);}}
    static String text(View v){if(v==null||v.getVisibility()!=View.VISIBLE)return "";StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText()).append('\n');if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)s.append(text(((ViewGroup)v).getChildAt(i)));return s.toString();}
    static View label(View v,String wanted){if(v instanceof TextView&&wanted.equals(((TextView)v).getText().toString()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=label(((ViewGroup)v).getChildAt(i),wanted);if(found!=null)return found;}return null;}
    static TextView selectable(View v){if(v instanceof TextView&&((TextView)v).isTextSelectable())return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=selectable(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}return null;}
    Activity start(Class<?> cls)throws Exception {Activity a=startActivitySync(new Intent(getTargetContext(),cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));screens.add(a);waitForIdleSync();return a;}
    View tag(Activity a,String wanted)throws Exception {for(int i=0;i<200;i++){View[] v={null};main(()->v[0]=a.findViewById(android.R.id.content).findViewWithTag(wanted));if(v[0]!=null){waitForIdleSync();Thread.sleep(150);return v[0];}Thread.sleep(100);}throw new AssertionError("Missing view "+wanted+"; "+text(a.findViewById(android.R.id.content)));}
    void waitLabel(Activity a,String wanted,String expected)throws Exception {for(int i=0;i<100;i++){boolean[] same={false};main(()->{View view=a.findViewById(android.R.id.content).findViewWithTag(wanted);same[0]=view instanceof TextView&&((TextView)view).getText().toString().equals(expected);});if(same[0]){check(true,"Live cache updates "+wanted);return;}Thread.sleep(100);}throw new AssertionError("Stale live label "+wanted+" expected="+expected);}
    void capture(String name)throws Exception {waitForIdleSync();Thread.sleep(250);Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"Screenshot "+name);try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),"beta9911-"+locale+"-"+theme+"-"+getTargetContext().getResources().getConfiguration().fontScale+"-"+name+".png"))){check(image.compress(Bitmap.CompressFormat.PNG,100,out),"Screenshot saved");}image.recycle();}
    Context failedCache(Context c){return new ContextWrapper(c){@Override public SharedPreferences getSharedPreferences(String name,int mode){SharedPreferences prefs=super.getSharedPreferences(name,mode);if(!name.equals(BetaMedalStore.CACHE))return prefs;return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)->{
        if(!method.getName().equals("edit"))return method.invoke(prefs,args);SharedPreferences.Editor editor=prefs.edit();return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(self,action,values)->{if(action.getName().equals("commit"))return false;Object result=action.invoke(editor,values);return result instanceof SharedPreferences.Editor?self:result;});});}};}
    static UsageSnapshot sample(long at,double used){return new UsageSnapshot("plus",true,false,new UsageWindow(20,18000,0,(at+7200000)/1000),new UsageWindow(used,604800,0,(BetaV4.day(at).plusDays(3).atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli())/1000),at);}
    JSONObject fixture(String account,long now)throws Exception {
        LocalDate today=BetaV4.day(now),from=today.minusDays(110);JSONObject state=BetaMedalModel.empty(account,from.atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli(),false);
        for(LocalDate date=from;date.isBefore(today);date=date.plusDays(1)){
            long at=date.atTime(10,0).atZone(BetaV4.ZONE).toInstant().toEpochMilli(),reset=LocalDate.parse(BetaV4.week(date)).plusDays(7).atTime(12,0).atZone(BetaV4.ZONE).toInstant().toEpochMilli();
            BetaMedalModel.observe(state,account,new LedgerRecord("weekly","plus",at,10,"10",reset,604800,"api_precise",false),at);
            BetaMedalModel.observe(state,account,new LedgerRecord("weekly","plus",at+60000,12,"12",reset,604800,"api_precise",false),at+60000);
        }return state;
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);locale=args.getString("locale","ko");theme=args.getString("theme","dark");start();}
    @Override public void onStart(){Context c=getTargetContext();Bundle result=new Bundle();int code=0;UsageSnapshot originalSnapshot=null;String originalTone=null,originalTheme=null,originalLocale=null,originalError=null,originalPin=null;android.app.LocaleManager locales=c.getSystemService(android.app.LocaleManager.class);JSONObject originalGrowth=null,originalMedals=null;String account=null;
        try{
            check(Build.MODEL.contains("sdk")&&"stable311-qa".equals(SecureTokenStore.load(c).accountId)&&c.getPackageName().equals("dev.bennett.codexmeter"),"Owned synthetic stable only");
            check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("3.1.1"),"Installed local9.9.11");
            account=SubscriptionStore.accountKey(c);String key=account;originalSnapshot=AppPreferences.loadSnapshot(c);originalTone=FunStore.tone(c);originalTheme=AppPreferences.getAppTheme(c);originalLocale=locales.getApplicationLocales().toLanguageTags();originalError=AppPreferences.getLastError(c);originalPin=BetaPreferences.pinned(c,key);
            originalGrowth=BetaGrowthStore.load(c,key);originalMedals=BetaMedalStore.export(c,key);check(originalGrowth!=null,"Existing growth preserved");
            boolean[] guarded={false};main(()->{try{BetaMedalStore.load(c,key);}catch(IllegalStateException expected){guarded[0]=true;}catch(Exception error){throw new AssertionError(error);}});check(guarded[0],"Medal database rejects main thread");
            long now=System.currentTimeMillis();JSONObject seed=fixture(key,now);BetaMedalStore.restore(c,key,seed);
            JSONObject imported=BetaMedalStore.export(c,key);check(imported.getJSONObject("titles").has("medal.title3"),"Three gold months grant a separate permanent title");
            JSONObject previousDay=imported.getJSONObject("days").optJSONObject(BetaV4.day(now).toString());double previousPoints=previousDay==null?0:previousDay.optDouble("points");now=System.currentTimeMillis();
            double previousUsed=imported.optJSONObject("cursor")==null?10:imported.getJSONObject("cursor").getJSONObject("record").getDouble("used");
            UsageSnapshot baseline=sample(now-2,previousUsed),increase=sample(now-1,previousUsed+2);
            check(BetaMedalStore.record(c,key,baseline,false),"Fresh baseline persisted");
            ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<Boolean>> results=new ArrayList<>();
            for(int i=0;i<24;i++)results.add(pool.submit(()->BetaMedalStore.record(c,key,increase,false)));int changed=0;for(Future<Boolean> f:results)if(f.get())changed++;pool.shutdown();check(changed==1,"24 concurrent retries persist exactly one observation");
            JSONObject measured=BetaMedalStore.export(c,key);JSONObject today=measured.getJSONObject("days").getJSONObject(BetaV4.day(now).toString());check(today.getDouble("points")==previousPoints+2&&today.getInt("grade")==3,"Actual SQLite adds exactly2 percentage points with personal gold");
            check(!BetaMedalStore.record(c,"another-account",increase,false)&&BetaMedalStore.load(c,"another-account")==null,"Another account cannot read or write this medal ledger");
            UsageSnapshot next=sample(now,previousUsed+3);boolean cacheFailed=false;try{BetaMedalStore.record(failedCache(c),key,next,false);}catch(java.io.IOException expected){cacheFailed=true;}check(cacheFailed&&BetaMedalStore.failed(c),"Cache failure reported after DB commit");
            check(!BetaMedalStore.record(c,key,next,false),"Retry after failed cache cannot duplicate committed evidence");BetaMedalStore.load(c,key);check(!BetaMedalStore.failed(c),"Worker load repairs medal cache");
            check(BetaMedalStore.peek(c).getJSONObject("today").getDouble("points")==previousPoints+3,"DB retained exact points after cache failure");
            try(BetaMedalStore helper=new BetaMedalStore(c);Cursor row=helper.getReadableDatabase().rawQuery("PRAGMA integrity_check",null)){check(row.moveToFirst()&&row.getString(0).equals("ok"),"Actual medal SQLite integrity_check");}
            JSONObject bundle=BetaBundle.collect(c,key,true,true);check(bundle.has("medals"),"Selected growth backup includes medal ledger");check(!BetaBundle.collect(c,key,false,true).has("medals"),"Settings-only backup excludes medals");
            String evidence=bundle.getJSONObject("medals").getJSONObject("days").toString();for(int i=0;i<3;i++)BetaBundle.restore(c,key,bundle,true,false);
            check(BetaMedalStore.export(c,key).getJSONObject("days").toString().equals(evidence),"Repeated actual bundle restore preserves exact medal facts");
            JSONObject legacy=new JSONObject(bundle.toString());legacy.remove("medals");BetaBundle.restore(c,key,legacy,true,false);check(BetaMedalStore.export(c,key).getJSONObject("days").toString().equals(evidence),"Old backup does not remove newer medals");
            JSONObject foreign=new JSONObject(bundle.toString());foreign.getJSONObject("medals").put("account","another-account");boolean denied=false;try{BetaBundle.preview(c,key,foreign,true,false);}catch(Exception expected){denied=true;}check(denied,"Cross-account medal backup rejected before applying growth");
            check(BetaGrowthStore.load(c,key).getLong("total")==originalGrowth.getLong("total"),"Medals/backups do not grant XP");
            BetaMedalStore.equip(c,key,"medal.title3");check(!BetaMedalUi.equippedTitle(c,"legacy").equals("legacy"),"New earned title available without deleting legacy equipment");BetaMedalStore.equip(c,key,"");
            check(BetaGrowthStore.load(c,key).getString("equipped").equals(originalGrowth.getString("equipped")),"Legacy equipped title unchanged");
            AppPreferences.setAppTheme(c,theme);locales.setApplicationLocales(LocaleList.forLanguageTags(locale));FunStore.tone(c,"spicy");AppPreferences.setLastError(c,"");AppPreferences.saveSnapshot(c,next);Thread.sleep(300);
            BetaPreferences.prefs(c).edit().putString(key+":goal","rpg3.frame_50").commit();BetaGrowthStore.refreshDisplay(c);BetaGrowthStore.WORKER.submit(()->{}).get(15,TimeUnit.SECONDS);
            check(!BetaGrowthStore.peek(c).getJSONObject("next_goal").optBoolean("pinned"),"Legacy pin retained but automatic recommendation used");
            MainActivity home=(MainActivity)start(MainActivity.class);tag(home,"beta999:summary");tag(home,"beta999:weekly");tag(home,"beta9911:coach");
            check(home.findViewById(android.R.id.content).findViewWithTag("beta998:focus")==null,"No next-action card on home");
            check(((TextView)home.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:value")).getText().toString().equals(UsagePrecision.remaining(next.weekly,AppPreferences.usageDecimalPlaces(c))),"Primary weekly remaining retained");
            check(home.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:reset").getVisibility()==View.GONE,"Reset details collapsed on home");
            check(home.findViewById(android.R.id.content).findViewWithTag("beta999:weekly-period").getVisibility()==View.GONE,"Period explanation collapsed on home");
            long observed=AppPreferences.loadSnapshot(c).fetchedAtMillis;BetaMedalStore.equip(c,key,"medal.title3");waitLabel(home,"beta996:title",BetaMedalUi.name(c,"medal.title3"));tag(home,"beta9911:coach");capture("home");BetaMedalStore.equip(c,key,"");waitLabel(home,"beta996:title",BetaIdentity.title(c,originalGrowth.optString("equipped")));tag(home,"beta9911:coach");check(AppPreferences.loadSnapshot(c).fetchedAtMillis==observed,"New medal title appears without usage refresh");
            if(locale.equals("ko")){
            SpicyQuotes.Selection selected=SpicyQuotes.peek(c,key);check(selected!=null&&selected.id.startsWith("ko.medal.")&&selected.tip!=null,"Actual app selects a medal reaction and linked tip");
            main(()->{View tryTip=label(home.findViewById(android.R.id.content),c.getString(R.string.coach_try));check(tryTip!=null,"One-tap practical tip");tryTip.performClick();});
            java.lang.reflect.Field open=CoachUi.class.getDeclaredField("OPEN");open.setAccessible(true);AlertDialog tip=(AlertDialog)((Map<?,?>)open.get(null)).get(home);check(tip!=null&&tip.isShowing(),"Actual local tip dialog");
            final String[] visible={null};main(()->{TextView prompt=selectable(tip.getWindow().getDecorView());check(prompt!=null,"Visible selectable practical prompt");visible[0]=prompt.getText().toString();tip.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
            ClipboardManager clipboard=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);check(clipboard.getPrimaryClip()!=null&&clipboard.getPrimaryClip().getItemAt(0).getText().toString().equals(visible[0]),"Explicit copy copies exactly the visible prompt");main(tip::dismiss);Thread.sleep(6500);
            }else check(SpicyQuotes.peek(c,key)==null,"English retains the existing localized fallback without Korean-only medal copy");
            check(BetaGrowthStore.load(c,key).getLong("total")==originalGrowth.getLong("total"),"Tip view/copy adds no XP");
            check(BetaMedalStore.export(c,key).getJSONObject("days").getJSONObject(BetaV4.day(now).toString()).getDouble("points")==previousPoints+3,"Tip view/copy creates no medal evidence");
            RecordsActivity records=(RecordsActivity)start(RecordsActivity.class);tag(records,"beta9911:records");capture("records");
            check(label(records.findViewById(android.R.id.content),BetaMedalUi.t(records,"사용량 상세","Usage details"))!=null,"Original usage analytics reachable by detail button");
            GrowthActivity growth=(GrowthActivity)start(GrowthActivity.class);tag(growth,"beta9911:next-goal");tag(growth,"beta9911:medals");tag(growth,"beta9911:collections");capture("growth");
            check(growth.findViewById(android.R.id.content).findViewWithTag("beta:challenges")==null,"Full mission rules absent from growth first screen");
            check(growth.findViewById(android.R.id.content).findViewWithTag("beta:hero")==null,"No duplicated profile hero on growth first screen");
            check(BetaPreferences.pinned(c,key).equals("rpg3.frame_50"),"Existing pin preference preserved for backup compatibility");
            for(String rarity:new String[]{"common","rare","epic","legendary","mythic"}){
                check(androidx.core.graphics.ColorUtils.calculateContrast(BetaMedalUi.textColor(rarity,true),0xff1b1e24)>=4.5,"Dark rarity text contrast "+rarity);
                check(androidx.core.graphics.ColorUtils.calculateContrast(BetaMedalUi.textColor(rarity,false),0xfff8f9fc)>=4.5,"Light rarity text contrast "+rarity);
            }
            for(int grade=0;grade<=3;grade++)check(androidx.core.graphics.ColorUtils.calculateContrast(BetaMedalUi.color(grade,false),0xfff8f9fc)>=4.5,"Light medal glyph contrast "+grade);
            AlertDialog[] guardedDialog={null};main(()->guardedDialog[0]=BetaMedalUi.show(growth,"SYNTHETIC_ACCOUNT_BOUNDARY",BetaMedalUi.content(growth),key));
            AuthTokens retained=SecureTokenStore.load(c);
            try{
                AccountSession.install(c,new AuthTokens("SYNTHETIC_BETA9911_OTHER","SYNTHETIC_REFRESH","",System.currentTimeMillis()+3600000,"beta9911-other-account",""),false);
                c.sendBroadcast(new Intent(AppConstants.ACTION_USAGE_UPDATED).setPackage(c.getPackageName()));waitForIdleSync();Thread.sleep(300);
                check(BetaMedalStore.peek(c)==null&&BetaMedalStore.load(c,key)==null,"Actual account transition hides previous medal cache and DB view");
                check(!BetaMedalStore.record(c,key,next,false),"Late observation from previous account cannot commit");
                main(()->check(!guardedDialog[0].isShowing(),"Account switch dismisses open medal details"));
            }finally{AccountSession.install(c,retained,false);AppPreferences.saveSnapshot(c,next);}
            result.putString("stream","Stable311 medals: "+checks+" checks passed; actual SQLite/concurrent24/cache failure/restore/compact roots/medal coach/explicit copy; "+locale+" / "+theme+"; synthetic only.\n");code=-1;
        }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}
        finally{
            try{main(()->{for(Activity screen:screens)if(!screen.isDestroyed())screen.finish();});
                if(account!=null){if(originalMedals!=null)BetaMedalStore.restore(c,account,originalMedals);BetaMedalStore.equip(c,account,originalMedals==null?"":originalMedals.optString("equipped"));if(originalPin!=null)BetaPreferences.prefs(c).edit().putString(account+":goal",originalPin).commit();}
                if(originalSnapshot!=null)AppPreferences.saveSnapshot(c,originalSnapshot);if(originalTone!=null)FunStore.tone(c,originalTone);if(originalError!=null)AppPreferences.setLastError(c,originalError);if(originalTheme!=null)AppPreferences.setAppTheme(c,originalTheme);if(originalLocale!=null)locales.setApplicationLocales(LocaleList.forLanguageTags(originalLocale));
                BetaGrowthStore.refreshDisplay(c);
            }catch(Throwable cleanup){code=0;result.putString("stream",result.getString("stream","")+"\nCleanup failed: "+android.util.Log.getStackTraceString(cleanup));}
        }finish(code,result);
    }
}
