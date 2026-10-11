package dev.bennett.codexmeter;
import java.util.*;

/** Stable IDs, deterministic conditions and bilingual metadata. Never randomize an unlock. */
public final class RpgCatalog {
    public static final class Entry {
        public final String id,ko,en,metric,title,titleEn,rarity;public final long target;public final int bonus;public final int days,weeks;
        Entry(String id,String ko,String en,String metric,long target,String title,String titleEn,String rarity,int bonus,int days,int weeks){this.id=id;this.ko=ko;this.en=en;this.metric=metric;this.target=target;this.title=title;this.titleEn=titleEn;this.rarity=rarity;this.bonus=bonus;this.days=days;this.weeks=weeks;}
        public String descriptionKo(){String amount=String.format(java.util.Locale.KOREA,"%,d",target),text;
            switch(metric){case "tokens":text="확인된 토큰 활동 지표 "+amount+" 이상";break;case "days":text="확인된 누적 활동 "+amount+"일";break;case "streak":text="동일 날짜 기준의 연속 활동 "+amount+"일";break;case "weeks":text="인증된 완전 주간 "+amount+"회";break;case "tier":text=en.replace(" Crest","")+" 이상 등급의 달성 기록";break;case "utilization":text="동일 정책의 결제 기간 활용도 "+amount+"% 이상";break;case "plans":text="확인된 요금제 "+amount+"종의 기록";break;case "dawn":text="서울 시간 00~06시의 실제 소비 증가 확인";break;case "weekend":text="서울 날짜 기준 주말 실제 활동 "+amount+"일";break;default:text="확인된 사용 이력 보유";}
            return text+(days>0?" · 누적 활동 "+days+"일 이상":"")+(weeks>0?" · 인증 주간 "+weeks+"회 이상":"");
        }
        public String descriptionEn(){return "Verified "+metric+" >= "+target+(days>0?"; active days >= "+days:"")+(weeks>0?"; certified weeks >= "+weeks:"")+". Unknown intervals do not count.";}
    }
    public static final List<Entry> ALL;
    static {
        List<Entry> list=new ArrayList<>();
        long[] tokens={1_000_000L,10_000_000L,100_000_000L,1_000_000_000L,10_000_000_000L,100_000_000_000L,1_000_000_000_000L};
        String[] tk={"첫 불씨","태양의 깃털","토큰의 강","천공의 날개","일식을 삼킨 자","태양의 귀환","천공의 지배자"};
        String[] te={"First Ember","Solar Feather","Token River","Celestial Wings","Eclipse Eater","Sun's Return","Sky Sovereign"};
        String[] tt={"프롬프트 예열 완료","새 깃털 장착","토큰 폭풍의 주인","캐시도 내 편","일식 뷔페 단골","태양 야근조","삼족오 최종 보스"};
        String[] tte={"Prompt Warmed Up","New Feather Equipped","Token Storm Keeper","Cache on My Side","Eclipse Buffet Regular","Solar Night Shift","Final Raven Boss"};
        for(int i=0;i<tokens.length;i++)add(list,"tokens_"+tokens[i],tk[i],te[i],"tokens",tokens[i],tt[i],tte[i],i>=4?"mythic":i>=2?"rare":"common",50+i*25,i==4?365:i==5?730:i==6?1825:0,i==4?26:i==5?52:i==6?156:0);
        long[] days={1,7,30,100,180,365,730};String[] dk={"깨어난 삼족오","일곱 번의 일출","한 달의 비행","백일의 날개","반년의 둥지","태양력 완주","두 바퀴의 태양"};
        String[] de={"Awakened Raven","Seven Sunrises","Month of Flight","Hundred-Day Wings","Half-Year Nest","Solar Year","Two Solar Orbits"};
        String[] dt={"둥지 탈출 성공","주간 출석의 신","월세 뽑는 까마귀","매일 태양을 깨우는 자","반년째 안 착륙","구독료를 부리는 자","고인물의 세 번째 발"};
        String[] dte={"Escaped the Nest","Weekly Attendance Deity","Rent-Earning Raven","Daily Sun Waker","Still Flying at Six Months","Subscription Tamer","Veteran's Third Foot"};
        for(int i=0;i<days.length;i++)add(list,"days_"+days[i],dk[i],de[i],"days",days[i],dt[i],dte[i],i>3?"epic":"common",30+i*15,0,0);
        long[] streaks={3,7,14,30};String[] sk={"끊기지 않는 불씨","칠일의 햇살","쌍주의 열기","태양의 연속 항로"};String[] se={"Unbroken Ember","Week of Sunlight","Fortnight Heat","Solar Flight Path"};
        String[] st={"작심삼일 돌파자","로그아웃이 뭔데","출석부 파괴자","삼족오 개근상"};String[] ste={"Three-Day Barrier Breaker","What's Logout","Attendance Ledger Breaker","Perfect Attendance Raven"};
        for(int i=0;i<streaks.length;i++)add(list,"streak_"+streaks[i],sk[i],se[i],"streak",streaks[i],st[i],ste[i],"rare",50+i*20,0,0);
        long[] weeks={1,4,12,26,52};String[] wk={"첫 완주","한 달의 궤도","계절의 비행사","반년의 항성","장기 궤도 진입"};String[] we={"First Full Window","Monthly Orbit","Season Pilot","Half-Year Star","Long Orbit"};
        String[] wt={"한도 관측대장","초기화에 안 속는 자","계절을 넘는 날개","장기전 전문 까마귀","연간 이용권의 주인"};String[] wte={"Quota Observer Captain","Reset-Proof Observer","Season-Spanning Wings","Long-Game Raven","Annual Orbit Keeper"};
        for(int i=0;i<weeks.length;i++)add(list,"weeks_"+weeks[i],wk[i],we[i],"weeks",weeks[i],wt[i],wte[i],i>2?"epic":"rare",50+i*25,0,0);
        String[] ranks={"IRON","BRONZE","SILVER","GOLD","PLATINUM","EMERALD","DIAMOND","MASTER","GRANDMASTER","CHALLENGER"};
        String[] rt={"철새 아님","동빛 세 발","은빛 부리","금빛 둥지 주인","백금 날개 정비사","에메랄드 광합성","다이아를 쪼는 자","태양의 계승자","하늘 위의 고인물","챌린저 삼족오"};
        String[] rte={"No Ordinary Migrant","Bronze Three Feet","Silver Beak","Golden Nest Keeper","Platinum Wing Mechanic","Emerald Photosynthesis","Diamond Pecking Raven","Solar Heir","Veteran Above the Clouds","Challenger Raven"};
        for(int i=0;i<10;i++)add(list,"tier_"+i,ranks[i]+"의 문장",ranks[i]+" Crest","tier",i+1,rt[i],rte[i],i>=7?"legendary":"rare",30+i*15,0,0);
        add(list,"value_50","반쯤 열린 태양","Half-Open Sun","utilization",50,"구독료 회수 원정대","Subscription Expedition","common",50,0,0);
        add(list,"value_100","한도 완주자","Full Allocation Runner","utilization",100,"한도 철거반","Quota Demolition Crew","rare",80,0,0);
        add(list,"value_200","두 번의 태양","Double Sun","utilization",200,"쿠폰보다 많이 날았다","Flew Beyond the Coupon","epic",120,0,0);
        add(list,"plans_2","옮겨도 남는 날개","Wings That Follow","plans",2,"요금제 이사 전문가","Plan Moving Expert","common",30,0,0);
        add(list,"dawn_1","새벽의 태양","Dawn Sun","dawn",1,"새벽 서버의 수호자","Dawn Server Guardian","rare",50,0,0);
        add(list,"weekend_8","주말의 천공","Weekend Sky","weekend",8,"주말 반납 완료","Weekend Surrendered","rare",60,0,0);
        add(list,"combined_1b","불꽃과 인내","Flame and Patience","tokens",1_000_000_000L,"불사조인 척하는 삼족오","Raven Pretending to Be Phoenix","epic",150,100,12);
        add(list,"heritage_1","잊지 않은 날개","Remembered Wings","heritage",1,"기록 복구의 생존자","History Recovery Survivor","common",30,0,0);
        ALL=Collections.unmodifiableList(list);
    }
    private static void add(List<Entry> list,String id,String ko,String en,String metric,long target,String title,String titleEn,String rarity,int bonus,int days,int weeks){list.add(new Entry(id,ko,en,metric,target,title,titleEn,rarity,bonus,days,weeks));}
    private RpgCatalog(){}
}
