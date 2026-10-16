package dev.bennett.codexmeter;

import android.content.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.function.Consumer;

final class BetaExperienceUi {
    static String t(Context c,String ko,String en,Object... args){return String.format(BetaCopy.ko(c)?java.util.Locale.KOREAN:java.util.Locale.US,BetaCopy.ko(c)?ko:en,args);}
    static void open(AppCompatActivity a,String page){a.startActivity(new Intent(a,BetaHubActivity.class).putExtra("page",page));}
    static void focus(MainActivity a,LinearLayout parent,UsageSnapshot snapshot,boolean dark,Consumer<Button> refresh){
        LinearLayout card=BetaUi.group(parent,t(a,"지금 할 일","Your next action"),dark);card.setTag("beta998:focus");
        boolean failed=!AppPreferences.getLastError(a).isEmpty();
        boolean fresh=snapshot!=null&&UsageInsights.freshness(snapshot.fetchedAtMillis,System.currentTimeMillis(),RefreshScheduler.effectiveRefreshMinutes(a))==UsageInsights.Freshness.FRESH;
        boolean reset=snapshot!=null&&snapshot.longWindow()!=null&&snapshot.longWindow().effectiveResetAtMillis(snapshot.fetchedAtMillis)<=System.currentTimeMillis();
        JSONObject growth=BetaGrowthStore.peek(a);boolean done=growth!=null&&growth.optInt("day")>0,paused=growth!=null&&!growth.optBoolean("enabled");
        String state=TrialDemo.active(a)?t(a,"가상 데모 · 실제 활동 인증 아님","Synthetic demo · not verified real activity")
            :failed?t(a,"확인 실패 · 이전 기록을 보관 중","Check failed · previous record retained")
            :!fresh||reset?t(a,"새 사용량 확인이 필요해요","A fresh usage check is needed")
            :paused?t(a,"사용량 확인됨 · 성장 수집 중지","Usage checked · growth collection paused")
            :done?t(a,"오늘 확인 완료 · 다음 목표를 살펴보세요","Checked today · explore your next goal")
            :t(a,"오늘의 성장 확인을 이어가세요","Continue today's growth check");
        card.addView(BetaUi.body(a,state,dark));
        if(snapshot!=null)card.addView(BetaUi.note(a,a.getString(R.string.next_observed_at,V3Display.time(snapshot.fetchedAtMillis)),dark));
        if(snapshot!=null&&snapshot.monthly!=null&&BetaPreferences.preset(a).equals("simple"))
            card.addView(BetaUi.body(a,t(a,"월간 남음 · %s","Monthly remaining · %s",UsagePrecision.remaining(snapshot.monthly,AppPreferences.usageDecimalPlaces(a))),dark));
        if(!fresh||reset||failed||!done&&!paused){Button action=BetaUi.action(a,t(a,"사용량 확인하기","Check usage"),true,dark,()->{});action.setTag("beta998:check");action.setEnabled(!MainActivity.refreshInProgress());action.setOnClickListener(v->refresh.accept(action));card.addView(action);}
        else card.addView(BetaUi.action(a,paused?t(a,"수집 설정 보기","Collection settings"):t(a,"성장 경로 보기","Explore growth path"),true,dark,()->{if(paused)a.startActivity(new Intent(a,MeterSettingsActivity.class));else open(a,"journey");}));
        BetaHomeUi.growthLinks(a,card,growth,dark);
        card.addView(BetaUi.row(a,t(a,"연결·업데이트 복구","Connection and update recovery"),t(a,"상태와 해결 방법 확인","Status and recovery actions"),dark,()->open(a,"health")));
        card.addView(BetaUi.row(a,t(a,"주간·월간 회고","Weekly and monthly recap"),t(a,"확인된 기록만 요약","Only recorded evidence"),dark,()->open(a,"recap")));
    }
    static void shortcuts(GrowthActivity a,LinearLayout parent,boolean dark){
        LinearLayout card=BetaUi.group(parent,t(a,"나의 성장 경로","My growth journey"),dark);
        card.addView(BetaUi.row(a,t(a,"목표 고정·명예 경로","Pin a goal and explore honors"),t(a,"기존 획득 조건 그대로","Existing award conditions preserved"),dark,()->open(a,"journey")));
        card.addView(BetaUi.row(a,t(a,"획득 타임라인·회고","Award timeline and recap"),"",dark,()->open(a,"recap")));
    }
    static void preset(AppCompatActivity a){
        String[] values={"simple","growth","detail"};String[] names={t(a,"간단히 보기","Simple"),t(a,"성장 중심","Growth"),t(a,"자세히 보기","Detailed")};
        new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(t(a,"홈 화면 구성","Home layout"))
            .setSingleChoiceItems(names,java.util.Arrays.asList(values).indexOf(BetaPreferences.preset(a)),(d,i)->{
                BetaPreferences.prefs(a).edit().putString("preset",values[i]).apply();d.dismiss();a.recreate();
            }).setNegativeButton(BetaCopy.t(a,"cancel"),null).show();
    }
}
