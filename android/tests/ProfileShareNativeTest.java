package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Production share Activity, renderer and FileProvider. Synthetic emulator data only. */
public final class ProfileShareNativeTest extends Instrumentation {
    Context c;Activity active;int checks;Throwable error;File folder;android.content.res.Configuration configuration;
    @Override public Activity newActivity(ClassLoader loader,String name,Intent intent)throws InstantiationException,IllegalAccessException,ClassNotFoundException{Activity a=super.newActivity(loader,name,intent);if(configuration!=null)a.applyOverrideConfiguration(configuration);return a;}
    void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    void main(Runnable r){runOnMainSync(()->{try{r.run();}catch(Throwable e){error=e;}});if(error!=null)throw new AssertionError(error);waitForIdleSync();}
    void ready(ProfileShareActivity a)throws Exception{for(int i=0;i<150;i++){Thread.sleep(40);boolean[] ready={false};main(()->ready[0]=!a.busy&&a.png!=null);if(ready[0])return;}throw new AssertionError("Preview not ready");}
    void image(Bitmap bitmap,String name)throws Exception{try(FileOutputStream out=new FileOutputStream(new File(folder,name))){check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out),"PNG generated");}}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle out=new Bundle();try{
        c=getTargetContext();check(c.getPackageName().equals("dev.bennett.codexmeter.rpg3qa")&&Build.MODEL.contains("sdk"),"Isolated emulator only");folder=new File(c.getExternalFilesDir(null),"profile-fixtures");folder.mkdirs();
        AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);UpdatePreferences.setAutomaticChecks(c,false);
        SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_PROFILE_ACCESS","SYNTHETIC_PROFILE_REFRESH","",System.currentTimeMillis()+86400000,"profile-"+UUID.randomUUID(),""));String account=SubscriptionStore.accountKey(c);
        active=startActivitySync(new Intent(c,ProfileShareActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));ProfileShareActivity a=(ProfileShareActivity)active;ready(a);
        main(()->check(a.nickname.getText().length()==0&&!a.date.isChecked()&&a.save.isEnabled(),"Anonymous, no date, working empty-state preview"));image(a.image,"empty.png");
        RpgProgressStore.tokens(c,account,new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",System.currentTimeMillis()).put("summary",new JSONObject().put("lifetimeTokens",100000000)));
        RpgProgressStore.setAchievementsEnabled(c,account,true);RpgProgressStore.setTitlesEnabled(c,account,true);Thread.sleep(10);
        RpgProgressStore.tokens(c,account,new JSONObject().put("account",account).put("source","codex_account_usage_v1").put("at",System.currentTimeMillis()).put("summary",new JSONObject().put("lifetimeTokens",200000000)));
        JSONObject s=RpgProgressStore.export(c,account).getJSONObject("rpg");List<String> medals=RpgGrowthFacts.earned(s);if(!medals.isEmpty())check(RpgShowcase.save(c,account,medals.subList(0,Math.min(3,medals.size())),s),"Real chosen medals saved");List<String> titles=RpgTitleCatalog.owned(s);if(!titles.isEmpty())check(RpgProgressStore.equip(c,account,titles.get(0)),"Real earned title equipped");
        String before=RpgProgressStore.archive(c,account).getJSONObject("document").toString();main(()->{a.nickname.setText("Synthetic Explorer");a.reload();});ready(a);image(a.image,"populated.png");
        main(()->{for(int width:new int[]{320,360,411}){View root=a.getWindow().getDecorView();int w=Ui.dp(a,width),h=Ui.dp(a,900);root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);check(a.share.getWidth()<=w&&a.save.getMeasuredHeight()>=Ui.dp(a,48),"Narrow preview and touch controls");Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bmp));try{image(bmp,"screen-"+width+".png");}catch(Exception e){throw new RuntimeException(e);}bmp.recycle();}});
        ProfileCard model=ProfileCard.from(s,medals,"Synthetic Explorer");for(boolean dark:new boolean[]{false,true}){Bitmap b=ProfileCardRenderer.render(c,model,dark,false);image(b,dark?"dark.png":"light.png");b.recycle();}
        File cache=new File(c.getCacheDir(),"profile-share");cache.mkdirs();File file=new File(cache,"synthetic.png");try(FileOutputStream stream=new FileOutputStream(file)){stream.write(a.png);}Uri uri=androidx.core.content.FileProvider.getUriForFile(c,c.getPackageName()+".profile.files",file);
        check(uri.getScheme().equals("content")&&!uri.toString().contains(account),"Opaque content URI without identity");check(c.getContentResolver().getType(uri).equals("image/png"),"Correct PNG type");
        main(()->{a.pending=a.png.clone();a.pendingAccount=a.account;a.saveTo(uri);});
        for(int i=0;i<100;i++){Thread.sleep(20);boolean[] done={false};main(()->done[0]=!a.busy);if(done[0])break;}main(()->check(a.status.getText().toString().equals(a.getString(R.string.profile_saved)),"Actual document stream saves successfully"));
        boolean denied=false;try{androidx.core.content.FileProvider.getUriForFile(c,c.getPackageName()+".profile.files",c.getDatabasePath(RpgProgressStore.NAME));}catch(IllegalArgumentException expected){denied=true;}check(denied,"Provider cannot expose database");
        main(()->{a.pending=a.png.clone();a.pendingAccount=a.account;a.saveTo(null);check(a.pending==null&&a.status.getText().toString().equals(a.getString(R.string.profile_cancelled)),"Save cancellation");a.pending=a.png.clone();a.pendingAccount=a.account;a.saveTo(Uri.parse("content://invalid.synthetic.provider/failure"));});
        for(int i=0;i<100;i++){Thread.sleep(20);boolean[] done={false};main(()->done[0]=!a.busy);if(done[0])break;}main(()->check(a.status.getText().toString().equals(a.getString(R.string.profile_failed)),"Save failure reported"));
        ActivityMonitor monitor=addMonitor(new IntentFilter(Intent.ACTION_CHOOSER),new ActivityResult(Activity.RESULT_CANCELED,null),true);main(a::shareImage);
        for(int i=0;i<100&&monitor.getHits()==0;i++)Thread.sleep(20);check(monitor.getHits()==1,"Explicit share opens chooser once, no message transmitted");removeMonitor(monitor);
        check(before.equals(RpgProgressStore.archive(c,account).getJSONObject("document").toString()),"Preview/save failure/chooser do not mutate RPG");
        main(()->{try{SecureTokenStore.clear(c);a.reload();check(a.png==null&&a.preview.getDrawable()==null&&!a.share.isEnabled(),"Account change clears preview and disables sharing");}catch(Exception e){throw new RuntimeException(e);}});
        main(a::finish);
        SecureTokenStore.save(c,new AuthTokens("SYNTHETIC_PROFILE_ACCESS","SYNTHETIC_PROFILE_REFRESH","",System.currentTimeMillis()+86400000,"profile-font-"+UUID.randomUUID(),""));
        for(int i=0;i<2;i++){
            final int index=i;android.content.res.Configuration conf=new android.content.res.Configuration(c.getResources().getConfiguration());conf.screenWidthDp=i==0?320:411;conf.fontScale=i==0?1.5f:2f;conf.setLocale(i==0?Locale.KOREAN:Locale.ENGLISH);configuration=conf;
            main(()->AppPreferences.setAppTheme(c,index==0?"light":"dark"));active=startActivitySync(new Intent(c,ProfileShareActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));ProfileShareActivity page=(ProfileShareActivity)active;ready(page);
            main(()->{check(page.getResources().getConfiguration().fontScale==conf.fontScale,"Actual large font configuration");View root=page.getWindow().getDecorView();int width=Ui.dp(page,conf.screenWidthDp),height=Ui.dp(page,1200);root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);check(page.share.getWidth()<=width&&page.share.getMeasuredHeight()>=Ui.dp(page,48),"Large font controls do not overlap");Bitmap shot=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);root.draw(new Canvas(shot));try{image(shot,"large-font-"+index+".png");}catch(Exception e){throw new RuntimeException(e);}shot.recycle();});main(page::finish);
        }
        JSONObject showState=new JSONObject(s.toString());showState.getJSONObject("KR").put("tier",6);Bitmap crest=ProfileCardRenderer.render(c,ProfileCard.from(showState,medals,"Synthetic Explorer"),true,true);image(crest,"tier-example.png");crest.recycle();
        out.putString("stream","Profile share native: "+checks+" checks passed; synthetic emulator only\n");finish(-1,out);
    }catch(Throwable e){out.putString("stream",android.util.Log.getStackTraceString(e));finish(0,out);}finally{if(active!=null)main(active::finish);}}
}
