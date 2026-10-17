package dev.bennett.codexmeter;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.time.*;
import org.json.*;

/** Records root: medal calendar first; original usage analytics remain an explicit detail destination. */
public final class RecordsActivity extends AppCompatActivity {
    LinearLayout body;boolean dark,registered;String account;long generation;JSONObject state;
    YearMonth month=YearMonth.from(BetaV4.day(System.currentTimeMillis()));
    final BroadcastReceiver updated=new BroadcastReceiver(){public void onReceive(Context c,Intent i){load();}};
    @Override protected void onCreate(Bundle saved){
        Ui.applySelectedTheme(this);super.onCreate(saved);dark=Ui.isDark(this);
        if(getIntent().getBooleanExtra("manual_record",false)||getIntent().hasExtra("selected_date")||getIntent().getIntExtra("screen",0)>0){details(getIntent().getBooleanExtra("manual_record",false));finish();return;}
        if(saved!=null)try{month=YearMonth.parse(saved.getString("medal_month",month.toString()));}catch(Exception ignored){}
        body=Ui.installPage(this,getString(R.string.matte_records),false).content;MatteNav.install(this,2);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=findViewById(R.id.dashboard_refresh);refresh.setEnabled(false);
    }
    @Override protected void onResume(){super.onResume();if(body==null)return;
        if(!registered){androidx.core.content.ContextCompat.registerReceiver(this,updated,new IntentFilter(AppConstants.ACTION_USAGE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);registered=true;}load();}
    @Override protected void onPause(){if(registered){unregisterReceiver(updated);registered=false;}super.onPause();}
    @Override protected void onDestroy(){generation++;super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("medal_month",month.toString());}
    void details(boolean manual){startActivity(new Intent(this,LedgerAnalyticsActivity.class).putExtras(getIntent()).putExtra("medal_usage_details",true).putExtra("manual_record",manual));}
    void load(){String expected=SubscriptionStore.accountKey(this);long ticket=++generation;account=expected;state=null;body.removeAllViews();body.addView(BetaUi.note(this,getString(R.string.next_loading),dark));
        BetaMedalStore.WORKER.execute(()->{JSONObject loaded=null;boolean error=false;try{loaded=BetaMedalStore.load(this,expected);}catch(Exception unavailable){error=true;}JSONObject value=loaded;boolean failed=error;
            runOnUiThread(()->{if(isDestroyed()||isFinishing()||ticket!=generation||!java.util.Objects.equals(expected,SubscriptionStore.accountKey(this)))return;state=value;render(failed);});});
    }
    void render(boolean failed){String expected=account;body.removeAllViews();body.setTag("beta9911:records");JSONObject days=state==null?new JSONObject():state.optJSONObject("days");
        if(TrialDemo.active(this))body.addView(BetaUi.note(this,BetaCopy.t(this,"demo"),dark));
        LinearLayout calendar=BetaUi.group(body,BetaMedalUi.t(this,"나의 메달 달력","My medal calendar"),dark);calendar.setTag("beta9911:calendar");
        LinearLayout navigation=new LinearLayout(this);navigation.setGravity(Gravity.CENTER_VERTICAL);
        Button before=BetaUi.action(this,"‹",false,dark,()->{month=month.minusMonths(1);render(false);});before.setContentDescription(BetaMedalUi.t(this,"이전 달","Previous month"));navigation.addView(before,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
        TextView label=BetaUi.body(this,month.format(java.time.format.DateTimeFormatter.ofPattern(BetaCopy.ko(this)?"yyyy년 M월":"MMMM yyyy",getResources().getConfiguration().getLocales().get(0))),dark);label.setGravity(Gravity.CENTER);navigation.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button after=BetaUi.action(this,"›",false,dark,()->{month=month.plusMonths(1);render(false);});after.setContentDescription(BetaMedalUi.t(this,"다음 달","Next month"));navigation.addView(after,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));calendar.addView(navigation);
        int count=BetaMedalModel.count(days,month.atDay(1),month.atEndOfMonth());calendar.addView(BetaUi.note(this,BetaMedalUi.t(this,"메달 기록 %d일","Medals recorded on %d days",count),dark));
        boolean list=getResources().getConfiguration().fontScale>1.3f||getResources().getConfiguration().screenWidthDp<360;
        if(list){calendar.setTag("beta9911:calendar-list");for(int i=1;i<=month.lengthOfMonth();i++){
            String date=month.atDay(i).toString();JSONObject row=days.optJSONObject(date);boolean future=month.atDay(i).isAfter(BetaV4.day(System.currentTimeMillis()));View entry=BetaUi.row(this,date,future?BetaMedalUi.t(this,"예정","Upcoming"):BetaMedalUi.status(this,row),dark,()->BetaMedalUi.day(this,date,row,dark,expected));entry.setEnabled(!future);calendar.addView(entry);
        }}else{
            GridLayout grid=new GridLayout(this);grid.setColumnCount(7);calendar.addView(grid,new LinearLayout.LayoutParams(-1,-2));
            int offset=month.atDay(1).getDayOfWeek().getValue()-1;
            String[] weekday=BetaCopy.ko(this)?new String[]{"월","화","수","목","금","토","일"}:new String[]{"M","T","W","T","F","S","S"};
            for(String title:weekday){TextView day=BetaUi.note(this,title,dark);day.setGravity(Gravity.CENTER);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);grid.addView(day,lp);}
            for(int cell=0;cell<offset+month.lengthOfMonth();cell++){
                LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setMinimumHeight(Ui.dp(this,64));
                GridLayout.LayoutParams lp=new GridLayout.LayoutParams(GridLayout.spec(1+cell/7),GridLayout.spec(cell%7,1f));lp.width=0;lp.height=-2;grid.addView(tile,lp);
                int number=cell-offset+1;if(number<1)continue;String date=month.atDay(number).toString();JSONObject row=days.optJSONObject(date);
                TextView day=BetaUi.body(this,Integer.toString(number),dark);day.setGravity(Gravity.CENTER);tile.addView(day);
                boolean future=month.atDay(number).isAfter(BetaV4.day(System.currentTimeMillis()));int grade=row==null?future?-4:-1:row.optBoolean("active")?row.optInt("grade"):row.optBoolean("measured")?-2:-3;
                ImageView icon=new ImageView(this);icon.setImageDrawable(new BetaMedalUi.Mark(this,grade,row!=null&&row.optBoolean("active"),dark));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);tile.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,28),Ui.dp(this,28)));
                tile.setTag("beta9911:day:"+date);tile.setContentDescription(date+" · "+(future?BetaMedalUi.t(this,"예정","Upcoming"):BetaMedalUi.status(this,row)));tile.setFocusable(true);tile.setClickable(!future);tile.setEnabled(!future);tile.setOnClickListener(v->BetaMedalUi.day(this,date,row,dark,expected));
            }
        }
        if(failed)body.addView(BetaUi.note(this,BetaMedalUi.t(this,"기록을 읽지 못했어요. 원본은 보존됩니다.","Could not read records. Original data is retained."),dark));
        body.addView(BetaUi.row(this,BetaMedalUi.t(this,"사용량 상세","Usage details"),"",dark,()->details(false)));
        body.addView(BetaUi.row(this,BetaMedalUi.t(this,"설정","Settings"),"",dark,()->startActivity(new Intent(this,MeterSettingsActivity.class))));
    }
}
