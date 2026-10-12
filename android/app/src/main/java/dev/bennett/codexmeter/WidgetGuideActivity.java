package dev.bennett.codexmeter;

import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.*;

/** Recommendation only: no provider is hidden, removed, or rebound. */
public final class WidgetGuideActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){
        Ui.applySelectedTheme(this);super.onCreate(state);boolean dark=Ui.isDark(this);
        LinearLayout content=Ui.installPage(this,getString(R.string.ws_guide),true).content;
        ((androidx.swiperefreshlayout.widget.SwipeRefreshLayout)findViewById(R.id.dashboard_refresh)).setEnabled(false);
        LinearLayout usage=card(content,R.string.ws_usage,R.string.ws_usage_note,dark);
        RemoteViews sample=new RemoteViews(getPackageName(),R.layout.widget_rings);
        sample.setInt(android.R.id.background,"setBackgroundResource",R.drawable.widget_bg_dark_88);
        sample.setTextViewText(R.id.primary_samsung_value,"76.25%");sample.setTextViewText(R.id.secondary_samsung_value,"58.125%");
        sample.setProgressBar(R.id.primary_samsung_progress,100,76,false);sample.setProgressBar(R.id.secondary_samsung_progress,100,58,false);
        sample.setTextColor(R.id.primary_samsung_value,0xfff5f5f5);sample.setTextColor(R.id.secondary_samsung_value,0xfff5f5f5);
        sample.setInt(R.id.primary_samsung_icon,"setColorFilter",0xffd5d8de);sample.setInt(R.id.secondary_samsung_icon,"setColorFilter",0xffd5d8de);
        preview(usage,sample,200,85);usage.addView(LedgerUi.caption(this,getString(R.string.ws_sample),dark));
        usage.addView(LedgerUi.action(this,getString(R.string.ws_add_usage),true,dark,()->pin(CodexUsageWidget.class)));
        installed(usage,CodexUsageWidget.class,WidgetConfigActivity.class,dark);
        LinearLayout tasks=card(content,R.string.ws_tasks,R.string.ws_tasks_note,dark);
        try{long now=System.currentTimeMillis();JSONArray rows=new JSONArray();String[] names={"GPT HUD 개발","기록 화면 검증","위젯 디자인","업데이트 확인"};
            for(int i=0;i<names.length;i++)rows.put(new JSONObject().put("name",names[i]).put("state",i<2?"running":"completed").put("at",now));
            int width=Math.min(320,getResources().getConfiguration().screenWidthDp-64);
            preview(tasks,TaskStatusWidget.render(this,width,180,4,new JSONObject().put("verified",true).put("checked",now).put("rows",rows),now,true),width,180);
        }catch(Exception ignored){}tasks.addView(LedgerUi.caption(this,getString(R.string.ws_sample),dark));
        tasks.addView(LedgerUi.action(this,getString(R.string.combined_widget_add),false,dark,()->pin(CombinedStatusWidget.class)));
        tasks.addView(LedgerUi.action(this,getString(R.string.hud_add_task_widget),true,dark,()->pin(TaskStatusWidget.class)));
        tasks.addView(LedgerUi.action(this,getString(R.string.task_trial_title),false,dark,()->startActivity(new Intent(this,TaskStatusActivity.class))));
        LinearLayout advanced=card(content,R.string.ws_advanced,R.string.ws_advanced_note,dark);
        LinearLayout options=new LinearLayout(this);options.setOrientation(LinearLayout.VERTICAL);options.setVisibility(View.GONE);
        advanced.addView(LedgerUi.action(this,getString(R.string.ws_advanced_show),false,dark,()->options.setVisibility(options.getVisibility()==View.GONE?View.VISIBLE:View.GONE)));
        advanced.addView(options);options.addView(LedgerUi.caption(this,getString(R.string.ws_lock_note),dark));
        options.addView(LedgerUi.action(this,getString(R.string.evo_widget),false,dark,()->pin(EvolutionWidget.class)));
        installed(options,EvolutionWidget.class,EvolutionWidgetConfigActivity.class,dark);
        options.addView(LedgerUi.action(this,getString(R.string.matte_widgets),false,dark,()->MatteNav.homeAction(this,"widgets")));
    }
    private LinearLayout card(LinearLayout parent,int title,int note,boolean dark){
        LinearLayout card=Ui.card(this,dark);card.addView(LedgerUi.heading(this,getString(title),dark));Ui.addSpacer(card,8);
        card.addView(LedgerUi.caption(this,getString(note),dark));Ui.addSpacer(card,12);parent.addView(card);Ui.addSpacer(parent,16);return card;
    }
    private void preview(LinearLayout parent,RemoteViews remote,int width,int height){
        FrameLayout frame=new FrameLayout(this);frame.setContentDescription(getString(R.string.ws_sample));
        // Use the production RemoteViews layouts, scaled down to the available page width.
        int dp=Math.min(width,getResources().getConfiguration().screenWidthDp-64);
        frame.addView(remote.apply(getApplicationContext(),frame),new FrameLayout.LayoutParams(-1,-1));
        parent.addView(frame,new LinearLayout.LayoutParams(Ui.dp(this,dp),Ui.dp(this,height)));
    }
    private void installed(LinearLayout parent,Class<?> provider,Class<?> editor,boolean dark){
        int[] ids=AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this,provider));
        for(int i=0;i<ids.length;i++){int id=ids[i];parent.addView(LedgerUi.action(this,getString(R.string.ws_edit,i+1),false,dark,
            ()->startActivity(new Intent(this,editor).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id))));}
    }
    private void pin(Class<?> provider){AppWidgetManager manager=AppWidgetManager.getInstance(this);
        if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,provider),null,null);
        else Toast.makeText(this,R.string.hud_add_task_widget_help,Toast.LENGTH_LONG).show();}
    @Override public boolean onSupportNavigateUp(){finish();return true;}
}
