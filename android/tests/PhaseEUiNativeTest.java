package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Production Activity code, representative resource configurations, synthetic accounts only. */
public final class PhaseEUiNativeTest extends Instrumentation {
    Context c; Activity active; int checks; Throwable failure; Configuration profileConfiguration;
    @Override public Activity newActivity(ClassLoader loader,String name,Intent intent)throws InstantiationException,IllegalAccessException,ClassNotFoundException{
        Activity activity=super.newActivity(loader,name,intent);
        if(profileConfiguration!=null)activity.applyOverrideConfiguration(profileConfiguration);
        return activity;
    }
    void check(boolean value,String note){checks++;if(!value)throw new AssertionError(note);}
    void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable e){failure=e;}});if(failure!=null)throw new AssertionError(failure);waitForIdleSync();}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;int night=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.phaseeqa")&&Build.MODEL.contains("sdk"),"Owned integration QA only");
        AppPreferences.setRefreshOnLaunch(c,false);UpdatePreferences.setAutomaticChecks(c,false);
        long now=System.currentTimeMillis();AppPreferences.saveSnapshot(c,new UsageSnapshot("pro",true,false,null,new UsageWindow(42.125,604800,0,(now+86400000)/1000),now));
        long xp=RpgProgressStore.export(c,SubscriptionStore.accountKey(c)).getJSONObject("rpg").getLong("xp");
        int[] widths={320,360,411,700,320,360,411,700};float[] fonts={2f,1.5f,1f,2f,1.5f,2f,2f,1f};
        List<WeakReference<Activity>> retired=new ArrayList<>();long[] entry=new long[7];int frames=0;
        for(int profile=0;profile<widths.length;profile++){
            final int width=widths[profile];final float font=fonts[profile];final boolean dark=profile%3!=0;final Locale locale=profile%2==0?Locale.KOREAN:Locale.ENGLISH;
            main(()->{Configuration conf=new Configuration(c.getResources().getConfiguration());conf.screenWidthDp=width;conf.fontScale=font;conf.setLocale(locale);conf.uiMode=(conf.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(dark?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);profileConfiguration=conf;AppPreferences.setAppTheme(c,dark?"dark":"light");});
            Class<?>[] pages={MainActivity.class,RecordsActivity.class,GrowthActivity.class,MeterSettingsActivity.class,RpgCollectionActivity.class,RpgCollectionActivity.class,RpgBackupActivity.class};
            for(int index=0;index<pages.length;index++){
                long start=SystemClock.elapsedRealtime();Intent intent=new Intent(c,pages[index]).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(index==5)intent.putExtra("collection_mode","titles");
                active=startActivitySync(intent);waitForIdleSync();Thread.sleep(300);
                check(active.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(locale.getLanguage()),"Activity actually uses requested language");
                check(Math.abs(active.getResources().getConfiguration().fontScale-font)<.01f,"Activity actually uses requested font size");
                if(active instanceof RpgCollectionActivity){RpgCollectionActivity a=(RpgCollectionActivity)active;for(int j=0;j<100;j++){boolean[] done={false};main(()->done[0]=!a.busy&&a.state!=null);if(done[0])break;Thread.sleep(30);}check(a.state!=null,"Real collection data ready");}
                entry[index]+=SystemClock.elapsedRealtime()-start;String name=profile+"-"+pages[index].getSimpleName()+(index==5?"-titles":"");
                main(()->{View root=active.findViewById(android.R.id.content);int w=Ui.dp(active,width),h=Ui.dp(active,1000);root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);check(root.getWidth()==w&&root.getHeight()>0,"Real screen renders configured width");Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.drawColor(Ui.background(active,dark));root.draw(canvas);File folder=new File(c.getExternalFilesDir(null),"phase-e-ui");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new RuntimeException(e);}image.recycle();check(root instanceof ViewGroup&&((ViewGroup)root).getChildCount()>0,"Actual screen has content");});frames++;
                retired.add(new WeakReference<>(active));main(active::finish);active=null;
            }
        }
        System.gc();System.runFinalization();Thread.sleep(800);System.gc();int alive=0;for(WeakReference<Activity> ref:retired)if(ref.get()!=null)alive++;
        check(RpgProgressStore.export(c,SubscriptionStore.accountKey(c)).getJSONObject("rpg").getLong("xp")==xp,"Opening screens does not award duplicate XP");
        result.putString("stream","Phase E UI: "+checks+" checks passed; "+frames+" Activity captures; representative resource profiles, not physical fold/TalkBack proof.\nRetired Activity weak references after GC: "+alive+"/"+retired.size()+" (diagnostic, not long-term leak certification).\nEntry totals ms: "+Arrays.toString(entry)+"\n");code=-1;
    }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));}finally{if(active!=null)main(active::finish);main(()->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(night));}finish(code,result);}
}
