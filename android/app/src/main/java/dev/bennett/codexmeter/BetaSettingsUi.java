package dev.bennett.codexmeter;
import android.content.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.function.Consumer;
final class BetaSettingsUi {
    static void hub(AppCompatActivity a,LinearLayout parent,boolean dark,Consumer<Boolean> export,Runnable clear,Runnable details){
        LinearLayout info=BetaUi.group(parent,BetaCopy.t(a,"my_info"),dark);info.addView(BetaUi.row(a,BetaCopy.t(a,"name"),BetaIdentity.name(a),dark,()->BetaIdentity.edit(a,a::recreate)));
        JSONObject cached=BetaGrowthStore.peek(a);info.addView(BetaUi.row(a,BetaCopy.t(a,"title"),cached==null||cached.optString("equipped").isEmpty()?BetaCopy.t(a,"no_title"):BetaIdentity.title(a,cached.optString("equipped")),dark,()->a.startActivity(new Intent(a,BetaCatalogActivity.class).putExtra("titles",true))));
        if(SubscriptionStore.key(a,AppPreferences.loadSnapshot(a))!=null)info.addView(BetaUi.row(a,BetaCopy.ko(a)?"구독 · 결제 기간":"Subscription and billing period","",dark,()->SubscriptionUi.edit(a,AppPreferences.loadSnapshot(a),dark,a::recreate)));
        info.addView(BetaUi.row(a,BetaExperienceUi.t(a,"홈 화면 구성","Home layout"),"",dark,()->BetaExperienceUi.preset(a)));
        info.addView(BetaUi.row(a,BetaExperienceUi.t(a,"연결·업데이트 복구","Connection and update recovery"),"",dark,()->BetaExperienceUi.open(a,"health")));
        info.addView(BetaUi.row(a,BetaExperienceUi.t(a,"백업·복원","Backup and restore"),"",dark,()->a.startActivity(new Intent(a,BetaBackupActivity.class))));
        LinearLayout display=BetaUi.group(parent,BetaCopy.t(a,"display"),dark);display.addView(BetaUi.row(a,BetaCopy.t(a,"language"),AppLanguage.currentName(a),dark,()->AppLanguage.show(a,false)));display.addView(BetaUi.row(a,BetaCopy.t(a,"theme"),"",dark,()->page(a,"appearance")));display.addView(BetaUi.row(a,BetaCopy.t(a,"display_options"),"",dark,()->a.startActivity(new Intent(a,DashboardReorderActivity.class))));
        LinearLayout connections=BetaUi.group(parent,BetaCopy.t(a,"connections"),dark);connections.addView(BetaUi.row(a,BetaCopy.t(a,"account"),"",dark,()->MatteNav.homeAction(a,"account")));connections.addView(BetaUi.row(a,BetaCopy.t(a,"automatic"),"",dark,()->page(a,"refresh_usage")));connections.addView(BetaUi.row(a,BetaCopy.t(a,"notifications"),"",dark,()->page(a,"notifications")));connections.addView(BetaUi.row(a,BetaCopy.t(a,"devices"),"",dark,()->a.startActivity(new Intent(a,WidgetGuideActivity.class))));
        LinearLayout app=BetaUi.group(parent,BetaCopy.t(a,"app_info"),dark);app.addView(BetaUi.note(a,"GPT HUD · "+AppConstants.VERSION_NAME,dark));app.addView(BetaUi.row(a,BetaCopy.t(a,"updates"),"",dark,()->a.startActivity(new Intent(a,UpdateActivity.class))));app.addView(BetaUi.row(a,BetaCopy.t(a,"help"),"",dark,()->a.startActivity(new Intent(a,AboutActivity.class))));
        LinearLayout advanced=BetaUi.group(parent,"",dark),items=new LinearLayout(a);items.setOrientation(LinearLayout.VERTICAL);items.setVisibility(android.view.View.GONE);
        android.view.View toggle=BetaUi.row(a,BetaCopy.t(a,"advanced"),BetaCopy.t(a,"advanced_note"),dark,()->{boolean show=items.getVisibility()!=android.view.View.VISIBLE;items.setVisibility(show?android.view.View.VISIBLE:android.view.View.GONE);});toggle.setTag("beta995:advanced");advanced.addView(toggle);advanced.addView(items);
        String account=SubscriptionStore.accountKey(a);items.addView(BetaUi.row(a,BetaCopy.t(a,"archive"),BetaCopy.t(a,"readonly"),dark,()->a.startActivity(new Intent(a,BetaArchiveActivity.class))));items.addView(BetaUi.row(a,BetaCopy.t(a,"backup"),"",dark,()->a.startActivity(new Intent(a,BetaBackupActivity.class))));
        if(cached!=null){androidx.appcompat.widget.SwitchCompat on=new androidx.appcompat.widget.SwitchCompat(a);on.setText(BetaCopy.t(a,"growth_enabled"));on.setTextColor(Ui.mainText(dark));on.setTextSize(15);on.setMinHeight(Ui.dp(a,48));on.setChecked(cached.optBoolean("enabled"));on.setOnCheckedChangeListener((b,checked)->{on.setOnCheckedChangeListener(null);BetaUi.task(a,account,()->BetaGrowthStore.enabled(a,account,checked),a::recreate);});items.addView(on);}else if(account!=null&&BetaGrowthStore.displayError(a))items.addView(BetaUi.row(a,BetaCopy.t(a,"retry_record"),BetaCopy.t(a,"record_error"),dark,()->BetaGrowthStore.refreshDisplay(a)));else if(account!=null)items.addView(BetaUi.action(a,BetaCopy.t(a,"new_growth"),true,dark,()->BetaUi.start(a,account,a::recreate)));
        items.addView(BetaUi.row(a,BetaCopy.t(a,"diagnostics"),"",dark,()->BetaDiagnostics.show(a)));
        LinearLayout legacy=new LinearLayout(a);legacy.setOrientation(LinearLayout.VERTICAL);legacy.setVisibility(android.view.View.GONE);items.addView(BetaUi.row(a,BetaCopy.t(a,"legacy_tools"),"",dark,()->{legacy.setVisibility(legacy.getVisibility()==android.view.View.VISIBLE?android.view.View.GONE:android.view.View.VISIBLE);}));items.addView(legacy);
        // Every existing setting, ledger backup, connection and diagnostic action remains reachable.
        RecordSettings.legacyHub(a,legacy,dark,export,clear,details);
    }
    static void page(AppCompatActivity a,String page){a.startActivity(new Intent(a,SettingsActivity.class).putExtra("settings_page",page));}
}
