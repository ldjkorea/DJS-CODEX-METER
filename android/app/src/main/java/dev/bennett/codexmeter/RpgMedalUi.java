package dev.bennett.codexmeter;

import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.*;

/** On-demand details for one detached achievement. No persistence or evaluation. */
final class RpgMedalUi {
    static int rarity(RpgCatalog.Entry e){switch(e.rarity){case "rare":return R.string.rpg3_rarity_rare;case "epic":return R.string.rpg3_rarity_epic;case "legendary":return R.string.rpg3_rarity_legendary;case "mythic":return R.string.rpg3_rarity_mythic;default:return R.string.rpg3_rarity_common;}}
    static ImageView icon(Context c,RpgCatalog.Entry e,boolean dark,int size){ImageView image=new ImageView(c);image.setImageDrawable(new RpgAchievementMark(e,dark));image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);image.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c,size),Ui.dp(c,size)));return image;}
    static String status(Context c,JSONObject s,RpgCatalog.Entry e){if(RpgCollectionFacts.earned(s,e))return c.getString(R.string.medal_owned);double ratio=RpgCollectionFacts.ratio(s,e);return ratio<0?c.getString(R.string.medal_unknown):c.getString(R.string.medal_percent,(int)(100*ratio));}
    static LinearLayout body(Context c,JSONObject s,RpgCatalog.Entry e,boolean dark){
        LinearLayout body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(c,24),Ui.dp(c,16),Ui.dp(c,24),Ui.dp(c,24));
        LinearLayout hero=Ui.card(c,dark);hero.setGravity(Gravity.CENTER);hero.addView(icon(c,e,dark,100));Ui.addSpacer(hero,16);TextView name=LedgerUi.heading(c,RpgGrowthUi.name(c,e),dark);name.setGravity(Gravity.CENTER);hero.addView(name);Ui.addSpacer(hero,12);hero.addView(LedgerUi.caption(c,c.getString(rarity(e))+" · "+status(c,s,e),dark));body.addView(hero);
        RpgTitleCatalog.Title independent=RpgTitleCatalog.find(e.id);if(independent!=null){body.addView(LedgerUi.caption(c,c.getString(R.string.reborn_medal_dependency),dark));Ui.addSpacer(body,12);body.addView(LedgerUi.caption(c,independent.description(RpgGrowthUi.korean(c)),dark));for(String id:independent.medals){RpgCatalog.Entry required=RpgGrowthFacts.entry(id);body.addView(LedgerUi.caption(c,RpgGrowthUi.name(c,required)+" · "+c.getString(RpgCollectionFacts.earned(s,required)?R.string.medal_done:R.string.medal_notyet),dark));}if(independent.days>0)condition(c,body,c.getString(R.string.medal_days),s!=null&&s.optInt("activity_days")>0,s==null?0:s.optInt("activity_days"),independent.days,dark);if(independent.weeks>0)condition(c,body,c.getString(R.string.medal_weeks),s!=null&&s.optInt("valid_weeks")>0,s==null?0:s.optInt("valid_weeks"),independent.weeks,dark);long at=RpgCollectionFacts.time(s,e);if(at>0)body.addView(LedgerUi.caption(c,c.getString(R.string.rpg3_earned,V3Display.time(at)),dark));return body;}
        Ui.addSpacer(body,16);body.addView(LedgerUi.heading(c,RpgDisplayCopy.flavor(e,RpgGrowthUi.korean(c)),dark));Ui.addSpacer(body,12);body.addView(LedgerUi.caption(c,RpgDisplayCopy.condition(e,RpgGrowthUi.korean(c)),dark));
        if(AchievementJourney.journey(s)!=null&&AchievementJourney.journey(s).optInt("collection_revision")>=CollectionRestart.REVISION){int tier=CollectionTierGoals.required(e);body.addView(LedgerUi.caption(c,c.getString(R.string.collection_tier_goal,TierPresentation.ENGLISH[tier]),dark));}
        JSONObject f=RpgCollectionFacts.fact(s,e);boolean known=RpgCollectionFacts.known(s,e);
        condition(c,body,c.getString(R.string.medal_primary),known,f==null?0:f.optLong("progress"),e.target,dark);
        if(e.days>0)condition(c,body,c.getString(R.string.medal_days),f!=null&&f.has("progress_days")&&(s.optInt("activity_days")>0||f.optInt("progress_days")>0),f==null?0:f.optInt("progress_days"),e.days,dark);
        if(e.weeks>0)condition(c,body,c.getString(R.string.medal_weeks),f!=null&&f.has("progress_weeks")&&(s.optInt("activity_days")>0||s.optInt("valid_weeks")>0),f==null?0:f.optInt("progress_weeks"),e.weeks,dark);
        ProgressBar progress=Ui.progress(c,dark);progress.setMax(1000);progress.setProgress((int)(1000*Math.max(0,RpgCollectionFacts.ratio(s,e))));progress.setContentDescription(status(c,s,e));body.addView(progress);
        boolean earned=RpgCollectionFacts.earned(s,e);long at=RpgCollectionFacts.time(s,e);Ui.addSpacer(body,12);
        body.addView(LedgerUi.caption(c,earned?(at>0?c.getString(R.string.rpg3_earned,V3Display.time(at)):c.getString(R.string.medal_time_unknown)):c.getString(R.string.medal_locked),dark));
        JSONObject journey=AchievementJourney.journey(s),old=journey==null?null:journey.optJSONObject("legacy");JSONObject previous=old==null?null:old.optJSONObject("achievements");boolean paid=previous!=null&&previous.optJSONObject(e.id)!=null&&previous.optJSONObject(e.id).optBoolean("unlocked");
        body.addView(LedgerUi.caption(c,paid?c.getString(R.string.reborn_reward_already):c.getString(R.string.medal_reward,e.bonus),dark));
        LinearLayout more=new LinearLayout(c);more.setOrientation(LinearLayout.VERTICAL);more.setVisibility(View.GONE);more.addView(LedgerUi.caption(c,c.getString(R.string.medal_linked_title,RpgGrowthUi.title(c,e.id)),dark));more.addView(LedgerUi.caption(c,c.getString(R.string.medal_confirmed),dark));body.addView(LedgerUi.action(c,c.getString(R.string.reborn_more),false,dark,()->more.setVisibility(more.getVisibility()==View.GONE?View.VISIBLE:View.GONE)));body.addView(more);return body;
    }
    static void condition(Context c,LinearLayout body,String label,boolean known,long value,long target,boolean dark){Ui.addSpacer(body,10);body.addView(LedgerUi.caption(c,c.getString(R.string.medal_condition,label,known?RpgGrowthUi.number(value):"—",RpgGrowthUi.number(target),c.getString(!known?R.string.medal_unknown:value>=target?R.string.medal_done:R.string.medal_notyet)),dark));}
    static AlertDialog show(AppCompatActivity a,JSONObject s,RpgCatalog.Entry e,boolean dark,boolean title,Runnable equip){
        ScrollView scroll=new ScrollView(a);LinearLayout body=body(a,s,e,dark);
        if(title){TextView preview=LedgerUi.amount(a,RpgGrowthUi.title(a,e.id),dark,22);RpgDisplayStyle.title(preview,dark,22);body.addView(preview,0);}
        scroll.addView(body);AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(title?R.string.medal_preview:R.string.medal_details).setView(scroll).setNegativeButton(R.string.growth_close,null);
        if(equip!=null&&RpgCollectionFacts.earned(s,e))builder.setPositiveButton(R.string.medal_equip,(d,w)->equip.run());AlertDialog d=builder.create();d.show();
        scroll.post(()->{if(d.isShowing()&&scroll.getHeight()>a.getResources().getDisplayMetrics().heightPixels*.65){scroll.getLayoutParams().height=(int)(a.getResources().getDisplayMetrics().heightPixels*.65);scroll.requestLayout();}});return d;
    }
}
