package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;import java.util.*;import org.json.*;import androidx.appcompat.app.AppCompatDelegate;import androidx.core.os.LocaleListCompat;
public final class Beta995UpgradeSeedTest extends Instrumentation {
 @Override public void onCreate(Bundle a){super.onCreate(a);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{Context c=getTargetContext();if(!Build.MODEL.contains("sdk")||!c.getPackageName().equals("dev.bennett.codexmeter.trial")||!c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("9.9.4")||(SecureTokenStore.load(c)!=null&&!TrialDemo.active(c)))throw new AssertionError("Owned synthetic 9.9.4 only");TrialDemo.prepare(c);String key=SubscriptionStore.accountKey(c);
 c.getSharedPreferences("beta_identity_v1",0).edit().putString("name","업데이트 보존 Alex").putBoolean("setup_done",true).commit();runOnMainSync(()->AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")));Thread.sleep(300);
 c.getSharedPreferences("beta995_upgrade_qa",0).edit().putString("account",key).putString("rpg",RpgProgressStore.export(c,key).toString()).putString("settings",new TreeMap<>(c.getSharedPreferences("codex_meter_settings_v1",0).getAll()).toString()).putString("settings_json",new JSONObject(c.getSharedPreferences("codex_meter_settings_v1",0).getAll()).toString()).putString("language",AppCompatDelegate.getApplicationLocales().toLanguageTags()).commit();result.putString("stream","Beta 9.9.4 upgrade seed: 3 checks passed; synthetic old balance and settings captured.\n");finish(-1,result);
 }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}}
}
