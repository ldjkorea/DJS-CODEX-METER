package dev.bennett.codexmeter;

import android.content.Context;
import android.view.Gravity;
import android.widget.*;

/** Shared, lightweight showcase and title styling. No stored facts are modified. */
final class RpgDisplayStyle {
    static void title(TextView text,boolean dark,int size){
        Context c=text.getContext();text.setTypeface(PretendardFont.bold(c));text.setTextSize(size);
        text.setLetterSpacing(.035f);text.setTextColor(TierTheme.accent(c,dark));text.setLineSpacing(Ui.dp(c,3),1.08f);
        text.setGravity(Gravity.CENTER);text.setPadding(Ui.dp(c,14),Ui.dp(c,14),Ui.dp(c,14),Ui.dp(c,14));
        android.graphics.drawable.GradientDrawable frame=LedgerUi.shape(c,Ui.cardColor(c,dark),16);
        frame.setStroke(Ui.dp(c,1),TierTheme.accent(c,dark));text.setBackground(frame);
    }
    static LinearLayout medal(Context c,RpgCatalog.Entry e,boolean dark){
        LinearLayout card=Ui.card(c,dark);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(c,8),Ui.dp(c,14),Ui.dp(c,8),Ui.dp(c,14));
        card.addView(RpgMedalUi.icon(c,e,dark,54));Ui.addSpacer(card,12);
        TextView name=LedgerUi.caption(c,RpgGrowthUi.name(c,e),dark);name.setTypeface(PretendardFont.semibold(c));
        name.setTextColor(Ui.mainText(dark));name.setTextSize(13);name.setGravity(Gravity.CENTER);name.setLineSpacing(Ui.dp(c,2),1.06f);
        card.addView(name,new LinearLayout.LayoutParams(-1,-2));card.setMinimumHeight(Ui.dp(c,122));
        card.setFocusable(true);card.setContentDescription(RpgGrowthUi.name(c,e)+" · "+c.getString(R.string.medal_details));return card;
    }
    static void addMedal(LinearLayout row,LinearLayout card){
        Context c=row.getContext();boolean stack=c.getResources().getConfiguration().fontScale>1.25f||c.getResources().getConfiguration().screenWidthDp<300;
        row.setOrientation(stack?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(stack?-1:0,-2,stack?0:1);
        if(row.getChildCount()>0){if(stack)lp.topMargin=Ui.dp(c,10);else lp.setMarginStart(Ui.dp(c,10));}row.addView(card,lp);
    }
    private RpgDisplayStyle(){}
}
