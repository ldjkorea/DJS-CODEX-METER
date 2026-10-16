package dev.bennett.codexmeter;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import org.json.*;
/** Explicit SAF operations with durable pending files across recreation. */
public final class BetaBackupActivity extends AppCompatActivity {
    String account,pendingName,importUri;LinearLayout body;AlertDialog dialog;long generation;CheckBox growth,settings;
    final ActivityResultLauncher<String> create=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),this::write);
    final ActivityResultLauncher<String[]> open=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{importUri=uri==null?null:uri.toString();read(uri);});
    String t(String ko,String en,Object... args){return BetaExperienceUi.t(this,ko,en,args);}
    @Override protected void onCreate(Bundle saved){
        Ui.applySelectedTheme(this);super.onCreate(saved);boolean dark=Ui.isDark(this);Ui.Page layout=Ui.installPage(this,"",true);layout.toolbar.setTitle("");body=layout.content;body.addView(BetaUi.title(this,BetaCopy.t(this,"backup"),dark));if(TrialDemo.active(this))body.addView(BetaUi.note(this,BetaCopy.t(this,"demo"),dark));
        if(saved!=null){account=saved.getString("account");pendingName=saved.getString("pending");importUri=saved.getString("import_uri");}
        body.addView(BetaUi.body(this,t("보관할 항목을 선택하세요","Choose sections to save"),dark));
        growth=new CheckBox(this);growth.setText(t("성장·획득·장착·이전 보관함","Growth, awards, equipment and archive"));growth.setTextColor(Ui.mainText(dark));growth.setMinHeight(Ui.dp(this,48));growth.setChecked(saved==null||saved.getBoolean("growth",true));body.addView(growth);
        settings=new CheckBox(this);settings.setText(t("이름·표시·알림·연결 설정 (로그인 제외)","Name, display, notifications and connection settings (no credentials)"));settings.setTextColor(Ui.mainText(dark));settings.setMinHeight(Ui.dp(this,48));settings.setChecked(saved==null||saved.getBoolean("settings",true));body.addView(settings);
        body.addView(BetaUi.note(this,t("성장은 병합합니다. 선택한 설정은 파일의 값으로 바뀝니다. 로그인 토큰·대화 내용·사용량 전체 이력은 포함하지 않습니다.","Growth is merged. Selected settings replace current values. No tokens, conversations or full usage history are included."),dark));
        body.addView(BetaUi.action(this,BetaCopy.t(this,"export"),true,dark,this::export));body.addView(BetaUi.action(this,BetaCopy.t(this,"import"),false,dark,()->{account=SubscriptionStore.accountKey(this);open.launch(new String[]{"application/json","text/plain"});}));findViewById(R.id.dashboard_refresh).setEnabled(false);
        if(importUri!=null)body.post(()->read(Uri.parse(importUri)));
    }
    @Override protected void onSaveInstanceState(Bundle saved){saved.putString("account",account);saved.putString("pending",pendingName);saved.putString("import_uri",importUri);saved.putBoolean("growth",growth.isChecked());saved.putBoolean("settings",settings.isChecked());super.onSaveInstanceState(saved);}
    @Override protected void onDestroy(){generation++;if(dialog!=null)dialog.dismiss();super.onDestroy();}
    void message(String key){Toast.makeText(this,BetaCopy.t(this,key),Toast.LENGTH_LONG).show();}
    java.io.File cached(String name)throws Exception {if(name==null||!name.matches("[a-f0-9-]{36}\\.json"))throw new IllegalArgumentException("Backup cache name");java.io.File folder=new java.io.File(getCacheDir(),"beta-backup");if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException();return new java.io.File(folder,name);}
    void export(){account=SubscriptionStore.accountKey(this);String key=account;boolean includeGrowth=growth.isChecked(),includeSettings=settings.isChecked();long ticket=++generation;
        BetaGrowthStore.WORKER.execute(()->{String fileName=null;try{JSONObject bundle=BetaBundle.collect(this,key,includeGrowth,includeSettings);String candidate=java.util.UUID.randomUUID()+".json";try(java.io.FileOutputStream out=new java.io.FileOutputStream(cached(candidate))){out.write(bundle.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}fileName=candidate;}catch(Exception ignored){}String ready=fileName;runOnUiThread(()->{if(ticket!=generation||!BetaUi.current(this,key))return;if(ready==null){message("failed");return;}pendingName=ready;create.launch("GPT-HUD-BETA-backup.json");});});
    }
    void write(Uri uri){String fileName=pendingName;pendingName=null;String key=account;if(uri==null||fileName==null||!BetaUi.current(this,key))return;
        BetaGrowthStore.WORKER.execute(()->{boolean ok=false;try{if(!BetaGrowthStore.current(this,key))return;try(java.io.InputStream in=new java.io.FileInputStream(cached(fileName));java.io.OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new java.io.IOException();byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))>=0;)out.write(buffer,0,n);out.flush();}ok=true;}catch(Exception ignored){}boolean success=ok;runOnUiThread(()->{if(BetaUi.current(this,key))message(success?"saved":"failed");});});
    }
    void read(Uri uri){String key=account;if(uri==null||!BetaUi.current(this,key))return;long ticket=++generation;boolean includeGrowth=growth.isChecked(),includeSettings=settings.isChecked();
        BetaGrowthStore.WORKER.execute(()->{JSONObject file=null,preview=null,current=null;try(java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            if(in==null)throw new java.io.IOException();byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))>=0;){if(out.size()+n>8*1024*1024)throw new java.io.IOException();out.write(buffer,0,n);}file=new JSONObject(out.toString("UTF-8"));preview=BetaBundle.preview(this,key,file,includeGrowth,includeSettings);current=BetaGrowthStore.load(this,key);
        }catch(Exception ignored){}JSONObject candidate=file,after=preview,before=current;runOnUiThread(()->{if(ticket!=generation||!BetaUi.current(this,key))return;if(after==null){importUri=null;message("backup_invalid");return;}
            boolean bundle=BetaBundle.bundle(candidate);String note=BetaCopy.t(this,"restore_note",LedgerUi.number(before==null?0:before.optLong("total")),LedgerUi.number(after.optLong("total")),before==null?0:before.optJSONObject("honors").length(),after.optJSONObject("honors").length(),before==null?0:before.optJSONObject("titles").length(),after.optJSONObject("titles").length())+"\n\n"+t("성장 %s · 설정 %s","Growth %s · settings %s",!bundle||includeGrowth?"✓":"—",bundle&&includeSettings?"✓":"—")+"\n"+t("로그인 정보 제외 · 선택한 설정은 교체","Credentials excluded · selected settings replaced");
            if(dialog!=null)dialog.dismiss();dialog=new AlertDialog.Builder(this).setTitle(BetaCopy.t(this,"restore_preview")).setMessage(note).setNegativeButton(BetaCopy.t(this,"cancel"),(d,w)->importUri=null).setOnCancelListener(d->importUri=null).setPositiveButton(BetaCopy.t(this,"restore"),(d,w)->{importUri=null;BetaGrowthStore.WORKER.execute(()->{String outcome;try{outcome=BetaBundle.restore(this,key,candidate,includeGrowth,includeSettings);}catch(Exception error){outcome=error.getMessage()==null?BetaCopy.t(this,"failed"):error.getMessage();}String result=outcome;runOnUiThread(()->{if(BetaUi.current(this,key))new AlertDialog.Builder(this).setTitle(BetaCopy.t(this,"restore")).setMessage(result).setPositiveButton(BetaCopy.t(this,"close"),null).show();});});}).create();dialog.show();
        });});
    }
}
