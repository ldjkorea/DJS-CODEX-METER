package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Current production home/card on the owned read-only synthetic preview AVD only. */
public final class Stable313HomeResetNativeTest extends Instrumentation {
    int checks;Throwable failure;MainActivity home;Context c;
    void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    void main(Runnable work){runOnMainSync(()->{try{work.run();}catch(Throwable t){failure=t;}});if(failure!=null){Throwable t=failure;failure=null;throw new AssertionError(t);}}
    TextView reset(View root,String key){return root.findViewWithTag("beta998:quota:"+key+":reset");}
    UsageSnapshot sample(long observed,long five,long weekly){return new UsageSnapshot("plus",true,false,new UsageWindow(25.25,18000,0,five/1000),new UsageWindow(66.75,604800,0,weekly/1000),observed);}
    void bounds(View v){
        if(v.getVisibility()!=View.VISIBLE)return;
        if(v instanceof TextView){TextView t=(TextView)v;android.text.Layout l=t.getLayout();
            if(l!=null&&t.length()>0){check(l.getEllipsisCount(l.getLineCount()-1)==0,"No ellipsis: "+t.getText());check(l.getLineEnd(l.getLineCount()-1)==t.length(),"Full reset text laid out");check(t.getHeight()>=t.getCompoundPaddingTop()+t.getCompoundPaddingBottom()+l.getLineBottom(l.getLineCount()-1),"No vertical clipping");}}
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)bounds(((ViewGroup)v).getChildAt(i));
    }
    void image(String name)throws Exception{
        Thread.sleep(250);getUiAutomation().waitForIdle(100,2000);java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);
        main(()->{View decor=home.getWindow().getDecorView();decor.getViewTreeObserver().registerFrameCommitCallback(frame::countDown);decor.invalidate();});
        check(frame.await(3,java.util.concurrent.TimeUnit.SECONDS),"Screenshot after committed native frame");
        File dir=new File(c.getExternalFilesDir(null),"stable313-home-reset");dir.mkdirs();
        Bitmap b=getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;c=getTargetContext();UsageSnapshot old=null;String error=null;android.app.LocaleManager locales=null;LocaleList oldLocale=null;boolean updates=false;String growth=null,auth=null;
        try{
            check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter")&&TrialDemo.active(c),"Owned emulator synthetic account only");
            check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("3.1.3"),"Local candidate version unchanged");
            check(!AppPreferences.getAutomaticRefresh(c)&&!AppPreferences.getRefreshOnLaunch(c),"No real account refresh");
            old=AppPreferences.loadSnapshot(c);error=AppPreferences.getLastError(c);updates=UpdatePreferences.automaticChecks(c);growth=BetaGrowthStore.load(c,SubscriptionStore.accountKey(c)).toString();auth=SecureTokenStore.load(c).toJson().toString();
            UpdatePreferences.setAutomaticChecks(c,false);AppPreferences.setLastError(c,"");locales=c.getSystemService(android.app.LocaleManager.class);oldLocale=locales.getApplicationLocales();locales.setApplicationLocales(LocaleList.forLanguageTags("ko"));
            long now=System.currentTimeMillis();UsageSnapshot s=sample(now,now+7200000,now+3*86400000L);AppPreferences.saveSnapshot(c,s);
            home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(900);waitForIdleSync();
            main(()->{View root=home.findViewById(android.R.id.content);TextView f=reset(root,"five-hour"),w=reset(root,"weekly");
                check(f!=null&&w!=null&&f.isShown()&&w.isShown(),"Both reset labels visible on actual home");
                check(f.getText().toString().contains(UsageFormat.absolute(home,s.fiveHour.resetAtMillis(),System.currentTimeMillis())),"5-hour civil reset time on main");
                check(w.getText().toString().contains(UsageFormat.absolute(home,s.weekly.resetAtMillis(),System.currentTimeMillis())),"Weekly date/time on main");
                check(f.getText().toString().contains("\n초기화까지 ")&&w.getText().toString().contains("\n초기화까지 "),"Both countdowns shown below absolute time");
                TextView fv=root.findViewWithTag("beta998:quota:five-hour:value"),wv=root.findViewWithTag("beta998:quota:weekly:value");check(fv.getText().toString().equals(UsagePrecision.remaining(s.fiveHour,AppPreferences.usageDecimalPlaces(home)))&&wv.getText().toString().equals(UsagePrecision.remaining(s.weekly,AppPreferences.usageDecimalPlaces(home))),"Percent unchanged");
                View summary=root.findViewWithTag("beta999:summary"),weekly=root.findViewWithTag("beta999:weekly");check(summary!=null&&weekly!=null&&summary.getTop()<weekly.getTop(),"Summary still precedes weekly section");bounds(summary);
            });image("main-ko-dark");
            // Native TextViews/layout at narrow and large-font configurations, without changing device settings.
            main(()->{Configuration saved=new Configuration(home.getResources().getConfiguration());try{
                for(int width:new int[]{320,360})for(float scale:new float[]{1f,2f}){
                    Configuration config=new Configuration(saved);config.screenWidthDp=width;config.fontScale=scale;home.getResources().updateConfiguration(config,home.getResources().getDisplayMetrics());
                    LinearLayout card=new LinearLayout(home);card.setOrientation(LinearLayout.VERTICAL);BetaQuotaUi.addContent(home,card,s,true,new ArrayList<>());
                    int px=Ui.dp(home,width-64);card.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));card.layout(0,0,px,card.getMeasuredHeight());
                    check(reset(card,"five-hour").getVisibility()==View.VISIBLE&&reset(card,"weekly").getVisibility()==View.VISIBLE,"Native reset labels survive width/font configuration");bounds(card);
                }
            }finally{home.getResources().updateConfiguration(saved,home.getResources().getDisplayMetrics());}});
            long at=System.currentTimeMillis();UsageSnapshot expiring=sample(at,at+3500,at+3500);AppPreferences.saveSnapshot(c,expiring);main(home::rebuild);Thread.sleep(4100);waitForIdleSync();
            main(()->{View root=home.findViewById(android.R.id.content);check(reset(root,"five-hour").getText().toString().contains("시각 경과")&&reset(root,"weekly").getText().toString().contains("시각 경과"),"Real timer crosses reset without claiming refreshed quota");});
            AppPreferences.saveSnapshot(c,s);AppPreferences.setLastError(c,"SYNTHETIC_OFFLINE");main(home::rebuild);
            main(()->{TextView status=home.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:status");check(status.isShown()&&status.getText().toString().equals(home.getString(R.string.phase_a_cached_failed)),"Error identifies cached reset");});
            AppPreferences.setLastError(c,"");AppPreferences.saveSnapshot(c,sample(System.currentTimeMillis()-86400000L,s.fiveHour.resetAtMillis(),s.weekly.resetAtMillis()));main(home::rebuild);
            main(()->{TextView status=home.findViewById(android.R.id.content).findViewWithTag("beta998:quota:weekly:status");check(status.isShown()&&status.getText().toString().equals(home.getString(R.string.phase_a_stale)),"Stale reset explicitly labelled");});
            AppPreferences.saveSnapshot(c,new UsageSnapshot("free",true,false,null,null,System.currentTimeMillis()));main(home::rebuild);
            main(()->{View root=home.findViewById(android.R.id.content);check(reset(root,"five-hour").length()==0&&reset(root,"weekly").length()==0,"No invented reset for missing windows");});
            AppPreferences.saveSnapshot(c,s);main(home::rebuild);
            check(BetaGrowthStore.load(c,SubscriptionStore.accountKey(c)).toString().equals(growth),"No growth rewards or progress mutation");check(SecureTokenStore.load(c).toJson().toString().equals(auth),"Synthetic auth preserved");
            result.putString("stream","Home reset native: "+checks+" checks passed; actual home, reset timer, native 320/360dp and 1x/2x font card layouts; owned synthetic read-only AVD only.\n");code=-1;
        }catch(Throwable t){result.putString("stream",android.util.Log.getStackTraceString(t));}
        finally{try{if(home!=null)main(home::finish);if(old!=null)AppPreferences.saveSnapshot(c,old);if(error!=null)AppPreferences.setLastError(c,error);UpdatePreferences.setAutomaticChecks(c,updates);if(locales!=null&&oldLocale!=null)locales.setApplicationLocales(oldLocale);}catch(Throwable t){code=0;result.putString("stream",result.getString("stream","")+android.util.Log.getStackTraceString(t));}}
        finish(code,result);
    }
}
