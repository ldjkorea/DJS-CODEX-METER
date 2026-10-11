package dev.bennett.codexmeter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Presentation only. Never changes achievement identities, conditions or rewards. */
public final class RpgDisplayCopy {
    private static final Map<String,String[]> NAMES;
    static {
        Map<String,String[]> names=new LinkedHashMap<>();
        group(names,"tokens_",new String[]{"1000000","10000000","100000000","1000000000","10000000000","100000000000","1000000000000"},
            new String[]{"백만 토큰의 시작","천만 토큰 돌파","1억 토큰 클럽","10억 토큰의 전설","100억의 개척자","천억의 기록왕","1조 토큰의 신화"},
            new String[]{"First Million","Ten Million Strong","100 Million Club","Billion-Token Legend","Ten-Billion Pioneer","100 Billion Archive","Trillion-Token Myth"});
        group(names,"days_",new String[]{"1","7","30","100","180","365","730"},
            new String[]{"첫 출격","일주일의 발자취","한 달의 동료","백일의 약속","반년의 내공","1년의 여정","2년의 유산"},
            new String[]{"First Launch","Seven Days Together","One-Month Companion","Hundred-Day Promise","Six-Month Veteran","Year of Progress","Two-Year Legacy"});
        group(names,"streak_",new String[]{"3","7","14","30"},new String[]{"작심삼일 클리어","7일 연속 출격","2주 연속 질주","30일 개근왕"},new String[]{"Three-Day Breakthrough","Seven-Day Streak","Fortnight Momentum","30-Day Perfect Run"});
        group(names,"weeks_",new String[]{"1","4","12","26","52"},new String[]{"첫 주간 결산","4주 기록 수집가","12주 장기 항해","26주 관측의 달인","52주 기록의 전설"},new String[]{"First Weekly Record","Four-Week Collector","Twelve-Week Voyage","26-Week Observer","52-Week Legend"});
        String[] ko={"아이언의 첫걸음","브론즈의 도약","실버의 날개","골드의 왕관","플래티넘의 위엄","에메랄드의 심장","다이아몬드의 광휘","마스터의 증표","그랜드마스터의 왕좌","챌린저의 전설"};
        String[] en={"Iron Beginning","Bronze Ascent","Silver Wings","Golden Crown","Platinum Prestige","Emerald Heart","Diamond Radiance","Master's Insignia","Grandmaster's Throne","Challenger Legend"};
        for(int i=0;i<10;i++)names.put("tier_"+i,new String[]{ko[i],en[i]});
        String[][] rest={
            {"value_50","절반의 한도, 확실한 발자취","Halfway Through"},
            {"value_100","한도 100% 완주","Full Allocation Complete"},
            {"value_200","두 배의 한도를 넘어서","Double Allocation"},
            {"plans_2","플랜이 바뀌어도 나답게","New Plan, Same Legacy"},
            {"dawn_1","새벽에 남긴 발자취","After-Hours Footprint"},
            {"weekend_8","여덟 번의 주말 기록","Eight Weekend Records"},
            {"combined_1b","10억 토큰의 베테랑","Billion-Token Veteran"},
            {"heritage_1","돌아온 나의 기록","Legacy Recovered"},
            {"tokens_500m","5억 토큰 마일스톤","Half-Billion Milestone"},
            {"tokens_5b","50억 토큰의 서사시","Five-Billion Epic"},
            {"days_60","60일의 동행","Sixty Days Together"},
            {"days_1000","천일의 전설","Thousand-Day Legend"},
            {"streak_60","60일 끊김 없는 항로","Sixty-Day Unbroken Run"},
            {"weekend_24","24번의 주말 발자취","24 Weekend Footprints"},
            {"dawn_veteran","별빛 아래 베테랑","Starlit Veteran"},
            {"value_75","한도 완주까지 한 걸음","Three-Quarter Milestone"},
            {"value_150","기본 한도를 넘어","Beyond the Allocation"},
            {"plans_3","세 플랜, 하나의 기록","Three Plans, One Legacy"},
            {"weeks_8","8주 기록의 수호자","Eight-Week Keeper"},
            {"combined_100m","1억 토큰과 한 달의 습관","100 Million, Lasting Habit"}};
        for(String[] row:rest)names.put(row[0],new String[]{row[1],row[2]});
        NAMES=Collections.unmodifiableMap(names);
    }
    private static void group(Map<String,String[]> map,String prefix,String[] ids,String[] ko,String[] en){for(int i=0;i<ids.length;i++)map.put(prefix+ids[i],new String[]{ko[i],en[i]});}
    public static String name(RpgCatalog.Entry e,boolean ko){String[] row=NAMES.get(e.id);return row==null?(ko?e.ko:e.en):row[ko?0:1];}
    public static String flavor(RpgCatalog.Entry e,boolean ko){
        switch(e.metric){
            case "tokens":return ko?"한 번의 질문은 작아도, 쌓인 기록은 전설이 됩니다.":"Small prompts. A growing legacy.";
            case "streak":return ko?"어제의 나와 오늘의 내가 합작한 메달. 무리한 야근은 조건이 아닙니다.":"Yesterday and today, working together. Overtime is not required.";
            case "days":return ko?"화려한 하루보다 오래 남는 건, 다시 찾아온 발자취.":"A lasting journey is built one return at a time.";
            case "weeks":return ko?"초기화는 지나가도 기록은 남습니다. 주간 결산 수집 완료!":"Resets pass. Your weekly records stay.";
            case "tier":return ko?"다음 문장을 향해. 이미 오른 자리는 기록에 새겨집니다.":"Reach for the next crest. Your milestone stays.";
            case "utilization":return ko?"관상용 구독은 졸업. 필요한 작업이 남긴 발자취입니다.":"A record of putting your plan to work.";
            case "plans":return ko?"요금제는 갈아타도 성취는 이삿짐에 빠지지 않습니다.":"A new plan never leaves your achievements behind.";
            case "dawn":return ko?"조용한 시간에 남은 작은 기록. 메달보다 수면이 먼저입니다.":"A quiet-hour footprint. Rest comes before medals.";
            case "weekend":return ko?"주말에도 남은 발자취. 쉬는 날은 쉬어도 됩니다.":"Weekend footprints, with room for days off.";
            default:return ko?"돌아온 기록에도 주인은 그대로. 반갑다, 나의 성취!":"Your history is home. Welcome back, achievements.";
        }
    }
    public static String condition(RpgCatalog.Entry e,boolean ko){
        String amount=String.format(ko?java.util.Locale.KOREA:java.util.Locale.US,"%,d",e.target),text;
        switch(e.metric){
            case "tokens":text=ko?"확인된 누적 토큰 "+amount:"Verified token milestone: "+amount;break;
            case "days":text=ko?"누적 활동 "+amount+"일":"Active days: "+amount;break;
            case "streak":text=ko?"연속 활동 "+amount+"일":"Consecutive days: "+amount;break;
            case "weeks":text=ko?"유효 주간 기록 "+amount+"회":"Certified weekly records: "+amount;break;
            case "tier":text=ko?name(e,true)+" · 해당 등급 도달":e.en+" reached";break;
            case "utilization":text=ko?"결제 기간 활용도 "+amount+"%":"Billing-period utilization: "+amount+"%";break;
            case "plans":text=ko?"요금제 "+amount+"종 기록":"Recorded plan types: "+amount;break;
            case "dawn":text=ko?"서울 00~06시 실제 소비 기록":"Observed usage, 00–06 Seoul time";break;
            case "weekend":text=ko?"주말 활동 "+amount+"일":"Weekend activity days: "+amount;break;
            default:text=ko?"이전 사용 기록 승계":"Inherited usage history";
        }
        return text+(e.days>0?(ko?" · 활동 "+e.days+"일":" · "+e.days+" active days"):"")+(e.weeks>0?(ko?" · 유효 주간 "+e.weeks+"회":" · "+e.weeks+" certified weeks"):"");
    }
    private RpgDisplayCopy(){}
}
