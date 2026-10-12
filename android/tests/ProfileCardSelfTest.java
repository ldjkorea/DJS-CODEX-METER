package dev.bennett.codexmeter;
import java.util.*;
import org.json.*;
public final class ProfileCardSelfTest {
    static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        ProfileCard empty=ProfileCard.from(null,Arrays.asList("unknown"),null);
        check(empty.tier==-1&&empty.level==0&&empty.medals.isEmpty()&&empty.titleId.isEmpty(),"Unknown remains unknown");
        JSONObject d=AccountProgress.empty();CollectionRestart.prepare(d,1791774000000L);JSONObject s=RpgEngine.state(d);
        s.put("xp",1000).put("equipped_title","locked").put("email","SECRET_EMAIL").put("account","SECRET_ACCOUNT").put("prompt","SECRET_PROMPT").put("payment","SECRET_PAYMENT");
        s.getJSONObject("KR").put("tier",6);List<String> ids=new ArrayList<>();
        for(int i=0;i<4;i++){RpgCatalog.Entry e=RpgCatalog.ALL.get(i);ids.add(e.id);s.getJSONObject("achievements").put(e.id,new JSONObject().put("unlocked",true));}
        String before=s.toString();ProfileCard card=ProfileCard.from(s,Arrays.asList(ids.get(2),"unknown",ids.get(2),ids.get(0),ids.get(1),ids.get(3)),"Name\n\u202e\u0000");
        check(card.nickname.equals("Name"),"No control or bidi characters");check(card.tier==6&&card.level==RpgEngine.level(1000),"Actual display values");
        check(card.medals.equals(Arrays.asList(ids.get(2),ids.get(0),ids.get(1))),"Three earned chosen medals in order");check(card.titleId.isEmpty(),"No locked title");check(before.equals(s.toString()),"No source mutation");
        boolean blocked=false;try{card.medals.add("x");}catch(UnsupportedOperationException e){blocked=true;}check(blocked,"Immutable detached list");
        String unicode=ProfileCard.nickname("😀".repeat(30));check(unicode.codePointCount(0,unicode.length())==24,"Unicode-safe length");
        for(java.lang.reflect.Field f:ProfileCard.class.getDeclaredFields())check(!Arrays.asList("account","email","xp","journal","tokens","billing","project").contains(f.getName()),"Privacy allow-list");
        s.getJSONObject("KR").put("tier",50);check(ProfileCard.from(s,ids,"").tier==-1,"Invalid tier unknown");
        System.out.println("Profile share allow-list: "+checks+" checks passed");
    }
}
