package dev.bennett.codexmeter;
import android.content.Context;
import androidx.appcompat.app.*;
import androidx.core.os.LocaleListCompat;
/** Reuses Android per-app locales. Empty locale list follows the system. */
final class AppLanguage {
    static boolean needsFirstChoice(Context c){return !AppPreferences.isOnboardingComplete(c)&&!c.getSharedPreferences("gpt_hud_language",0).getBoolean("chosen",false);}
    static int index(){LocaleListCompat locales=AppCompatDelegate.getApplicationLocales();return locales.isEmpty()?0:"ko".equals(locales.get(0).getLanguage())?1:2;}
    static String currentName(Context c){return index()==0?BetaCopy.t(c,"system"):index()==1?"한국어":"English";}
    static AlertDialog show(AppCompatActivity a,boolean first){String[] tags={"","ko","en"},names={BetaCopy.t(a,"system"),"한국어","English"};AlertDialog dialog=new AlertDialog.Builder(a).setTitle(BetaCopy.t(a,"language")).setSingleChoiceItems(names,index(),(d,i)->{a.getSharedPreferences("gpt_hud_language",0).edit().putBoolean("chosen",true).apply();d.dismiss();AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[i]));}).setNegativeButton(BetaCopy.t(a,"cancel"),null).create();dialog.show();return dialog;}
}
