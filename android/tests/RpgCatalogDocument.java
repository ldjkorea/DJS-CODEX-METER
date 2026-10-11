package dev.bennett.codexmeter;
/** Developer documentation from the exact production catalog. No user state. */
public final class RpgCatalogDocument {
    public static void main(String[] args){
        System.out.println("# 삼족오 업적·칭호 도감 — RPG v1\n\n41개 업적과 41개 고정 칭호. 조건 충족 여부는 실제 기록으로 판정하고, 모르는 활동을 만들지 않는다. 토큰은 저장된 근거 범위(공식 백엔드 활동/확인된 로컬 처리량)를 따른다. 정확한 국가 순위가 아니다.\n");
        System.out.println("| ID | 업적 | 실제 해금 조건 | 칭호 | 희귀도 | 최초 XP |\n|---|---|---|---|---|---:|");
        for(RpgCatalog.Entry e:RpgCatalog.ALL)System.out.println("| "+e.id+" | "+e.ko+" | "+e.descriptionKo()+" | "+e.title+" | "+e.rarity+" | "+e.bonus+" |");
        System.out.println("\n이 도감은 배포 UI가 아니라 개발 확인 자료다. 칭호는 추천 후 저장하며, 화면을 다시 열어도 바뀌지 않는다. 획득한 것만 장착할 수 있고, XP와 티어에 영향을 주지 않는다.");
    }
}
