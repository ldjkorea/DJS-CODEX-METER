package dev.bennett.codexmeter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.widget.*;
import org.json.JSONObject;

final class BetaNextGoalUi {
    static void render(GrowthActivity a,LinearLayout parent,JSONObject state,BetaGrowthModel model,boolean dark,String account){
        JSONObject goal=model.summary.optJSONObject("next_goal");String id=goal==null?"":goal.optString("id");
        LinearLayout card=BetaUi.group(parent,BetaCopy.t(a,"next_focus"),dark);card.setTag("beta997:next-goal");
        card.addView(BetaUi.note(a,"Lv. "+model.summary.optInt("level")+" · "+BetaIdentity.name(a),dark));
        if(id.isEmpty()){card.addView(BetaUi.body(a,BetaCopy.t(a,"complete_all"),dark));return;}
        String[] entry=BetaCatalog.find(id);if(entry==null)return;
        ImageView emblem=new ImageView(a);emblem.setImageDrawable(new BetaEmblem(id,false,dark));emblem.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(emblem,new LinearLayout.LayoutParams(Ui.dp(a,56),Ui.dp(a,56)));
        TextView title=BetaUi.title(a,BetaCatalog.name(id,BetaIdentity.name(a),BetaCopy.ko(a)),dark);title.setTag("beta997:goal-name");card.addView(title);
        card.addView(BetaUi.body(a,entry[BetaCopy.ko(a)?3:4],dark));
        int target=Math.max(1,goal.optInt("target")),current=Math.min(target,Math.max(0,goal.optInt("current")));
        ProgressBar bar=Ui.progress(a,dark);bar.setMax(target);bar.setProgress(current);bar.setProgressTintList(ColorStateList.valueOf(BetaUi.accent(a,dark)));bar.setContentDescription(title.getText()+" "+current+" / "+target);card.addView(bar);
        card.addView(BetaUi.note(a,current+" / "+target+" · "+BetaCopy.t(a,"progress"),dark));
        String metric=goal.optString("metric");java.time.LocalDate date=BetaV4.day(System.currentTimeMillis());
        if(metric.equals("weeks")||metric.equals("perfect")||metric.equals("week_now")){
            int stage=metric.equals("perfect")?5:3,count=state.optJSONObject("weeks").optInt(BetaV4.week(date));
            card.addView(BetaUi.note(a,BetaCopy.t(a,"weekly")+" · "+BetaCopy.t(a,"days",Math.min(count,stage),stage),dark));
        }else if(metric.equals("bronze")||metric.equals("silver")||metric.equals("gold")){
            int stage=metric.equals("bronze")?7:metric.equals("silver")?10:14,count=state.optJSONObject("months").optInt(java.time.YearMonth.from(date).toString());
            card.addView(BetaUi.note(a,BetaCopy.t(a,"monthly")+" · "+BetaCopy.t(a,"days",Math.min(count,stage),stage),dark));
        }
        boolean done=goal.optBoolean("today_done");
        String status=!model.collectionEnabled?"paused":model.status==BetaGrowthModel.Status.ERROR?"error":done?"next_done_today":model.key();
        TextView note=BetaUi.note(a,BetaCopy.t(a,status),dark);note.setTag("beta997:goal-status");card.addView(note);
        if(!model.collectionEnabled){card.addView(BetaUi.action(a,BetaCopy.t(a,"goal_settings"),true,dark,()->a.startActivity(new Intent(a,MeterSettingsActivity.class))));}
        else if(!done){Button check=BetaUi.action(a,BetaCopy.t(a,"goal_check"),true,dark,()->MatteNav.open(a,1));check.setTag("beta997:goal-check");card.addView(check);}
        Button details=BetaUi.action(a,BetaCopy.t(a,"goal_details"),done&&model.collectionEnabled,dark,()->BetaGrowthUi.detail(a,state,id,dark,account));details.setTag("beta997:goal-details");card.addView(details);
    }
}
