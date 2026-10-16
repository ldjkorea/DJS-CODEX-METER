package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
/** New-body accessibility matrix, separate from existing toolbar/Wear/OEM gates. */
public final class Beta998UiNativeTest extends Instrumentation {
    String locale,theme;int checks;Throwable failure;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable t){failure=t;}});if(failure!=null){Throwable t=failure;failure=null;throw new AssertionError(t);}}
    void bounds(View view){if(view==null||view.getVisibility()!=View.VISIBLE)return;if(view instanceof TextView){TextView t=(TextView)view;if(t.getLayout()!=null&&t.length()>0){android.text.Layout layout=t.getLayout();check(layout.getEllipsisCount(layout.getLineCount()-1)==0,"No truncated new body text");check(t.getHeight()+1>=t.getCompoundPaddingTop()+t.getCompoundPaddingBottom()+layout.getLineBottom(layout.getLineCount()-1),"No clipped new body text: "+t.getText());}if(view instanceof Button)check(t.getHeight()+1>=48*t.getResources().getDisplayMetrics().density,"48dp target");}if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)bounds(((android.view.ViewGroup)view).getChildAt(i));}
    @Override public void onCreate(Bundle args){super.onCreate(args);locale=args.getString("locale","ko");theme=args.getString("theme","dark");start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();String oldTheme=AppPreferences.getAppTheme(c);android.app.LocaleManager locales=c.getSystemService(android.app.LocaleManager.class);String oldLocale=locales.getApplicationLocales().toLanguageTags();Activity active=null;try{
        check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic fixture");String account=SubscriptionStore.accountKey(c);long xp=BetaGrowthStore.load(c,account).getLong("total");AppPreferences.setAppTheme(c,theme);locales.setApplicationLocales(android.os.LocaleList.forLanguageTags(locale));Thread.sleep(350);
        for(String page:new String[]{"health","journey","recap"}){
            active=startActivitySync(new Intent(c,BetaHubActivity.class).putExtra("page",page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Activity a=active;BetaGrowthStore.WORKER.submit(()->{}).get(20,java.util.concurrent.TimeUnit.SECONDS);waitForIdleSync();Thread.sleep(150);
            check(a.getResources().getConfiguration().getLocales().get(0).getLanguage().equals(locale),"Actual locale");check(Ui.isDark(a)==theme.equals("dark"),"Actual theme");main(()->{View body=a.findViewById(android.R.id.content).findViewWithTag("beta998:"+page);check(body!=null,"Actual ready screen");bounds(body);});main(a::finish);active=null;
        }
        active=startActivitySync(new Intent(c,BetaBackupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));BetaBackupActivity backup=(BetaBackupActivity)active;waitForIdleSync();main(()->bounds(backup.body));main(backup::finish);active=null;
        check(BetaGrowthStore.load(c,account).getLong("total")==xp,"Matrix never changes XP");
        result.putString("stream","Beta998 UI: "+checks+" checks passed; "+locale+" / "+theme+" / font "+c.getResources().getConfiguration().fontScale+"; four new screens.\n");code=-1;
    }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}finally{if(active!=null){Activity a=active;main(a::finish);}AppPreferences.setAppTheme(c,oldTheme);locales.setApplicationLocales(android.os.LocaleList.forLanguageTags(oldLocale));}finish(code,result);}
}
