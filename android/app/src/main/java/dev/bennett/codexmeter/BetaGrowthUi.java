package dev.bennett.codexmeter;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Beta-only presentation. Reads detached counters; never changes evidence or awards. */
final class BetaGrowthUi {
    static boolean render(GrowthActivity a,LinearLayout parent,JSONObject journal,boolean dark) {
        JSONObject state=journal==null?null:journal.optJSONObject("rpg"),m=MxpEconomy.state(state);
        if(m==null)return false;
        String account=SubscriptionStore.accountKey(a);
        JSONObject view;
        try{view=MxpEconomy.summary(m,System.currentTimeMillis());}catch(JSONException e){view=new JSONObject();}
        LinearLayout hero=Ui.card(a,dark);hero.setTag("beta:hero");
        TextView heading=LedgerUi.caption(a,"나의 성장 · RPG 3.0",dark);hero.addView(heading);
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ring=new ImageView(a);ring.setImageDrawable(new LevelRing(m.optInt("level",1),MxpEconomy.progress(m)/Math.max(1,m.optLong("next_cost")),dark));ring.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(ring,new LinearLayout.LayoutParams(Ui.dp(a,108),Ui.dp(a,108)));
        LinearLayout copy=new LinearLayout(a);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(Ui.dp(a,14),0,0,0);
        copy.addView(LedgerUi.amount(a,"Lv. "+m.optInt("level",1),dark,30));
        copy.addView(LedgerUi.caption(a,"현재 레벨 진행 · "+LedgerUi.number(MxpEconomy.progress(m))+" / "+m.optLong("next_cost")+" MXP",dark));
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));hero.addView(row);
        String equipped=state.optString("equipped_title");
        Button title=LedgerUi.action(a,equipped.isEmpty()?"칭호를 골라보세요":"장착 중 · "+RpgGrowthUi.title(a,equipped),false,dark,()->catalog(a,state,dark,true,0,account));title.setTag("beta:wardrobe");hero.addView(title);
        parent.addView(hero);Ui.addSpacer(parent,12);

        LinearLayout challenges=Ui.card(a,dark);challenges.setTag("beta:challenges");challenges.addView(LedgerUi.heading(a,"이번에는 여기까지",dark));
        period(a,challenges,"오늘","확인된 활동",view.optInt("day"),1,0xff62b6ff,dark);
        period(a,challenges,"이번 주","3일 · 5일 달성",view.optInt("week"),5,0xff8d9bff,dark);
        period(a,challenges,"이번 달","7일 · 10일 · 14일 달성",view.optInt("month"),14,0xffefa956,dark);
        challenges.addView(LedgerUi.caption(a,"확인된 활동만 반영 · "+view.optString("as_of_day","기록 없음"),dark));
        challenges.addView(LedgerUi.action(a,"성장 규칙 · 기간별 획득 기록",false,dark,()->a.detail("level",state)));parent.addView(challenges);Ui.addSpacer(parent,12);

        JSONObject honors=m.optJSONObject("honors"),titles=m.optJSONObject("titles");
        LinearLayout collection=Ui.card(a,dark);collection.addView(LedgerUi.heading(a,"나만의 명예 진열장",dark));
        collection.addView(LedgerUi.caption(a,"명예 "+size(honors)+" / 11   ·   칭호 "+size(titles)+" / 5",dark));Ui.addSpacer(collection,10);
        ArrayList<String[]> earned=new ArrayList<>();for(String[] item:MxpHonors.ALL)if(!MxpHonors.isTitle(item[0])&&honors!=null&&honors.has(item[0]))earned.add(item);
        earned.sort((x,y)->Long.compare(honors.optLong(y[0]),honors.optLong(x[0])));
        LinearLayout recent=new LinearLayout(a);recent.setOrientation(LedgerUi.stacked(a)?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        if(earned.isEmpty())recent.addView(LedgerUi.caption(a,"첫 활동을 확인하면 첫 메달이 여기에 걸립니다.",dark));
        for(int i=0;i<Math.min(3,earned.size());i++){String[] item=earned.get(i);recent.addView(tile(a,state,item,true,dark,account),new LinearLayout.LayoutParams(LedgerUi.stacked(a)?-1:0,-2,LedgerUi.stacked(a)?0:1));}
        collection.addView(recent);Ui.addSpacer(collection,8);
        Button medals=LedgerUi.action(a,"명예 도감 · "+size(honors)+"개 획득",false,dark,()->catalog(a,state,dark,false,0,account));medals.setTag("beta:honors");
        collection.addView(LedgerUi.pair(a,medals,LedgerUi.action(a,"칭호 옷장 · "+size(titles)+"개 보유",false,dark,()->catalog(a,state,dark,true,0,account))));parent.addView(collection);Ui.addSpacer(parent,12);

        LinearLayout kr=RpgGrowthUi.tier(a,state.optJSONObject("KR"),"KR",dark),world=RpgGrowthUi.tier(a,state.optJSONObject("WORLD"),"WORLD",dark);
        kr.setOnClickListener(v->a.detail("KR",state));world.setOnClickListener(v->a.detail("WORLD",state));
        kr.setContentDescription("한국 티어 · 자세히 보기");world.setContentDescription("세계 티어 · 자세히 보기");kr.setFocusable(true);world.setFocusable(true);
        parent.addView(RpgGrowthUi.pair(a,kr,world));Ui.addSpacer(parent,12);
        parent.addView(LedgerUi.action(a,"기존 업적 · 대표 메달 관리",false,dark,()->a.open("achievements")));
        parent.addView(LedgerUi.action(a,"기존 칭호 모음",false,dark,()->a.open("titles")));
        a.collections(journal);
        parent.addView(LedgerUi.action(a,"확인된 토큰 기록",false,dark,()->a.detail("tokens",state)));
        parent.addView(LedgerUi.action(a,a.getString(R.string.profile_share),false,dark,()->Ui.startSecondaryActivity(a,ProfileShareActivity.class)));
        return true;
    }
    static int size(JSONObject j){return j==null?0:j.length();}
    static void period(android.content.Context a,LinearLayout p,String name,String note,int count,int target,int color,boolean dark){
        Ui.addSpacer(p,12);LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=LedgerUi.heading(a,name,dark);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(LedgerUi.badge(a,count+" / "+target+"일",dark,false));p.addView(row);Ui.addSpacer(p,6);
        ProgressBar bar=Ui.progress(a,dark);bar.setMax(target);bar.setProgress(Math.min(count,target));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));bar.setContentDescription(name+" "+count+"일, 목표 "+target+"일");p.addView(bar);
        p.addView(LedgerUi.caption(a,note+(count>=target?" · 달성":" · 진행 중"),dark));
    }
    static boolean current(GrowthActivity a,String account){return !a.isDestroyed()&&!a.isFinishing()&&Objects.equals(account,SubscriptionStore.accountKey(a));}
    static void catalog(GrowthActivity a,JSONObject state,boolean dark,boolean title,int filter,String account){
        if(!current(a,account))return;
        JSONObject m=MxpEconomy.state(state),facts=m.optJSONObject(title?"titles":"honors");
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Ui.dp(a,16),Ui.dp(a,12),Ui.dp(a,16),Ui.dp(a,16));
        body.addView(LedgerUi.caption(a,title?"보유한 칭호를 눌러 장착하세요. 잠긴 칭호는 획득 조건을 볼 수 있습니다.":"활동의 흔적이 영구 메달로 남습니다. 메달을 누르면 조건과 획득일을 볼 수 있습니다.",dark));
        body.addView(LedgerUi.tabs(a,new String[]{"전체","획득","도전 중"},filter,dark,i->catalog(a,state,dark,title,i,account)));Ui.addSpacer(body,12);
        int shown=0;LinearLayout row=null;boolean stack=LedgerUi.stacked(a);
        for(String[] item:MxpHonors.ALL){if(MxpHonors.isTitle(item[0])!=title)continue;boolean owned=facts!=null&&facts.has(item[0]);if(filter==1&&!owned||filter==2&&owned)continue;
            if(stack||shown%2==0){row=new LinearLayout(a);row.setOrientation(stack?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);body.addView(row);Ui.addSpacer(body,10);}
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(stack?-1:0,-2,stack?0:1);if(!stack&&shown%2==1)lp.setMarginStart(Ui.dp(a,8));row.addView(tile(a,state,item,owned,dark,account),lp);shown++;
        }
        if(shown==0)body.addView(LedgerUi.caption(a,"아직 이곳에 놓인 메달이 없습니다.",dark));
        if(!stack&&shown%2==1&&row!=null)row.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));
        ScrollView scroll=new ScrollView(a);scroll.addView(body);if(a.detailDialog!=null)a.detailDialog.dismiss();
        a.detailDialog=new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(title?"칭호 옷장":"영구 명예 도감").setView(scroll).setPositiveButton("닫기",null).create();a.detailDialog.show();
    }
    static LinearLayout tile(GrowthActivity a,JSONObject state,String[] item,boolean owned,boolean dark,String account){
        LinearLayout card=Ui.card(a,dark);card.setPadding(Ui.dp(a,10),Ui.dp(a,12),Ui.dp(a,10),Ui.dp(a,12));card.setGravity(Gravity.CENTER_HORIZONTAL);card.setMinimumHeight(Ui.dp(a,148));
        ImageView medal=new ImageView(a);medal.setImageDrawable(new Medal(item,owned,dark));medal.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(medal,new LinearLayout.LayoutParams(Ui.dp(a,64),Ui.dp(a,64)));
        TextView name=LedgerUi.heading(a,item[RpgGrowthUi.korean(a)?1:2],dark);name.setTextSize(14);name.setGravity(Gravity.CENTER);card.addView(name,new LinearLayout.LayoutParams(-1,-2));
        String status=owned?(state.optString("equipped_title").equals(item[0])?"장착 중":"획득"):"잠김";
        TextView sub=LedgerUi.caption(a,a.getString(RpgMedalUi.rarity(MxpHonors.title(item[0])))+" · "+status,dark);sub.setGravity(Gravity.CENTER);card.addView(sub);
        card.setFocusable(true);card.setClickable(true);card.setTag("beta:medal:"+item[0]);card.setContentDescription(name.getText()+" · "+sub.getText()+" · 조건 보기");
        card.setOnClickListener(v->detail(a,state,item,owned,dark,account));return card;
    }
    static void detail(GrowthActivity a,JSONObject state,String[] item,boolean owned,boolean dark,String account){
        if(!current(a,account))return;
        int index=0;for(;index<MxpHonors.ALL.length;index++)if(MxpHonors.ALL[index][0].equals(item[0]))break;
        JSONObject m=MxpEconomy.state(state),facts=m.optJSONObject(MxpHonors.isTitle(item[0])?"titles":"honors");
        String condition=a.getResources().getStringArray(R.array.mxp_conditions)[index];
        String date=owned?"\n획득 · "+V3Display.time(facts.optLong(item[0])):"\n아직 잠겨 있습니다. 활동이 확인되면 자동으로 반영됩니다.";
        if(a.detailDialog!=null)a.detailDialog.dismiss();
        androidx.appcompat.app.AlertDialog.Builder builder=new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(item[RpgGrowthUi.korean(a)?1:2]).setMessage(condition+date).setNegativeButton("닫기",null);
        if(owned&&MxpHonors.isTitle(item[0])){if(state.optString("equipped_title").equals(item[0]))builder.setPositiveButton("장착 중",null);else builder.setPositiveButton("이 칭호 장착",(d,w)->{if(current(a,account))a.equip(item[0]);});}
        a.detailDialog=builder.create();a.detailDialog.show();
    }
    static final class LevelRing extends Drawable {
        final Paint p=new Paint(3);final int level;final double progress;final boolean dark;
        LevelRing(int l,double v,boolean d){level=l;progress=Math.max(0,Math.min(1,v));dark=d;}
        public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/108f,b.height()/108f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(dark?0xff303640:0xffdfe5ed);c.drawCircle(54,54,43,p);p.setColor(dark?0xff71b7ff:0xff265b9c);p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(11,11,97,97,-90,(float)(progress*360),false,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(27);p.setColor(dark?Color.WHITE:0xff17283e);c.drawText(Integer.toString(level),54,62,p);p.setTextSize(10);c.drawText("LEVEL",54,77,p);c.restore();}
        public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static final class Medal extends Drawable {
        final Paint p=new Paint(3);final String[] item;final boolean owned,dark;
        Medal(String[] i,boolean o,boolean d){item=i;owned=o;dark=d;}
        public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/64f,b.height()/64f);
            int color=!owned?(dark?0xff7a8596:0xff687689):item[3].equals("common")?0xff71b7ff:item[3].equals("rare")?0xff79ccb7:item[3].equals("epic")?0xffb5a4ff:item[3].equals("legendary")?0xffedb95d:0xffed8bba;
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,64,64,dark?0xff273143:0xffe7edf5,dark?0xff141c29:0xfffafcfe,Shader.TileMode.CLAMP));c.drawCircle(32,30,26,p);p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(32,30,25,p);c.drawCircle(32,30,21,p);
            p.setStrokeCap(Paint.Cap.ROUND);
            if(!owned){c.drawRoundRect(22,27,42,43,3,3,p);c.drawArc(25,17,39,35,180,180,false,p);c.drawCircle(32,34,1,p);}
            else if(MxpHonors.isTitle(item[0])){Path crown=new Path();crown.moveTo(18,24);crown.lineTo(23,40);crown.lineTo(41,40);crown.lineTo(46,24);crown.lineTo(37,30);crown.lineTo(32,18);crown.lineTo(27,30);crown.close();c.drawPath(crown,p);c.drawLine(23,44,41,44,p);}
            else if(item[0].contains("gold")||item[0].contains("annual")||item[0].contains("frame_50")){c.drawCircle(32,30,9,p);c.save();c.rotate(-30,32,30);c.drawOval(14,24,50,36,p);c.restore();c.drawCircle(46,19,3,p);}
            else if(item[0].contains("frame")){Path gem=new Path();gem.moveTo(32,15);gem.lineTo(44,29);gem.lineTo(32,45);gem.lineTo(20,29);gem.close();c.drawPath(gem,p);c.drawLine(20,29,44,29,p);}
            else {c.drawRoundRect(20,19,44,42,3,3,p);c.drawLine(20,25,44,25,p);c.drawLine(26,15,26,22,p);c.drawLine(38,15,38,22,p);c.drawLine(25,33,30,38,p);c.drawLine(30,38,39,29,p);}
            c.restore();}
        public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
