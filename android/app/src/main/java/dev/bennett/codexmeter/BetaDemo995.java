package dev.bennett.codexmeter;
import android.content.Context;
import java.time.*;
final class BetaDemo995 {
    static void prepare(Context c)throws Exception{
        if(!TrialDemo.active(c))throw new IllegalArgumentException("Synthetic demo only");String account=SubscriptionStore.accountKey(c);
        if(BetaGrowthStore.load(c,account)!=null)return;
        long now=System.currentTimeMillis();LocalDate first=BetaV4.day(now).minusDays(29);long cutover=first.atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli();
        BetaGrowthStore.start(c,account,cutover,true);
        for(int i=0;i<30;i++){LocalDate d=first.plusDays(i);if(d.getDayOfWeek().getValue()>5)continue;long at=d.atStartOfDay(BetaV4.ZONE).toInstant().toEpochMilli()+3600000;if(at<=now)BetaGrowthStore.confirm(c,account,at,now,true);}
        org.json.JSONObject state=BetaGrowthStore.load(c,account);if(state.getJSONObject("titles").has("rpg3.workbench"))BetaGrowthStore.equip(c,account,"rpg3.workbench");
    }
}
