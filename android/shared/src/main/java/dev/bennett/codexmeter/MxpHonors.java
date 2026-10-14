package dev.bennett.codexmeter;

/** New permanent decorations; the original 53-entry catalog is unchanged. No bonus MXP. */
public final class MxpHonors {
    private MxpHonors(){}
    public static final String[][] ALL={
        {"rpg3.first_day","첫 발자국","First Footstep","common","1 verified core day"},
        {"rpg3.first_week","첫 주의 페이스","First Week Pace","common","1 core week with at least 3 verified days"},
        {"rpg3.month_four_weeks","달력을 잇는 발자국","Across Four Weeks","rare","4 distinct weeks in a calendar month; decorative only"},
        {"rpg3.daily_7","일곱 번의 출근 도장","Seven Stamps","rare","7 verified core days"},
        {"rpg3.weekly_4","한 달의 리듬","Monthly Rhythm","rare","4 distinct core weeks with at least 3 days"},
        {"rpg3.month_gold_3","세 번의 황금달","Three Golden Months","epic","3 distinct gold months"},
        {"rpg3.month_gold_6","반년의 금빛 기록","Six Golden Months","legendary","6 distinct gold months"},
        {"rpg3.annual_gold_1","한 바퀴의 황금 궤도","Golden Orbit","legendary","1 gold personal season"},
        {"rpg3.annual_gold_3","세 바퀴의 기록자","Three Orbits","mythic","3 gold personal seasons"},
        {"rpg3.frame_30","작업대 개방","Workbench Frame","epic","level 30 and 14 new verified core days"},
        {"rpg3.frame_50","황금 궤도의 문장","Golden Orbit Frame","mythic","level 50 and 1 gold personal season"},
        {"rpg3.starter","첫 수를 둔 사람","First Move","common","1 verified day while title collection enabled"},
        {"rpg3.workbench","질문창의 작업반장","Workbench Foreman","epic","level 30 and 14 new verified title days"},
        {"rpg3.title_rhythm","일곱 날의 손맛","Seven-day Rhythm","rare","7 verified title days"},
        {"rpg3.title_orbit","황금달의 단골","Golden Month Regular","legendary","6 title-collection months with 14 verified days each"},
        {"rpg3.title_chronicler","세 해의 기록장인","Three-year Chronicler","mythic","3 personal title seasons with 10 gold months each"}
    };
    public static boolean isTitle(String id){return id.equals("rpg3.starter")||id.equals("rpg3.workbench")||id.startsWith("rpg3.title_");}
    public static String[] find(String id){for(String[] row:ALL)if(row[0].equals(id))return row;return null;}
    public static RpgCatalog.Entry title(String id){String[] row=find(id);return row==null?null:new RpgCatalog.Entry(id,row[1],row[2],"special",1,row[1],row[2],row[3],0,0,0);}
}
