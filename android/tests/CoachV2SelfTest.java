package dev.bennett.codexmeter;

import java.nio.file.*;import java.util.*;import org.json.*;

public final class CoachV2SelfTest {
    static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        JSONObject raw=new JSONObject(Files.readString(Path.of(args[0])));CoachCatalog c=new CoachCatalog(raw);Random random=new Random(7282);
        JSONObject state=c.migrate(new JSONObject().put("id",17).put("recent",new JSONArray().put(17).put(99999)).put("hidden",new JSONArray().put(18)));
        check(state.getString("current").equals(c.legacyIds.get(17)),"frozen numeric mapping");check(state.getJSONArray("recent").length()==1,"unknown IDs discarded only from presentation");
        JSONObject reordered=new JSONObject(raw.toString());JSONArray reverse=new JSONArray(),q=raw.getJSONArray("quotes");for(int i=q.length()-1;i>=0;i--)reverse.put(q.get(i));reordered.put("quotes",reverse);CoachCatalog reorderedCatalog=new CoachCatalog(reordered);
        check(reorderedCatalog.migrate(new JSONObject().put("id",17)).getString("current").equals(state.getString("current")),"reorder cannot remap numeric ID");check(c.migrate(state).toString().equals(state.toString()),"idempotent migration");
        long now=1800000000000L;UsageWindow low=new UsageWindow(2,604800,0,(now+86400000)/1000),shortHigh=new UsageWindow(98,18000,0,(now+3600000)/1000);
        UsageSnapshot s=new UsageSnapshot("unknown",true,false,shortHigh,low,now);
        check(CoachCatalog.group(s,true,false,true,30,now).equals("critical"),"short constraint before low long/burst/bonus");
        check(CoachCatalog.group(s,false,false,true,30,now).equals("stale"),"stale first");check(CoachCatalog.group(s,true,true,true,30,now).equals("network"),"error first");check(CoachCatalog.group(null,true,false,false,0,now).equals("missing"),"missing");
        UsageSnapshot bonus=new UsageSnapshot("free",true,false,new UsageWindow(100,18000,0,(now+3600000)/1000),low,now);check(CoachCatalog.group(bonus,true,false,false,0,now).equals("heavy"),"100 allowed proves neither blocking nor additional consumption");
        UsageSnapshot blocked=new UsageSnapshot("plus",false,true,null,low,now);check(CoachCatalog.group(blocked,true,false,true,0,now).equals("critical"),"authoritative block");
        check(CoachCatalog.group(new UsageSnapshot("unknown",true,false,null,low,now),true,false,false,0,now).equals("missing"),"partial short coverage cannot recommend more work");
        check(c.select(state,"low","off","","one",false,random)==null,"off");check(c.select(state,"low","calm","","one",false,random)==null,"tone never relaxed");
        state=c.migrate(new JSONObject());Set<String> seen=new HashSet<>();Map<String,Integer> frequencies=new TreeMap<>();
        for(int i=0;i<10000;i++){String interest=new String[]{"","explorer","creator","developer","analyst","practitioner"}[i%6];CoachCatalog.Quote selected=c.select(state,"low","spicy",interest,"draw-"+i,true,random);check(selected!=null,"candidate");check(selected.group.equals("low")&&selected.tone.equals("spicy"),"hard constraints");check(state.getJSONArray("recent").length()<=20&&state.getJSONArray("tips").length()<=5,"bounded cooldowns");seen.add(selected.id);frequencies.merge(selected.tipId,1,Integer::sum);}
        check(seen.size()>=60,"quote diversity under fixed seed");CoachCatalog.Quote current=c.select(state,"low","spicy","","fixed",false,random);check(c.select(state,"low","spicy","","fixed",false,random)==current,"stable while reading");
        JSONArray hidden=new JSONArray();for(CoachCatalog.Quote item:c.quotes)hidden.put(item.id);state.put("hidden",hidden);check(c.select(state,"low","spicy","","all-hidden",true,random)==null,"all hidden terminates");
        JSONObject singleRaw=new JSONObject(raw.toString());singleRaw.put("quotes",new JSONArray().put(q.getJSONObject(0))).put("legacyIds",new JSONObject());CoachCatalog single=new CoachCatalog(singleRaw);JSONObject one=single.migrate(new JSONObject());for(int i=0;i<50;i++)check(single.select(one,"zero","spicy","","single-"+i,true,random)!=null,"single candidate soft cooldown");
        for(String group:new String[]{"critical","missing","stale","network"})for(int i=0;i<100;i++){JSONObject st=c.migrate(new JSONObject());CoachCatalog.Quote item=c.select(st,group,"spicy","developer","guard-"+i,true,random);check(item!=null&&item.tipId.isEmpty(),"constrained states never propose a new prompt call");}
        System.out.println("Coach V2: "+checks+" assertions, seed 7282 / 10,000 draws, "+seen.size()+" distinct low-context quotes. Policy simulation only.");
    }
}
