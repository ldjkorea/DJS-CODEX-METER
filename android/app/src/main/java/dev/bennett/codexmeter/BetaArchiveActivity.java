package dev.bennett.codexmeter;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import androidx.appcompat.app.AppCompatActivity;
public final class BetaArchiveActivity extends AppCompatActivity {
    long generation;LinearLayout body;String account;
    @Override protected void onCreate(Bundle saved){Ui.applySelectedTheme(this);super.onCreate(saved);body=Ui.installPage(this,BetaCopy.t(this,"archive"),true).content;androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=findViewById(R.id.dashboard_refresh);refresh.setEnabled(false);}
    @Override protected void onResume(){super.onResume();long ticket=++generation;account=SubscriptionStore.accountKey(this);String key=account;boolean dark=Ui.isDark(this);body.removeAllViews();body.addView(BetaUi.note(this,BetaCopy.t(this,"readonly"),dark));
        BetaGrowthStore.WORKER.execute(()->{JSONObject archive=null;try{archive=BetaGrowthStore.archive(this,key);}catch(Exception ignored){}final JSONObject data=archive;runOnUiThread(()->{if(ticket!=generation||!BetaUi.current(this,key))return;show(data,dark);});});}
    @Override protected void onDestroy(){generation++;super.onDestroy();}
    void show(JSONObject archive,boolean dark){if(archive==null){body.addView(BetaUi.note(this,BetaCopy.t(this,"no_archive"),dark));return;}JSONObject r=archive.optJSONObject("rpg"),m=MxpEconomy.state(r);LinearLayout summary=BetaUi.group(body,BetaCopy.t(this,"archive"),dark);summary.addView(BetaUi.body(this,"Lv. "+(m==null?r.optInt("level",1):m.optInt("level",1)),dark));summary.addView(BetaUi.note(this,BetaCopy.t(this,"total",LedgerUi.number(m==null?r.optLong("xp"):m.optLong("total"))),dark));summary.addView(BetaUi.note(this,BetaGrowthUi.time(this,archive.optLong("archived_at")),dark));
        java.util.LinkedHashSet<String> ids=new java.util.LinkedHashSet<>(RpgGrowthFacts.earned(r));if(m!=null&&m.optJSONObject("honors")!=null)m.optJSONObject("honors").keys().forEachRemaining(ids::add);
        LinearLayout achievements=BetaUi.group(body,BetaCopy.t(this,"honors"),dark);for(String id:ids){String[] modern=MxpHonors.find(id);RpgCatalog.Entry old=RpgGrowthFacts.entry(id);achievements.addView(BetaUi.body(this,modern!=null?modern[BetaCopy.ko(this)?1:2]:old==null?id:RpgGrowthUi.name(this,old),dark));Ui.addSpacer(achievements,8);}
        LinearLayout titles=BetaUi.group(body,BetaCopy.t(this,"titles"),dark);java.util.LinkedHashSet<String> oldTitles=new java.util.LinkedHashSet<>(RpgTitleCatalog.owned(r));if(m!=null&&m.optJSONObject("titles")!=null)m.optJSONObject("titles").keys().forEachRemaining(oldTitles::add);for(String id:oldTitles){titles.addView(BetaUi.body(this,RpgGrowthUi.title(this,id)+(id.equals(r.optString("equipped_title"))?" · "+BetaCopy.t(this,"equipped"):""),dark));Ui.addSpacer(titles,8);}
        if(ids.isEmpty())achievements.addView(BetaUi.note(this,BetaCopy.t(this,"no_items"),dark));if(oldTitles.isEmpty())titles.addView(BetaUi.note(this,BetaCopy.t(this,"no_items"),dark));
    }
}
