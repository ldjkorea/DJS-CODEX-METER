package dev.bennett.codexmeter;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Explicit SAF transfer: encrypted metadata only, authenticated account required. */
public final class RpgBackupActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private Button export,restore;private TextView status;private String account;private boolean busy;private char[] pending;private AlertDialog passwordDialog;
    private final ActivityResultLauncher<String> create=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/octet-stream"),uri->file(uri,false));
    private final ActivityResultLauncher<String[]> open=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->file(uri,true));
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);boolean dark=Ui.isDark(this);account=SubscriptionStore.accountKey(this);
        LinearLayout body=Ui.installPage(this,getString(R.string.ref_backup),true).content;body.addView(LedgerUi.heading(this,getString(R.string.ref_backup),dark));body.addView(LedgerUi.caption(this,getString(R.string.ref_backup_note),dark));
        export=LedgerUi.action(this,getString(R.string.ref_export),true,dark,()->password(false));restore=LedgerUi.action(this,getString(R.string.ref_import),false,dark,()->password(true));body.addView(export);body.addView(restore);status=LedgerUi.caption(this,account==null?getString(R.string.rpg3_no_account):getString(R.string.ref_backup_ready),dark);status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(status);enable();
    }
    private boolean valid(){return account!=null&&account.equals(SubscriptionStore.accountKey(this))&&!isDestroyed()&&!isFinishing();}
    private void enable(){export.setEnabled(!busy&&valid());restore.setEnabled(!busy&&valid());}
    private void erase(){if(pending!=null)Arrays.fill(pending,'\0');pending=null;}
    private void password(boolean importing){if(busy||!valid())return;LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(Ui.dp(this,20),0,Ui.dp(this,20),0);
        EditText entry=new EditText(this);entry.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);entry.setHint(R.string.ref_password);entry.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);fields.addView(entry);
        EditText confirm=new EditText(this);confirm.setInputType(entry.getInputType());confirm.setHint(R.string.ref_password_again);confirm.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);if(!importing)fields.addView(confirm);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(importing?R.string.ref_import:R.string.ref_export).setMessage(R.string.ref_password_note).setView(fields).setNegativeButton(android.R.string.cancel,(d,w)->{entry.getText().clear();confirm.getText().clear();}).setPositiveButton(android.R.string.ok,null).create();
        passwordDialog=dialog;dialog.setOnDismissListener(d->{entry.getText().clear();confirm.getText().clear();if(passwordDialog==dialog)passwordDialog=null;});
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{if(entry.length()<10||entry.length()>256||!importing&&!entry.getText().toString().equals(confirm.getText().toString())){entry.setError(getString(R.string.ref_password_error));return;}
            erase();pending=new char[entry.length()];android.text.TextUtils.getChars(entry.getText(),0,entry.length(),pending,0);entry.getText().clear();confirm.getText().clear();dialog.dismiss();
            if(importing)open.launch(new String[]{"application/octet-stream","application/json","*/*"});else create.launch("GPT-HUD-achievements-"+java.time.LocalDate.now()+".gpthud");}));dialog.show();
    }
    private void file(Uri uri,boolean importing){if(uri==null||pending==null||!valid()){erase();return;}char[] pass=pending;pending=null;busy=true;enable();status.setText(R.string.ref_working);String expected=account;
        WORKER.execute(()->{boolean ok=false;try{
            if(importing){byte[] bytes;try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException("No input");byte[] chunk=new byte[8192];for(int n;(n=in.read(chunk))!=-1;){out.write(chunk,0,n);if(out.size()>RpgBackupCodec.MAX_BYTES*3/2)throw new IOException("Bound");}bytes=out.toByteArray();}
                JSONObject decoded=RpgBackupCodec.decrypt(bytes,pass);ok=RpgProgressStore.restoreArchive(getApplicationContext(),expected,decoded);
            }else {JSONObject data=RpgProgressStore.archive(getApplicationContext(),expected);byte[] encrypted=RpgBackupCodec.encrypt(data,pass);if(!expected.equals(SubscriptionStore.accountKey(this)))throw new IOException("Account changed");try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No output");out.write(encrypted);out.flush();}ok=true;}
        }catch(Exception ignored){}finally{Arrays.fill(pass,'\0');}boolean done=ok;runOnUiThread(()->{if(isDestroyed()||isFinishing())return;busy=false;enable();if(!valid()){status.setText(R.string.rpg3_no_account);return;}status.setText(done?(importing?R.string.ref_restored:R.string.ref_exported):R.string.ref_backup_failed);if(done&&importing){sendBroadcast(new Intent(AppConstants.ACTION_USAGE_UPDATED).setPackage(getPackageName()));WidgetRenderer.updateAll(this);}});});
    }
    @Override protected void onDestroy(){if(passwordDialog!=null)passwordDialog.dismiss();erase();super.onDestroy();}
    @Override public boolean onSupportNavigateUp(){finish();return true;}
}
