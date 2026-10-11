package dev.bennett.codexmeter;

/** Original local vectors; product family labels never infer a dollar price. */
final class RpgPlanSeals {
    private RpgPlanSeals(){}
    static int image(String plan){switch(PlanArtwork.key(plan)){
        case "free":return R.drawable.rpg_seal_free;
        case "go":return R.drawable.rpg_seal_go;
        case "plus":return R.drawable.rpg_seal_plus;
        case "prolite":return R.drawable.rpg_seal_prolite;
        case "pro":return R.drawable.rpg_seal_pro;
        default:return R.drawable.rpg_seal_neutral;
    }}
    static String concept(String plan){switch(PlanArtwork.key(plan)){
        case "free":return "Graphite Origin";
        case "go":return "Bronze Motion";
        case "plus":return "Silver Vanguard";
        case "prolite":return "Platinum Core";
        case "pro":return "Obsidian Sovereign";
        default:return "Neutral Seal";
    }}
}
