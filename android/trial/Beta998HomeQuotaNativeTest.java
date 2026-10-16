package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Regression for the primary home quota surface; owned synthetic AVD only. */
public final class Beta998HomeQuotaNativeTest extends Instrumentation {
    int checks; Throwable failure; String locale, theme;
    void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    void main(Runnable work) { runOnMainSync(() -> { try { work.run(); } catch (Throwable e) { failure=e; } });
        if (failure!=null) { Throwable e=failure; failure=null; throw new AssertionError(e); } }
    TextView label(View root, String tag) { return root.findViewWithTag("beta998:quota:"+tag); }
    void bounds(View v) {
        if(v.getVisibility()!=View.VISIBLE)return;
        if(v instanceof TextView) { TextView t=(TextView)v; android.text.Layout l=t.getLayout();
            if(l!=null&&t.length()>0) { check(l.getEllipsisCount(l.getLineCount()-1)==0,"No ellipsis");
                check(t.getHeight()+1>=t.getCompoundPaddingTop()+t.getCompoundPaddingBottom()+l.getLineBottom(l.getLineCount()-1),"No clipped quota text: "+t.getText()); } }
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)bounds(((ViewGroup)v).getChildAt(i));
    }
    void capture(Context c) throws Exception {
        java.io.File dir=new java.io.File(c.getExternalFilesDir(null),"beta998-home-limits");dir.mkdirs();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,locale+"-"+theme+".png"))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
        }
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args);locale=args.getString("locale","ko");theme=args.getString("theme","dark");start(); }
    @Override public void onStart() {
        Bundle result=new Bundle(); int code=0; Context c=getTargetContext(); MainActivity home=null;
        SharedPreferences settings=c.getSharedPreferences("codex_meter_settings_v1",0);
        String oldSnapshot=settings.getString("last_snapshot",null), oldError=AppPreferences.getLastError(c);
        String oldPreset=BetaPreferences.preset(c), oldTheme=AppPreferences.getAppTheme(c);
        android.app.LocaleManager locales=c.getSystemService(android.app.LocaleManager.class);
        String oldLocale=locales.getApplicationLocales().toLanguageTags();
        try {
            check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic sandbox");
            String account=SubscriptionStore.accountKey(c), growth=BetaGrowthStore.load(c,account).toString(), tokens=SecureTokenStore.load(c).toJson().toString();
            AppPreferences.setAppTheme(c,theme);locales.setApplicationLocales(LocaleList.forLanguageTags(locale));Thread.sleep(300);
            long now=System.currentTimeMillis();
            UsageWindow five=new UsageWindow(25.25,18000,0,(now+7200000)/1000), week=new UsageWindow(66.75,604800,0,(now+3*86400000L)/1000);
            UsageSnapshot sample=new UsageSnapshot("plus",true,false,five,week,now);
            check(AppPreferences.saveSnapshot(c,sample),"Synthetic cached quota persisted");
            home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Thread.sleep(1200);BetaGrowthStore.WORKER.submit(()->{}).get();waitForIdleSync();
            MainActivity a=home;
            for(String preset:new String[]{"simple","growth","detail"}) {
                BetaPreferences.prefs(c).edit().putString("preset",preset).commit();
                main(a::rebuild);waitForIdleSync();Thread.sleep(120);
                main(()->{
                    View root=a.findViewById(android.R.id.content), quota=root.findViewWithTag("beta998:quota"), crest=root.findViewWithTag("beta996:identity");
                    check(quota!=null&&crest!=null,"Quota and tier identity retained in "+preset);
                    ViewGroup parent=(ViewGroup)quota.getParent();View profile=(View)crest.getParent();
                    check(parent.indexOfChild(profile)==0,"Tier and level box is first in "+preset);
                    check(profile.getBottom()<=quota.getTop(),"Quota directly below profile");
                    View focus=root.findViewWithTag("beta998:focus");
                    check(parent.indexOfChild(profile)<parent.indexOfChild(quota)&&parent.indexOfChild(quota)<parent.indexOfChild(focus),"Profile then quota then focus in every preset");
                    check(label(root,"five-hour:value").getText().toString().equals(UsagePrecision.remaining(five,AppPreferences.usageDecimalPlaces(c))),"5-hour remaining, not used");
                    check(label(root,"weekly:value").getText().toString().equals(UsagePrecision.remaining(week,AppPreferences.usageDecimalPlaces(c))),"Weekly remaining, not used");
                    check(label(root,"five-hour:reset").length()>0&&label(root,"weekly:reset").length()>0,"Both reset countdowns visible");
                    if(a.getResources().getConfiguration().fontScale<=1.3f&&a.getResources().getConfiguration().orientation==1) {
                        int[] position=new int[2];label(root,"weekly:value").getLocationOnScreen(position);
                        View scroll=a.findViewById(R.id.dashboard_scroll);android.graphics.Rect viewport=new android.graphics.Rect();scroll.getGlobalVisibleRect(viewport);
                        check(position[1]+label(root,"weekly:value").getHeight()<=viewport.bottom,"Both values in initial viewport");
                    }
                    bounds(quota);
                });
            }
            capture(c);
            AppPreferences.setLastError(c,"SYNTHETIC_OFFLINE");main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"five-hour:status").getText().toString().equals(a.getString(R.string.phase_a_cached_failed)),"Cached failure marked");check(label(root,"weekly:value").getText().toString().equals(UsagePrecision.remaining(week,0)),"Failure retains last observed value");});
            UsageSnapshot missing=new UsageSnapshot("free",true,false,null,null,now);AppPreferences.saveSnapshot(c,missing);main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"five-hour:value").getText().toString().equals("—")&&label(root,"weekly:value").getText().toString().equals("—"),"Unprovided windows never invented as 0 or 100");});
            settings.edit().remove("last_snapshot").commit();main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"five-hour:status").length()>0&&label(root,"weekly:value").getText().toString().equals("—"),"No snapshot still has both labelled placeholders");});
            AppPreferences.saveSnapshot(c,new UsageSnapshot("plus",true,false,five,week,now-86400000));main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"weekly:status").getText().toString().equals(a.getString(R.string.phase_a_stale)),"Stale observation clearly marked");});
            UsageSnapshot expired=new UsageSnapshot("plus",true,false,new UsageWindow(25,18000,0,(now-60000)/1000),new UsageWindow(67,604800,0,(now-60000)/1000),now);
            AppPreferences.saveSnapshot(c,expired);main(a::rebuild);waitForIdleSync();
            main(()->{View root=a.findViewById(android.R.id.content);check(label(root,"weekly:reset").getText().toString().equals(a.getString(R.string.phase_a_reset_passed)),"Expired weekly is not reset to full");});
            check(BetaGrowthStore.load(c,account).toString().equals(growth),"Rendering never changes growth evidence or rewards");
            check(SecureTokenStore.load(c).toJson().toString().equals(tokens),"Rendering preserves authentication");
            result.putString("stream","Beta998 home limits: "+checks+" checks passed; "+locale+" / "+theme+" / font "+c.getResources().getConfiguration().fontScale+".\n");code=-1;
        } catch(Throwable e) {result.putString("stream",android.util.Log.getStackTraceString(e));}
        finally {
            if(home!=null){MainActivity a=home;main(a::finish);}
            SharedPreferences.Editor edit=settings.edit();if(oldSnapshot==null)edit.remove("last_snapshot");else edit.putString("last_snapshot",oldSnapshot);edit.commit();
            AppPreferences.setLastError(c,oldError);BetaPreferences.prefs(c).edit().putString("preset",oldPreset).commit();
            AppPreferences.setAppTheme(c,oldTheme);locales.setApplicationLocales(LocaleList.forLanguageTags(oldLocale));
        }
        finish(code,result);
    }
}
