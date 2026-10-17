package dev.bennett.codexmeter;
import android.content.*;
import android.os.Bundle;
import android.text.*;
import android.widget.*;
import androidx.appcompat.app.*;
public final class BetaSetupActivity extends AppCompatActivity {
    EditText name;
    @Override protected void onCreate(Bundle saved){Ui.applySelectedTheme(this);super.onCreate(saved);boolean dark=Ui.isDark(this);LinearLayout body=Ui.installPage(this,"GPT HUD",false).content;
        body.addView(BetaUi.title(this,BetaCopy.t(this,"ask_name"),dark));Ui.addSpacer(body,8);body.addView(BetaUi.body(this,BetaCopy.t(this,"name_note"),dark));Ui.addSpacer(body,16);
        name=new EditText(this);name.setTextColor(Ui.mainText(dark));name.setHintTextColor(Ui.secondaryText(dark));name.setTextSize(16);name.setHint(BetaCopy.t(this,"user"));name.setContentDescription(BetaCopy.t(this,"name"));name.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)});
        name.setText(saved!=null?saved.getString("draft",""):BetaIdentity.prefs(this).getString("draft_name",BetaIdentity.prefs(this).getString("name","")));name.setTag("beta995:name");body.addView(name);body.addView(BetaUi.note(this,BetaCopy.t(this,"name_limit"),dark));
        name.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){BetaIdentity.prefs(BetaSetupActivity.this).edit().putString("draft_name",s.toString()).apply();}public void afterTextChanged(Editable e){}});
        body.addView(BetaUi.row(this,BetaCopy.t(this,"language"),AppLanguage.currentName(this),dark,()->AppLanguage.show(this,false)));
        body.addView(BetaUi.action(this,BetaCopy.t(this,"continue"),true,dark,()->finishSetup(name.getText().toString())));body.addView(BetaUi.action(this,BetaCopy.t(this,"skip"),false,dark,()->finishSetup("")));
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=findViewById(R.id.dashboard_refresh);refresh.setEnabled(false);refresh.setRefreshing(false);
    }
    void finishSetup(String value){BetaIdentity.prefs(this).edit().putString("name",ProfileCard.nickname(value)).putBoolean("setup_done",true).remove("draft_name").commit();getSharedPreferences("gpt_hud_language",0).edit().putBoolean("chosen",true).apply();startActivity(new Intent(this,MainActivity.class));finish();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("draft",name.getText().toString());}
}
