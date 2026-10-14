package dev.bennett.codexmeter;
import android.content.*;
import androidx.appcompat.app.*;
import android.widget.*;
final class BetaIdentity {
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("beta_identity_v1",0);}
    static String name(Context c){String value=ProfileCard.nickname(prefs(c).getString("name",""));return value.isEmpty()?BetaCopy.t(c,"user"):value;}
    static String title(Context c,String id){return id.isEmpty()?name(c):BetaCatalog.name(id,name(c),BetaCopy.ko(c));}
    static boolean done(Context c){return prefs(c).getBoolean("setup_done",false);}
    static void save(Context c,String value){prefs(c).edit().putString("name",ProfileCard.nickname(value)).putBoolean("setup_done",true).apply();}
    static void edit(AppCompatActivity a,Runnable after){if(a.getSupportFragmentManager().findFragmentByTag("beta-name-editor")==null)new BetaNameDialog().show(a.getSupportFragmentManager(),"beta-name-editor");}
}
