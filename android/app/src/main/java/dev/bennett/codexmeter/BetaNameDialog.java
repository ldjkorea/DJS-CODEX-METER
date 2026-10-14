package dev.bennett.codexmeter;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.*;
/** Fragment state retains an unfinished name across rotation and per-app language changes. */
public final class BetaNameDialog extends AppCompatDialogFragment {
    EditText input;
    @Override public Dialog onCreateDialog(Bundle saved){android.content.Context c=requireContext();boolean dark=Ui.isDark(c);LinearLayout body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(c,20),Ui.dp(c,16),Ui.dp(c,20),Ui.dp(c,16));body.addView(BetaUi.body(c,BetaCopy.t(c,"name_note"),dark));Ui.addSpacer(body,12);
        input=new EditText(c);input.setTextColor(Ui.mainText(dark));input.setHintTextColor(Ui.secondaryText(dark));input.setHint(BetaCopy.t(c,"user"));input.setText(saved==null?BetaIdentity.prefs(c).getString("name",""):saved.getString("draft",""));input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(48)});input.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);input.setContentDescription(BetaCopy.t(c,"name"));input.setTag("beta995:name-edit");body.addView(input);body.addView(BetaUi.note(c,BetaCopy.t(c,"name_limit"),dark));ScrollView scroll=new ScrollView(c);scroll.addView(body);
        return new AlertDialog.Builder(c).setTitle(BetaCopy.t(c,"ask_name")).setView(scroll).setNegativeButton(BetaCopy.t(c,"cancel"),null).setPositiveButton(BetaCopy.t(c,"save"),(d,w)->{BetaIdentity.save(requireContext(),input.getText().toString());requireActivity().recreate();}).create();}
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(input!=null)out.putString("draft",input.getText().toString());}
}
