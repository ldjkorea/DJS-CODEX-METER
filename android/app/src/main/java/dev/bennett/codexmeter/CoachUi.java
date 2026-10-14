package dev.bennett.codexmeter;

import android.app.AlertDialog;
import android.content.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

/** Explicit local actions only. Never reads clipboard, refreshes usage or reconciles growth. */
final class CoachUi {
    private static final java.util.concurrent.ExecutorService WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final Map<AppCompatActivity,AlertDialog> OPEN=new WeakHashMap<>();
    private static final Map<AppCompatActivity,String> OWNER=new WeakHashMap<>();
    private CoachUi(){}
    static LinearLayout card(AppCompatActivity a,boolean dark,UsageSnapshot snapshot,boolean fresh,boolean bonus,double daily){
        LinearLayout card=new LinearLayout(a);card.setOrientation(LinearLayout.VERTICAL);
        String account=SubscriptionStore.accountKey(a),tone=FunStore.tone(a),identity=SpicyQuotes.identity(a,account);
        SpicyQuotes.Selection selection=SpicyQuotes.peek(a,account);String group=CoachCatalog.group(snapshot,fresh,!AppPreferences.getLastError(a).isEmpty(),bonus,daily,System.currentTimeMillis());
        if(OPEN.containsKey(a)&&(!identity.equals(OWNER.get(a))||tone.equals("off")||CoachCatalog.constrained(group))){OPEN.get(a).dismiss();OPEN.remove(a);}
        if(tone.equals("off")){card.setVisibility(View.GONE);return card;}
        card.setPadding(Ui.dp(a,10),Ui.dp(a,14),Ui.dp(a,10),Ui.dp(a,14));
        card.addView(LedgerUi.caption(a,a.getString(R.string.live_ai_title),dark));
        boolean ko=RpgGrowthUi.korean(a);String text;
        if(ko&&tone.equals("spicy")){if(selection==null){card.setVisibility(View.GONE);return card;}text=selection.text;}
        else if(CoachCatalog.constrained(group))text=a.getString(R.string.coach_prepare_only);
        else text=FunCoach.current(a,tone,RpgProgressStore.tier(a),FunInsights.Situation.TIER,CoachMoment.Kind.NORMAL,"local",System.currentTimeMillis());
        TextView quote=LedgerUi.heading(a,"“"+text+"”",dark);quote.setTextSize(18);card.addView(quote);
        if(selection!=null&&selection.tip!=null&&!CoachCatalog.constrained(group)&&tone.equals("spicy"))card.addView(LedgerUi.action(a,a.getString(R.string.coach_try),false,dark,()->tip(a,dark,account,selection)));
        if(ko&&tone.equals("spicy"))card.addView(LedgerUi.action(a,a.getString(R.string.coach_more),false,dark,()->new AlertDialog.Builder(a).setItems(new String[]{a.getString(R.string.coach_another),a.getString(R.string.coach_hide)},(dialog,which)->{
            SpicyQuotes.rotate(a.getApplicationContext(),account,snapshot,fresh,bonus,daily,which==1,()->{if(a.isDestroyed()||!card.isAttachedToWindow())return;LinearLayout replacement=card(a,dark,snapshot,fresh,bonus,daily);android.view.ViewGroup parent=(android.view.ViewGroup)card.getParent();if(parent!=null){int index=parent.indexOfChild(card);parent.removeView(card);parent.addView(replacement,index);}});
        }).show()));
        return card;
    }
    private static void tip(AppCompatActivity a,boolean dark,String account,SpicyQuotes.Selection selection){
        if(!selection.identity.equals(SpicyQuotes.identity(a,account))||!account.equals(SubscriptionStore.accountKey(a)))return;
        CoachCatalog.Tip t=selection.tip;if(t==null)return;
        LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(Ui.dp(a,20),Ui.dp(a,12),Ui.dp(a,20),Ui.dp(a,12));
        content.addView(LedgerUi.caption(a,t.inputs+"\n\n"+t.fallback,dark));Ui.addSpacer(content,16);
        TextView prompt=LedgerUi.heading(a,t.prompt,dark);prompt.setTextIsSelectable(true);content.addView(prompt);Ui.addSpacer(content,16);content.addView(LedgerUi.caption(a,t.check,dark));
        ScrollView scroll=new ScrollView(a);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(t.title).setView(scroll).setPositiveButton(R.string.coach_copy,null).setNegativeButton(R.string.coach_close,null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(!selection.identity.equals(SpicyQuotes.identity(a,account))||!account.equals(SubscriptionStore.accountKey(a))){dialog.dismiss();return;}
            boolean copied=copyPrompt((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE),t.title,prompt.getText().toString());Toast.makeText(a,copied?R.string.coach_copied:R.string.coach_copy_failed,copied?Toast.LENGTH_SHORT:Toast.LENGTH_LONG).show();
        }));OPEN.put(a,dialog);OWNER.put(a,selection.identity);dialog.setOnDismissListener(x->{if(OPEN.get(a)==dialog){OPEN.remove(a);OWNER.remove(a);}});dialog.show();
    }
    static boolean copyPrompt(ClipboardManager clipboard,String title,String visiblePrompt){try{if(clipboard==null)return false;clipboard.setPrimaryClip(ClipData.newPlainText(title,visiblePrompt));return true;}catch(RuntimeException error){return false;}}
    static void settings(AppCompatActivity a,LinearLayout parent,boolean dark){
        parent.addView(LedgerUi.action(a,a.getString(R.string.live_ai_title),false,dark,()->{
            String[] values={"calm","playful","spicy","off"};new AlertDialog.Builder(a).setTitle(R.string.live_ai_title).setSingleChoiceItems(a.getResources().getStringArray(R.array.coach_tones),Arrays.asList(values).indexOf(FunStore.tone(a)),(d,index)->{d.dismiss();WORKER.execute(()->{FunStore.tone(a,values[index]);a.runOnUiThread(a::recreate);});}).show();
        }));
        parent.addView(LedgerUi.action(a,a.getString(R.string.coach_interest),false,dark,()->{String[] values={"","explorer","creator","developer","analyst","practitioner"};new AlertDialog.Builder(a).setTitle(R.string.coach_interest).setSingleChoiceItems(a.getResources().getStringArray(R.array.coach_interests),Arrays.asList(values).indexOf(SpicyQuotes.interest(a)),(d,index)->{a.getSharedPreferences("codex_fun_settings",0).edit().putString("interest",values[index]).apply();d.dismiss();a.recreate();}).show();}));
        parent.addView(LedgerUi.action(a,a.getString(R.string.coach_restore_hidden),false,dark,()->SpicyQuotes.restoreHidden(a,SubscriptionStore.accountKey(a),a::recreate)));
        parent.addView(LedgerUi.caption(a,a.getString(R.string.coach_local_help),dark));
    }
}
