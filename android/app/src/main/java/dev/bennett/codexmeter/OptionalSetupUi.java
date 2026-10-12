package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;

/** Explicit help only: never changes onboarding, collection or permission settings. */
final class OptionalSetupUi {
    private OptionalSetupUi() { }
    static AlertDialog show(AppCompatActivity a, boolean dark) {
        ScrollView scroll=new ScrollView(a);
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(a,20),Ui.dp(a,12),Ui.dp(a,20),Ui.dp(a,12));scroll.addView(body);
        body.addView(LedgerUi.caption(a,a.getString(R.string.phase_a_optional_note),dark));
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(R.string.phase_a_optional)
                .setView(scroll).setPositiveButton(R.string.ui_done_e9b450,null).create();
        add(a,body,dark,"widgets",R.string.phase_a_setup_widgets,()->a.startActivity(new Intent(a,WidgetGuideActivity.class)),dialog);
        add(a,body,dark,"pc",R.string.phase_a_setup_pc,()->a.startActivity(new Intent(a,TaskStatusActivity.class)),dialog);
        add(a,body,dark,"achievements",R.string.phase_a_setup_achievements,()->a.startActivity(new Intent(a,GrowthActivity.class)),dialog);
        add(a,body,dark,"titles",R.string.phase_a_setup_titles,()->a.startActivity(new Intent(a,GrowthActivity.class)),dialog);
        add(a,body,dark,"payment",R.string.phase_a_setup_payment,()->a.startActivity(new Intent(a,MeterSettingsActivity.class)),dialog);
        add(a,body,dark,"notifications",R.string.phase_a_setup_notifications,()->a.startActivity(new Intent(a,SettingsActivity.class).putExtra("settings_page","notifications")),dialog);
        dialog.show();return dialog;
    }
    private static void add(AppCompatActivity a,LinearLayout body,boolean dark,String key,int text,Runnable action,AlertDialog dialog) {
        Ui.addSpacer(body,8);android.widget.Button button=LedgerUi.action(a,a.getString(text),false,dark,()->{dialog.dismiss();action.run();});
        button.setTag("optional:"+key);body.addView(button,new LinearLayout.LayoutParams(-1,-2));
    }
}
