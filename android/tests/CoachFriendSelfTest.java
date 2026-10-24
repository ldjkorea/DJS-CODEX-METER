package dev.bennett.codexmeter;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Production voice/evidence boundaries, with synthetic snapshots; no server or billing calls. */
public final class CoachFriendSelfTest {
    static int checks;
    static final long NOW=1800000000000L;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static UsageWindow window(double used,long seconds){return new UsageWindow(used,seconds,0,(NOW+seconds*500)/1000);}
    static UsageSnapshot sample(String plan,double shortUsed,double longUsed){return new UsageSnapshot(plan,true,false,window(shortUsed,18000),window(longUsed,604800),NOW);}
    static String group(UsageSnapshot s){return CoachCatalog.group(s,true,false,false,0,NOW);}
    static String display(CoachCatalog.Quote q,UsageSnapshot s){return CoachCatalog.displayText(q,s,true,false,false,0,NOW);}
    public static void main(String[] args)throws Exception{
        JSONObject raw=new JSONObject(Files.readString(Path.of(args[0]))),medals=new JSONObject(Files.readString(Path.of(args[1])));
        CoachCatalog catalog=new CoachCatalog(raw);List<CoachCatalog.Quote> paid=new ArrayList<>();
        check(raw.getInt("schemaVersion")==2&&raw.getInt("contentVersion")==4,"Schema preserved, content revision incremented");
        check(catalog.quotes.size()==600&&catalog.tips.size()==40&&medals.getJSONArray("quotes").length()==24&&medals.getInt("version")==1,"624 stable quotes / 40 tips");
        Set<String> texts=new HashSet<>();for(CoachCatalog.Quote q:catalog.quotes){check(texts.add(q.text),"Unique base copy: "+q.id);check(q.text.length()<=130&&!q.text.matches(".*[\\r\\n].*"),"Compact single-paragraph copy");if(!q.paidText.isEmpty())paid.add(q);}
        check(paid.size()==4,"Exactly four paid variants with neutral fallbacks");
        check(group(sample("plus",0,0)).equals("zero"),"Both confirmed windows zero");
        for(double positive:new double[]{0.0001,0.1,0.49,0.5,1,25,50,75,90,99,100}){
            check(!group(sample("plus",positive,0)).equals("zero"),"Positive short window never zero "+positive);
            check(!group(sample("plus",0,positive)).equals("zero"),"Precise positive long window never zero "+positive);
        }
        check(group(sample("plus",0,0.1)).equals("tiny"),"Rounded display zero retains actual positive precision");
        check(group(sample("plus",25,0)).equals("moderate"),"Positive five-hour activity chooses active voice");
        check(group(new UsageSnapshot("plus",true,false,null,window(0,604800),NOW)).equals("missing"),"Missing relevant short window");
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),null,NOW)).equals("missing"),"Missing long window");
        check(group(null).equals("missing"),"No snapshot");
        check(CoachCatalog.group(sample("plus",0,0),false,false,false,0,NOW).equals("stale"),"Explicit stale");
        check(CoachCatalog.group(sample("plus",0,0),true,true,false,0,NOW).equals("network"),"Error beats zero");
        UsageSnapshot future=new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),NOW+1);
        check(group(future).equals("stale"),"Future observation");
        UsageSnapshot old=new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),NOW-900001);
        check(group(old).equals("stale"),"Age greater than fifteen minutes");
        UsageWindow expired=new UsageWindow(0,18000,0,NOW/1000);
        check(group(new UsageSnapshot("plus",true,false,expired,window(0,604800),NOW)).equals("stale"),"Short reset boundary stale");
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),expired,NOW)).equals("stale"),"Long reset boundary stale");
        for(boolean allowed:new boolean[]{false,true})for(boolean reached:new boolean[]{false,true})if(!allowed||reached)
            check(CoachCatalog.group(new UsageSnapshot("plus",allowed,reached,window(0,18000),window(0,604800),NOW),true,false,true,25,NOW).equals("critical"),"Blocked flags before bonus/burst");
        check(CoachCatalog.group(sample("plus",95,0),true,false,true,25,NOW).equals("critical"),"Short constraint before bonus/burst");
        check(CoachCatalog.group(sample("plus",0,0),true,false,true,0,NOW).equals("bonus"),"Confirmed bonus precedence");
        check(CoachCatalog.group(sample("plus",0,0),true,false,false,13,NOW).equals("burst"),"Positive daily burst precedence");
        check(CoachCatalog.group(sample("plus",0,0),true,false,false,0.1,NOW).equals("tiny"),"Other confirmed positive daily activity blocks zero joke");
        UsageWindow reset=new UsageWindow(0,604800,0,(NOW+604800000L-3600000)/1000),imminent=new UsageWindow(0,604800,0,(NOW+3600000)/1000);
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),reset,NOW)).equals("reset"),"New period priority");
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),imminent,NOW)).equals("imminent"),"Imminent reset priority");
        check(group(new UsageSnapshot("free",true,false,null,null,window(0,2592000),Collections.emptyList(),null,-1,NOW)).equals("zero"),"Free monthly complete coverage needs no invented five-hour quota");
        for(String plan:new String[]{"plus","unknown"})check(group(new UsageSnapshot(plan,true,false,null,null,window(0,2592000),Collections.emptyList(),null,-1,NOW)).equals("missing"),"Monthly-only non-Free cannot prove complete zero "+plan);
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),expired,Collections.emptyList(),null,-1,NOW)).equals("stale"),"Expired other monthly quota blocks zero");
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),window(3,2592000),Collections.emptyList(),null,-1,NOW)).equals("tiny"),"Other observed monthly use blocks zero");
        for(double used:new double[]{0,0.01,5,100}){
            UsageLimit extra=new UsageLimit("review","Review","review",true,false,window(used,18000),null);
            UsageSnapshot s=new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),List.of(extra),null,-1,NOW);
            check(group(s).equals(used==0?"zero":"missing"),"Independent meter never folded into invented zero/percentage "+used);
        }
        UsageLimit unknown=new UsageLimit("unknown","Unknown","unknown",true,false,null,null);
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),List.of(unknown),null,-1,NOW)).equals("missing"),"Incomplete independent quota");
        UsageLimit outdated=new UsageLimit("old","Old","old",true,false,expired,null);
        check(group(new UsageSnapshot("plus",true,false,window(0,18000),window(0,604800),List.of(outdated),null,-1,NOW)).equals("stale"),"Expired independent quota");
        for(CoachCatalog.Quote q:paid){double used=q.group.equals("zero")?0:1;
            for(String plan:new String[]{"plus","pro","pro_20x","pro_5x","PRO-LITE"}){
                String text=display(q,sample(plan,used,used));check(!text.equals(q.text)&&!text.contains("{plan}"),"Verified paid entitlement variant: "+plan);
                if(q.paidText.contains("{plan}"))check(text.contains(plan.equals("plus")?"플러스":plan.contains("5")||plan.equals("PRO-LITE")?"프로 라이트":"프로"),"Correct verified label");
            }
            for(String plan:new String[]{"free","go","unknown","","team","enterprise","pro500","not-pro","plus<script>"})check(display(q,sample(plan,used,used)).equals(q.text),"Unknown/Free never invents payment "+plan);
            check(CoachCatalog.displayText(q,sample("plus",used,used),false,false,false,0,NOW).equals(q.text),"No paid joke on stale data");
            check(CoachCatalog.displayText(q,sample("plus",used,used),true,true,false,0,NOW).equals(q.text),"No paid joke on lookup error");
            check(display(q,sample("plus",50,50)).equals(q.text),"Different activity group never gets low/zero paid joke");
            check(display(q,null).equals(q.text),"No account evidence");
            if(q.group.equals("zero"))check(display(q,sample("plus",0.1,0)).equals(q.text),"Paid zero text also rejects short positive precision");
        }
        JSONObject summary=new JSONObject().put("date",BetaV4.day(NOW).toString()).put("today",new JSONObject().put("active",true).put("grade",3));
        check(BetaMedalCoach.group(summary,"zero",NOW).equals("medal_gold"),"Confirmed medal precedes zero joke");
        for(String guard:new String[]{"critical","missing","network","stale"})check(BetaMedalCoach.group(summary,guard,NOW).equals(guard),"Medal cannot override guard "+guard);
        summary.getJSONObject("today").put("uncertain",true);check(BetaMedalCoach.group(summary,"zero",NOW).equals("zero"),"Uncertain medal cannot claim activity");
        System.out.println("Friend voice: "+checks+" assertions passed; real production selector/display/evidence code, synthetic observations, no billing or OAuth proof.");
    }
}
