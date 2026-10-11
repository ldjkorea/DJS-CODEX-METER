package dev.bennett.codexmeter;
import java.util.*;
import org.json.*;

/** Integration contracts, synthetic account records only. */
public final class RpgStep3SelfTest {
    static int checks;static void check(boolean v,String name){checks++;if(!v)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        JSONObject d=AccountProgress.empty();d.getJSONObject("tier").put("tier",7).put("highest",9);
        long now=System.currentTimeMillis();RpgEngine.reconcile(d,List.of(),List.of(),List.of(),List.of(),now,true);
        JSONObject kr=RpgEngine.summary(d).getJSONObject("KR");check(kr.getInt("tier")==7&&kr.getInt("highest")==9,"Existing current/highest migrate into protected common Korea result");
        check(RpgEngine.state(d).getJSONObject("WORLD").getInt("highest")<9,"World remains independent, not a copied Korean best");
        long xp=RpgEngine.state(d).getLong("xp");String chosen=RpgEngine.recommend(d,new Random(7));check(!chosen.isEmpty()&&RpgEngine.equip(d,chosen),"Migrated tier achievements unlock eligible title");
        check(RpgEngine.equip(d,"")&&!RpgEngine.state(d).has("equipped_title"),"Title can be removed without deleting unlocks");check(RpgEngine.state(d).getLong("xp")==xp,"Recommend/equip/remove never grants XP");
        d.getJSONObject("tier").put("tier",0);RpgEngine.reconcile(d,List.of(),List.of(),List.of(),List.of(),now+86400000,true);
        check(RpgEngine.summary(d).getJSONObject("KR").getInt("tier")==7,"Later legacy reset cannot lower common Korea tier");
        JSONObject restored=AccountProgress.empty();RpgEngine.merge(restored,"account",RpgEngine.export(d,"account"),now+86400000);long restoredXp=RpgEngine.state(restored).getLong("xp");RpgEngine.merge(restored,"account",RpgEngine.export(d,"account"),now+86400000);
        check(RpgEngine.state(restored).getLong("xp")==restoredXp&&restoredXp==xp,"Reinstalled journal and repeated sync are idempotent");
        check(RpgEngine.state(restored).optBoolean("kr_legacy_seeded"),"Restored migration marker prevents legacy tier re-promotion after valid demotion");
        check(RpgEngine.summary(restored).getJSONObject("KR").getInt("highest")==9,"Best survives restart/export/restore");
        String original=restored.toString();check(!RpgEngine.merge(restored,"other",RpgEngine.export(d,"account"),now+86400000)&&restored.toString().equals(original),"Account switch rejects foreign achievements");
        System.out.println("RPG Step 3 integration: "+checks+" assertions passed; synthetic records only.");
    }
}
