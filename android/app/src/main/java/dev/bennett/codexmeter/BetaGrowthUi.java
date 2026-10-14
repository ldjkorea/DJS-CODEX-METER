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
        if(m==null){
            LinearLayout pending=card(a,dark);pending.setTag("beta:pending");pending.addView(LedgerUi.heading(a,"성장 기록 확인 전",dark));
            pending.addView(LedgerUi.caption(a,"아직 이 계정의 성장 기록을 확인하지 못했습니다. 연결 상태와 수집 설정을 확인하세요. 기존 기록을 0으로 표시하거나 새로 만들지 않습니다.",dark));
            pending.addView(LedgerUi.action(a,"홈에서 연결 상태 확인",true,dark,()->MatteNav.open(a,1)));parent.addView(pending);return true;
        }
        String account=SubscriptionStore.accountKey(a);
        BetaGrowthModel model=a.betaGrowthModel;
        if(model==null)return false;
        JSONObject view=model.summary;
        LinearLayout hero=card(a,dark);hero.setTag("beta:hero");
        TextView status=LedgerUi.badge(a,model.label(),dark,model.status==BetaGrowthModel.Status.ERROR);status.setTag("beta:status");
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);hero.addView(status);Ui.addSpacer(hero,8);
        hero.addView(LedgerUi.amount(a,model.known()?"Lv. "+m.optInt("level",1):"레벨 확인 불가",dark,28));
        if(model.known()){
            double progress=MxpEconomy.progress(m);long cost=m.optLong("next_cost");
            hero.addView(LedgerUi.caption(a,"다음 레벨까지 "+LedgerUi.number(Math.max(0,cost-progress))+" MXP",dark));
            ProgressBar xp=Ui.progress(a,dark);xp.setMax(1000);xp.setProgress((int)(1000*Math.min(1,progress/Math.max(1,cost))));
            xp.setContentDescription("레벨 진행 "+LedgerUi.number(progress)+" / "+cost+" MXP");hero.addView(xp);
            hero.addView(LedgerUi.caption(a,LedgerUi.number(progress)+" / "+cost+" MXP · 성장 경험치",dark));
        }
        hero.addView(LedgerUi.caption(a,model.lastObserved>0?"이 계정의 마지막 사용량 관측 · "+V3Display.time(model.lastObserved):"아직 이 계정의 사용량 관측 기록이 없습니다",dark));
        String equipped=state.optString("equipped_title");
        JSONObject honors=m.optJSONObject("honors"),titles=m.optJSONObject("titles");
        if(!equipped.isEmpty())hero.addView(LedgerUi.caption(a,"장착 칭호 · "+RpgGrowthUi.title(a,equipped),dark));
        parent.addView(hero);Ui.addSpacer(parent,12);

        LinearLayout goal=card(a,dark);goal.setTag("beta:goal");
        String[] next=null;
        for(String[] item:MxpHonors.ALL)if(!MxpHonors.isTitle(item[0])&&(honors==null||!honors.has(item[0]))){next=item;break;}
        if(next!=null){
            final String[] target=next;int index=Arrays.asList(MxpHonors.ALL).indexOf(target);
            LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
            ImageView icon=new ImageView(a);icon.setImageDrawable(new Medal(target,false,dark));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(a,52),Ui.dp(a,52)));
            LinearLayout copy=new LinearLayout(a);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(Ui.dp(a,12),0,0,0);
            copy.addView(LedgerUi.caption(a,"다음 명예 목표",dark));copy.addView(LedgerUi.heading(a,target[1],dark));
            row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));goal.addView(row);Ui.addSpacer(goal,8);
            goal.addView(LedgerUi.caption(a,a.getResources().getStringArray(R.array.mxp_conditions)[index],dark));
            goal.addView(LedgerUi.action(a,model.status==BetaGrowthModel.Status.ERROR||model.status==BetaGrowthModel.Status.BASELINE||model.status==BetaGrowthModel.Status.STALE?"수집 상태 · 인정 조건 확인":"목표와 획득 조건 보기",true,dark,()->{
                if(!current(a,account))return;
                if(model.status==BetaGrowthModel.Status.ERROR||model.status==BetaGrowthModel.Status.BASELINE||model.status==BetaGrowthModel.Status.STALE)statusHelp(a,model,dark,account);
                else detail(a,state,target,false,dark,account);
            }));
        }else{goal.addView(LedgerUi.heading(a,"모든 영구 명예를 모았어요",dark));goal.addView(LedgerUi.caption(a,"기간별 활동과 레벨 성장은 계속 기록됩니다.",dark));}
        if(size(titles)==0)goal.addView(LedgerUi.caption(a,"첫 칭호: 칭호 수집을 켠 동안 확인된 활동 1일 · 첫 수를 둔 사람",dark));
        parent.addView(goal);Ui.addSpacer(parent,12);

        LinearLayout challenges=card(a,dark);challenges.setTag("beta:challenges");
        LinearLayout today=new LinearLayout(a);today.setOrientation(LedgerUi.stacked(a)?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);today.setGravity(Gravity.CENTER_VERTICAL);
        today.addView(LedgerUi.heading(a,"오늘",dark),new LinearLayout.LayoutParams(LedgerUi.stacked(a)?-1:0,-2,LedgerUi.stacked(a)?0:1));today.addView(LedgerUi.badge(a,model.daily(),dark,false));challenges.addView(today);
        period(a,challenges,"이번 주",model.known()?view.optInt("week"):-1,new int[]{3,5},0xff8d9bff,dark);
        period(a,challenges,"이번 달",model.known()?view.optInt("month"):-1,new int[]{7,10,14},0xffefa956,dark);
        challenges.addView(LedgerUi.caption(a,model.known()?"집계 기준일 · "+view.optString("as_of_day")+" · 한국 시간":"성장 요약 확인 실패 · 0으로 표시하지 않습니다",dark));
        challenges.addView(link(a,"성장 규칙 · 기간별 획득 기록",dark,()->a.detail("level",state)));
        parent.addView(challenges);Ui.addSpacer(parent,12);

        LinearLayout collection=card(a,dark);collection.addView(LedgerUi.heading(a,"나의 수집품",dark));
        collection.addView(LedgerUi.caption(a,"RPG 3.0 명예 "+size(honors)+" / 11 · 칭호 "+size(titles)+" / 5",dark));
        ArrayList<String[]> earned=new ArrayList<>();for(String[] item:MxpHonors.ALL)if(!MxpHonors.isTitle(item[0])&&honors!=null&&honors.has(item[0]))earned.add(item);
        earned.sort((x,y)->Long.compare(honors.optLong(y[0]),honors.optLong(x[0])));
        if(!earned.isEmpty()){
            String[] latest=earned.get(0);collection.addView(link(a,"최근 메달 · "+latest[1],dark,()->detail(a,state,latest,true,dark,account)));
        }else collection.addView(LedgerUi.caption(a,"획득한 메달이 여기에 모입니다. 첫 목표는 위에서 확인하세요.",dark));
        TextView medals=link(a,"명예 도감 · "+size(honors)+"개 획득",dark,()->catalog(a,state,dark,false,0,account));medals.setTag("beta:honors");collection.addView(medals);
        TextView wardrobe=link(a,size(titles)==0?"칭호 모음 · 첫 칭호 획득 조건":"칭호 모음 · "+size(titles)+"개 보유",dark,()->catalog(a,state,dark,true,0,account));wardrobe.setTag("beta:wardrobe");collection.addView(wardrobe);
        collection.addView(LedgerUi.caption(a,"이전 버전에서 얻은 업적과 칭호는 아래 관리 메뉴에서 확인할 수 있습니다.",dark));parent.addView(collection);Ui.addSpacer(parent,12);

        LinearLayout tier=card(a,dark);tier.addView(LedgerUi.heading(a,"활동 티어",dark));
        for(String region:new String[]{"KR","WORLD"}){
            JSONObject facts=state.optJSONObject(region);int rank=facts==null?-1:facts.optInt("tier",-1);
            tier.addView(link(a,(region.equals("KR")?"대한민국":"세계")+" · "+(rank<0?"산정 전 · 활동 자료 필요":RpgGrowthUi.rank(a,rank)),dark,()->a.detail(region,state)));
        }
        parent.addView(tier);Ui.addSpacer(parent,12);
        LinearLayout manage=card(a,dark),items=new LinearLayout(a);items.setOrientation(LinearLayout.VERTICAL);items.setVisibility(View.GONE);
        TextView toggle=link(a,"기록 · 설정 관리  ＋",dark,()->{boolean show=items.getVisibility()!=View.VISIBLE;items.setVisibility(show?View.VISIBLE:View.GONE);});
        toggle.setTag("beta:manage");toggle.setOnClickListener(v->{boolean show=items.getVisibility()!=View.VISIBLE;items.setVisibility(show?View.VISIBLE:View.GONE);toggle.setText(show?"기록 · 설정 관리  −":"기록 · 설정 관리  ＋");androidx.core.view.ViewCompat.setStateDescription(toggle,show?"펼침":"접힘");});manage.addView(toggle);manage.addView(items);parent.addView(manage);
        items.addView(link(a,"업적 · 대표 메달 관리",dark,()->a.open("achievements")));
        items.addView(link(a,"전체 칭호 모음",dark,()->a.open("titles")));
        // Existing switches and archive remain intact; only their presentation location changes.
        a.betaCollections(journal,items);
        items.addView(link(a,"확인된 토큰 기록",dark,()->a.detail("tokens",state)));
        items.addView(link(a,a.getString(R.string.profile_share),dark,()->Ui.startSecondaryActivity(a,ProfileShareActivity.class)));
        items.addView(link(a,"로그인 · 업데이트 진단",dark,()->{if(current(a,account))BetaDiagnostics.show(a);}));
        return true;
    }
    static LinearLayout card(GrowthActivity a,boolean dark){LinearLayout card=Ui.card(a,dark);card.setPadding(Ui.dp(a,16),Ui.dp(a,16),Ui.dp(a,16),Ui.dp(a,16));return card;}
    static TextView link(GrowthActivity a,String label,boolean dark,Runnable action){
        TextView row=Ui.text(a,label+"  ›",15,Ui.mainText(dark));row.setGravity(Gravity.CENTER_VERTICAL);row.setMinHeight(Ui.dp(a,48));
        row.setPadding(0,Ui.dp(a,8),0,Ui.dp(a,8));row.setFocusable(true);row.setClickable(true);row.setContentDescription(label+" · 열기");
        android.util.TypedValue ripple=new android.util.TypedValue();a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true);
        if(ripple.resourceId!=0)row.setBackgroundResource(ripple.resourceId);row.setOnClickListener(v->action.run());return row;
    }
    static void statusHelp(GrowthActivity a,BetaGrowthModel model,boolean dark,String account){
        if(!current(a,account))return;
        if(a.detailDialog!=null)a.detailDialog.dismiss();
        a.detailDialog=new androidx.appcompat.app.AlertDialog.Builder(a).setTitle("수집 상태 · 활동 인정 조건")
            .setMessage(model.label()+"\n\n확인된 Codex 활동 구간만 성장에 반영합니다. 일반 ChatGPT 대화, 사용자 직접 기록, 화면 조회에는 성장 보상을 지급하지 않습니다. 첫 관측은 기준 기록이며 이후 확인 가능한 활동 변화가 필요합니다.\n\n명예·칭호 수집 설정은 기록 · 설정 관리에서 확인하세요. 관측 시각은 활동 관측 기록의 시각이며 서버 연결 성공 시각이 아닙니다.")
            .setNegativeButton("닫기",null).setPositiveButton("홈에서 연결 상태 보기",(d,w)->{if(current(a,account))MatteNav.open(a,1);}).create();a.detailDialog.show();
    }
    static int size(JSONObject j){return j==null?0:j.length();}
    static void period(GrowthActivity a,LinearLayout p,String name,int count,int[] stages,int color,boolean dark){
        Ui.addSpacer(p,14);LinearLayout row=new LinearLayout(a);row.setOrientation(LedgerUi.stacked(a)?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);int target=stages[stages.length-1];
        row.addView(LedgerUi.heading(a,name,dark),new LinearLayout.LayoutParams(LedgerUi.stacked(a)?-1:0,-2,LedgerUi.stacked(a)?0:1));row.addView(LedgerUi.badge(a,count<0?"확인 불가":count+" / "+target+"일",dark,false));p.addView(row);Ui.addSpacer(p,6);
        if(count>=0){
            if(target==5){LinearLayout dots=new LinearLayout(a);for(int i=1;i<=5;i++){TextView dot=Ui.text(a,count>=i?"●":"○",22,count>=i?color:LedgerUi.muted(dark));dot.setGravity(Gravity.CENTER);dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);dots.addView(dot,new LinearLayout.LayoutParams(0,-2,1));}dots.setContentDescription("이번 주 확인된 활동 "+count+"일");p.addView(dots);}
            else {ImageView bar=new ImageView(a);bar.setImageDrawable(new MilestoneBar(count,color,dark));bar.setContentDescription(name+" "+count+"일, 보상 지점 7일, 10일, 14일");p.addView(bar,new LinearLayout.LayoutParams(-1,Ui.dp(a,22)));}
        }
        p.addView(LedgerUi.caption(a,target==5?"3일 보상 20 MXP · 5일 누적 30 MXP":"7일 40 · 10일 80 · 14일 120 MXP (누적)",dark));
        int next=count<0?-1:BetaGrowthModel.next(count,stages);
        p.addView(LedgerUi.caption(a,count<0?"요약을 확인한 뒤 다음 보상을 안내합니다":next<0?"이번 기간 목표 달성": "다음 보상까지 "+(next-count)+"일 · 추가 "+BetaGrowthModel.rewardDelta(count,target==5)+" MXP",dark));
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
    static final class MilestoneBar extends Drawable {
        final Paint paint=new Paint(3);final int count,color;final boolean dark;
        MilestoneBar(int count,int color,boolean dark){this.count=count;this.color=color;this.dark=dark;}
        public void draw(Canvas canvas){Rect b=getBounds();float left=b.left+6,right=b.right-6,y=b.exactCenterY();paint.setStrokeWidth(5);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(dark?0xff657080:0xff667384);canvas.drawLine(left,y,right,y,paint);paint.setColor(color);canvas.drawLine(left,y,left+(right-left)*Math.min(count,14)/14f,y,paint);for(int stage:new int[]{7,10,14}){float x=left+(right-left)*stage/14f;paint.setColor(count>=stage?color:dark?0xffc6ced8:0xff33435a);canvas.drawCircle(x,y,5,paint);}}
        public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
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
