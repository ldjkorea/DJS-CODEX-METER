package dev.bennett.codexmeter;

import java.util.*;
import org.json.JSONObject;

/** Detached share allow-list. Never retains the source journal or account identifier. */
public final class ProfileCard {
    public final String nickname, titleId;
    public final int tier, level;
    public final List<String> medals;
    private ProfileCard(String name,int tier,int level,List<String> medals,String title){
        this.nickname=name;this.tier=tier;this.level=level;
        this.medals=Collections.unmodifiableList(new ArrayList<>(medals));this.titleId=title;
    }
    public static String nickname(String value){
        StringBuilder clean=new StringBuilder();
        if(value!=null)value.codePoints().filter(cp->!Character.isISOControl(cp)&&Character.getType(cp)!=Character.FORMAT).limit(24).forEach(clean::appendCodePoint);
        return clean.toString().trim();
    }
    public static ProfileCard from(JSONObject state,List<String> selected,String name){
        JSONObject kr=state==null?null:state.optJSONObject("KR");int tier=kr==null?-1:kr.optInt("tier",-1);
        return from(state,selected,name,tier);
    }
    public static ProfileCard from(JSONObject state,List<String> selected,String name,int tier){
        if(tier<0||tier>9)tier=-1;
        int level=state==null||!state.has("xp")||state.optLong("xp",-1)<0?0:RpgEngine.level(state.optLong("xp"));
        List<String> earned=RpgGrowthFacts.earned(state),medals=new ArrayList<>();
        if(selected!=null)for(String id:selected)if(medals.size()<3&&earned.contains(id)&&!medals.contains(id))medals.add(id);
        String title=state==null?"":state.optString("equipped_title");
        if(!RpgTitleCatalog.owned(state).contains(title)||RpgGrowthFacts.titleEntry(title)==null)title="";
        return new ProfileCard(nickname(name),tier,level,medals,title);
    }
}
