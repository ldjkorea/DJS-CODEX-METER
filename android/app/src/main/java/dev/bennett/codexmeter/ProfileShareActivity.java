package dev.bennett.codexmeter;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Local-only preview. Each save/share needs an explicit user action and current account check. */
public final class ProfileShareActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    String account; ImageView preview; Bitmap image; byte[] png; TextView status; Button save,share;
    EditText nickname; CheckBox date; boolean dark,busy; volatile long generation; boolean registered;
    final android.os.Handler ui=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable redraw=this::reload;
    byte[] pending;String pendingAccount;
    final ActivityResultLauncher<String> create=registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"),this::saveTo);
    final BroadcastReceiver changed=new BroadcastReceiver(){public void onReceive(Context c,Intent i){reload();}};
    @Override protected void onCreate(Bundle state){Ui.applySelectedTheme(this);super.onCreate(state);dark=Ui.isDark(this);account=state==null?SubscriptionStore.accountKey(this):state.getString("account");
        LinearLayout body=Ui.installPage(this,getString(R.string.profile_share),true).content;
        body.addView(LedgerUi.caption(this,getString(R.string.profile_privacy),dark));
        nickname=new EditText(this);nickname.setSingleLine(true);nickname.setTextColor(Ui.mainText(dark));nickname.setHintTextColor(Ui.secondaryText(dark));nickname.setHint(R.string.profile_nickname);nickname.setContentDescription(getString(R.string.profile_nickname));nickname.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        nickname.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)});if(state!=null)nickname.setText(state.getString("nickname",""));body.addView(nickname);
        date=new CheckBox(this);date.setText(R.string.profile_date);date.setTextColor(Ui.mainText(dark));date.setMinHeight(Ui.dp(this,48));date.setChecked(state!=null&&state.getBoolean("date"));body.addView(date);
        preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setContentDescription(getString(R.string.profile_preview));body.addView(preview,new LinearLayout.LayoutParams(-1,-2));
        status=LedgerUi.caption(this,getString(R.string.next_loading),dark);status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(status);
        save=LedgerUi.action(this,getString(R.string.profile_save),false,dark,this::saveImage);share=LedgerUi.action(this,getString(R.string.profile_share_button),true,dark,this::shareImage);body.addView(save);body.addView(share);buttons();
        nickname.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){requestPreview();}public void afterTextChanged(Editable e){}});date.setOnCheckedChangeListener((b,on)->requestPreview());
    }
    @Override protected void onResume(){super.onResume();if(!registered){androidx.core.content.ContextCompat.registerReceiver(this,changed,new IntentFilter(AppConstants.ACTION_USAGE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);registered=true;}reload();}
    @Override protected void onPause(){if(registered){unregisterReceiver(changed);registered=false;}super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("account",account);out.putString("nickname",nickname.getText().toString());out.putBoolean("date",date.isChecked());}
    @Override protected void onDestroy(){generation++;ui.removeCallbacks(redraw);pending=null;png=null;preview.setImageDrawable(null);if(image!=null)image.recycle();super.onDestroy();}
    boolean valid(){return account!=null&&account.equals(SubscriptionStore.accountKey(this))&&!isDestroyed()&&!isFinishing();}
    void buttons(){if(save!=null){save.setEnabled(valid()&&!busy&&png!=null&&pending==null);share.setEnabled(valid()&&!busy&&png!=null&&pending==null);}}
    void clear(){preview.setImageDrawable(null);if(image!=null){image.recycle();image=null;}png=null;pending=null;pendingAccount=null;}
    void requestPreview(){generation++;png=null;busy=true;buttons();ui.removeCallbacks(redraw);ui.postDelayed(redraw,250);}
    void reload(){
        ui.removeCallbacks(redraw);if(isDestroyed())return;String key=SubscriptionStore.accountKey(this);
        if(!java.util.Objects.equals(key,account)){clear();account=key;nickname.setText("");date.setChecked(false);}
        final long ticket=++generation;png=null;buttons();if(key==null){clear();busy=false;status.setText(R.string.rpg3_no_account);buttons();return;}
        busy=true;buttons();status.setText(R.string.next_loading);String name=ProfileCard.nickname(nickname.getText().toString());boolean showDate=date.isChecked();
        WORKER.execute(()->{Bitmap rendered=null;byte[] bytes=null;String description=null;try{
            if(ticket!=generation)return;
            ProfileCard model; synchronized(UsageApi.NETWORK_LOCK){if(!key.equals(SubscriptionStore.accountKey(this)))return;JSONObject journal=RpgProgressStore.export(getApplicationContext(),key);JSONObject state=journal==null?null:journal.optJSONObject("rpg");model=ProfileCard.from(state,RpgShowcase.selected(this,state),name,RpgProgressStore.tier(this));}
            description=ProfileCardRenderer.description(this,model);rendered=ProfileCardRenderer.render(this,model,dark,showDate);ByteArrayOutputStream out=new ByteArrayOutputStream();if(!rendered.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException();bytes=out.toByteArray();
        }catch(Exception ignored){}finally{Bitmap result=rendered;byte[] content=bytes;String accessible=description;runOnUiThread(()->{if(ticket!=generation||!valid()){if(result!=null)result.recycle();return;}busy=false;preview.setImageDrawable(null);if(image!=null)image.recycle();image=result;png=content;if(result!=null)preview.setImageBitmap(result);status.setText(content==null?R.string.profile_failed:R.string.profile_ready);if(content!=null)preview.setContentDescription(accessible);buttons();});}});
    }
    void saveImage(){if(!valid()){reload();return;}if(busy||png==null)return;pending=png.clone();pendingAccount=account;buttons();try{create.launch("GPT-HUD-profile.png");}catch(RuntimeException e){pending=null;pendingAccount=null;status.setText(R.string.profile_failed);buttons();}}
    void saveTo(Uri uri){byte[] bytes=pending;String key=pendingAccount;pending=null;pendingAccount=null;if(uri==null){status.setText(R.string.profile_cancelled);buttons();return;}if(bytes==null||!valid()||!account.equals(key)){reload();return;}
        busy=true;buttons();WORKER.execute(()->{boolean ok=false;try{if(!key.equals(SubscriptionStore.accountKey(this)))throw new IOException();try(OutputStream out=getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IOException();if(!key.equals(SubscriptionStore.accountKey(this)))throw new IOException();out.write(bytes);out.flush();}ok=true;}catch(Exception ignored){}final boolean saved=ok;runOnUiThread(()->{if(!valid()||!key.equals(account))return;busy=false;status.setText(saved?R.string.profile_saved:R.string.profile_failed);buttons();});});
    }
    void shareImage(){if(!valid()){reload();return;}if(busy||png==null)return;byte[] bytes=png.clone();String key=account;busy=true;buttons();
        WORKER.execute(()->{Uri uri=null;try{if(!key.equals(SubscriptionStore.accountKey(this)))throw new IOException();File folder=new File(getCacheDir(),"profile-share");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();File[] old=folder.listFiles();if(old!=null)for(File f:old)if(f.isFile()&&System.currentTimeMillis()-f.lastModified()>86400000L)f.delete();File file=new File(folder,java.util.UUID.randomUUID()+".png");try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);}uri=FileProvider.getUriForFile(this,getPackageName()+".profile.files",file);}catch(Exception ignored){}final Uri result=uri;
            runOnUiThread(()->{if(!valid()||!key.equals(account)){reload();return;}busy=false;buttons();if(result==null){status.setText(R.string.profile_failed);return;}try{Intent send=new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,result).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);send.setClipData(ClipData.newRawUri("GPT HUD profile",result));startActivity(Intent.createChooser(send,getString(R.string.profile_share_button)));status.setText(R.string.profile_share_opened);}catch(RuntimeException e){status.setText(R.string.profile_failed);}});
        });
    }
}
