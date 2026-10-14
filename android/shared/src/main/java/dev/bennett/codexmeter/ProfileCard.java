package dev.bennett.codexmeter;

import java.util.*;
import org.json.JSONObject;

/** Detached share allow-list. Never retains the source journal or account identifier. */
public final class ProfileCard {
    public final String nickname, titleId, frameId;
    public final int tier, level;
    public final List<String> medals;
    private ProfileCard(String name,int tier,int level,List<String> medals,String title,String frame){
        this.nickname=name;this.tier=tier;this.level=level;
        this.medals=Collections.unmodifiableList(new ArrayList<>(medals));this.titleId=title;this.frameId=frame;
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
        if(MxpEconomy.state(state)!=null)level=MxpEconomy.state(state).optInt("level",level);
        List<String> earned=RpgGrowthFacts.earned(state),medals=new ArrayList<>();
        if(selected!=null)for(String id:selected)if(medals.size()<3&&earned.contains(id)&&!medals.contains(id))medals.add(id);
        String title=state==null?"":state.optString("equipped_title");
        if(!(RpgTitleCatalog.owned(state).contains(title)&&RpgGrowthFacts.titleEntry(title)!=null)&&!(MxpEconomy.state(state)!=null&&MxpEconomy.state(state).optJSONObject("titles").has(title)))title="";
        JSONObject mxp=MxpEconomy.state(state),honors=mxp==null?null:mxp.optJSONObject("honors");String frame=honors==null?"":honors.has("rpg3.frame_50")?"rpg3.frame_50":honors.has("rpg3.frame_30")?"rpg3.frame_30":"";
        return new ProfileCard(nickname(name),tier,level,medals,title,frame);
    }
}
