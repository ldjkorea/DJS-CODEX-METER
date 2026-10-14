package dev.bennett.codexmeter;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.json.*;

/** Beta 9.9.5 rules. Successful, fresh Codex usage responses; never task completion or token spending. */
final class BetaV4 {
    static final int RULES=4;
    static final String NAMESPACE="usage-check-v4";
    static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    static LocalDate day(long at){return Instant.ofEpochMilli(at).atZone(ZONE).toLocalDate();}
    static String week(LocalDate d){return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();}
    static int[] targets(int period){return period==0?new int[]{1}:period==1?new int[]{3,5}:new int[]{7,10,14};}
    static int reward(int period,int stage){return period==0?1:period==1?(stage==3?20:10):40;}
    static long cost(int level){if(level<1)throw new IllegalArgumentException("Level");return level<30?8:level<40?40+10L*(level-30):level<50?200+50L*(level-40):700;}
    static long threshold(int level){if(level<1)throw new IllegalArgumentException("Level");long total=0;for(int l=1;l<Math.min(level,50);l++)total=Math.addExact(total,cost(l));return Math.addExact(total,Math.multiplyExact(Math.max(0L,level-50L),700));}
    static JSONObject create(String account,long at)throws JSONException{
        if(account==null||!account.matches("[a-f0-9]{64}")||at<=0)throw new IllegalArgumentException("Identity");
        JSONObject state=new JSONObject().put("rules",RULES).put("namespace",NAMESPACE).put("account",account).put("cutover",at)
            .put("enabled",true).put("days",new JSONObject()).put("equipped","").put("seen_total",0);
        rebuild(state,at);return state;
    }
    static boolean confirm(JSONObject state,String account,long responseAt,long now,boolean freshResponse)throws JSONException{
        validateIdentity(state,account);
        if(!freshResponse||!state.optBoolean("enabled")||responseAt<=state.getLong("cutover")||responseAt>now||now-responseAt>900000)return false;
        String date=day(responseAt).toString();JSONObject days=state.getJSONObject("days");if(days.has(date))return false;
        JSONObject staged=new JSONObject(state.toString());staged.getJSONObject("days").put(date,new JSONObject().put("at",responseAt).put("source","usage_response"));rebuild(staged,now);Iterator<String> fields=staged.keys();while(fields.hasNext()){String key=fields.next();state.put(key,staged.get(key));}return true;
    }
    static void validateIdentity(JSONObject s,String account)throws JSONException{
        int catalog=s.has("catalog_revision")?new java.math.BigDecimal(s.get("catalog_revision").toString()).intValueExact():1;if(catalog<1||catalog>2)throw new IllegalArgumentException("Newer catalog preserved");
        if(new java.math.BigDecimal(s.get("rules").toString()).intValueExact()!=RULES||!NAMESPACE.equals(s.getString("namespace"))||!Objects.equals(account,s.getString("account")))throw new IllegalArgumentException("Rules/account");
    }
    private static void pay(JSONObject rewards,String period,String stage,long value,long at)throws JSONException{
        rewards.put(NAMESPACE+":"+period+":"+stage,new JSONObject().put("xp",value).put("at",at));
    }
    static void rebuild(JSONObject state,long now)throws JSONException{
        validateIdentity(state,state.getString("account"));JSONObject days=state.getJSONObject("days"),weeks=new JSONObject(),months=new JSONObject(),rewards=new JSONObject();
        long cutover=new java.math.BigDecimal(state.get("cutover").toString()).longValueExact();if(cutover<=0||cutover>now||days.length()>40000)throw new IllegalArgumentException("Cutover/size");
        ArrayList<String> dates=new ArrayList<>();days.keys().forEachRemaining(dates::add);Collections.sort(dates);
        long last=0;int weekStages=0;Map<String,Set<String>> monthWeeks=new HashMap<>();
        for(String date:dates){JSONObject row=days.getJSONObject(date);long at=new java.math.BigDecimal(row.get("at").toString()).longValueExact();
            LocalDate d=LocalDate.parse(date);if(at<=cutover||at>now||!day(at).equals(d)||!"usage_response".equals(row.getString("source")))throw new IllegalArgumentException("Invalid response day");
            last=Math.max(last,at);String w=week(d),m=YearMonth.from(d).toString();int wn=weeks.optInt(w)+1,mn=months.optInt(m)+1;
            weeks.put(w,wn);months.put(m,mn);monthWeeks.computeIfAbsent(m,k->new HashSet<>()).add(w);
            pay(rewards,"day:"+date,"confirm",reward(0,1),at);
            if(wn==3){pay(rewards,"week:"+w,"3",reward(1,3),at);weekStages++;}if(wn==5)pay(rewards,"week:"+w,"5",reward(1,5),at);
            if(mn==7||mn==10||mn==14)pay(rewards,"month:"+m,Integer.toString(mn),reward(2,mn),at);
        }
        long total=0;Iterator<String> keys=rewards.keys();while(keys.hasNext())total=Math.addExact(total,rewards.getJSONObject(keys.next()).getLong("xp"));
        int level=1;long progress=total;while(level<50&&progress>=cost(level)){progress-=cost(level);level++;}
        if(level>=50){long extra=progress/700;if(extra>Integer.MAX_VALUE-50L)throw new IllegalArgumentException("Level overflow");level=50+(int)extra;progress%=700;}
        JSONObject honors=state.optJSONObject("honors");if(honors==null)honors=new JSONObject();JSONObject titles=state.optJSONObject("titles");if(titles==null)titles=new JSONObject();
        int gold=0;boolean fourWeeks=false;Iterator<String> mk=months.keys();while(mk.hasNext()){String m=mk.next();if(months.getInt(m)>=14)gold++;if(monthWeeks.get(m).size()>=4)fourWeeks=true;}
        LocalDate first=day(cutover).withDayOfMonth(1).plusMonths(1);if(java.time.temporal.ChronoUnit.YEARS.between(first,day(now))>200)throw new IllegalArgumentException("Clock horizon");int goldYears=0;
        for(LocalDate start=first;!start.plusYears(1).isAfter(day(now));start=start.plusYears(1)){int n=0;for(int i=0;i<12;i++)if(months.optInt(YearMonth.from(start.plusMonths(i)).toString())>=14)n++;if(n>=10)goldYears++;}
        earned(honors,"rpg3.first_day",dates.size()>=1,last);earned(honors,"rpg3.first_week",weekStages>=1,last);earned(honors,"rpg3.month_four_weeks",fourWeeks,last);
        earned(honors,"rpg3.daily_7",dates.size()>=7,last);earned(honors,"rpg3.weekly_4",weekStages>=4,last);earned(honors,"rpg3.month_gold_3",gold>=3,last);earned(honors,"rpg3.month_gold_6",gold>=6,last);
        earned(honors,"rpg3.annual_gold_1",goldYears>=1,last);earned(honors,"rpg3.annual_gold_3",goldYears>=3,last);earned(honors,"rpg3.frame_30",level>=30&&dates.size()>=14,last);earned(honors,"rpg3.frame_50",level>=50&&goldYears>=1,last);
        earned(titles,"rpg3.starter",dates.size()>=1,last);earned(titles,"rpg3.title_rhythm",dates.size()>=7,last);earned(titles,"rpg3.workbench",level>=30&&dates.size()>=14,last);earned(titles,"rpg3.title_orbit",gold>=6,last);earned(titles,"rpg3.title_chronicler",goldYears>=3,last);
        state.put("total",total).put("level",level).put("progress",progress).put("next_cost",cost(level)).put("last_response",last).put("weeks",weeks).put("months",months)
            .put("rewards",rewards).put("honors",honors).put("titles",titles).put("checked_days",dates.size()).put("completed_weeks",weekStages).put("gold_months",gold).put("gold_years",goldYears);
        int perfect=0,bronze=0,silver=0;Iterator<String> weekKeys=weeks.keys();while(weekKeys.hasNext())if(weeks.getInt(weekKeys.next())>=5)perfect++;Iterator<String> monthKeys=months.keys();while(monthKeys.hasNext()){int count=months.getInt(monthKeys.next());if(count>=7)bronze++;if(count>=10)silver++;}
        state.put("perfect_weeks",perfect).put("bronze_months",bronze).put("silver_months",silver);
        if(state.optInt("catalog_revision",2)>=2){expand(state,dates,now);state.put("catalog_revision",2);}
        if(!state.optString("equipped").isEmpty()&&!titles.has(state.getString("equipped")))throw new IllegalArgumentException("Unowned equipment");
    }
    static void expand(JSONObject state,List<String> dates,long now)throws JSONException{
        ArrayList<String[]> pending=new ArrayList<>();for(String[] entry:BetaCatalog.ALL)if(entry.length>7&&!state.getJSONObject(BetaCatalog.title(entry[0])?"titles":"honors").has(entry[0]))pending.add(entry);
        if(pending.isEmpty())return;
        JSONObject counts=new JSONObject();HashMap<String,Integer> weeks=new HashMap<>(),months=new HashMap<>();long xp=0;int level=1,wn=0,pn=0,bn=0,sn=0,gn=0,index=0;
        for(String date:dates){long at=state.getJSONObject("days").getJSONObject(date).getLong("at");LocalDate day=LocalDate.parse(date);String week=week(day),month=date.substring(0,7);int w=weeks.getOrDefault(week,0)+1,m=months.getOrDefault(month,0)+1;weeks.put(week,w);months.put(month,m);xp++;
            if(w==3){wn++;xp+=20;}if(w==5){pn++;xp+=10;}if(m==7){bn++;xp+=40;}if(m==10){sn++;xp+=40;}if(m==14){gn++;xp+=40;}
            while(level<100&&xp>=threshold(level+1))level++;
            counts.put("checked_days",++index).put("completed_weeks",wn).put("perfect_weeks",pn).put("bronze_months",bn).put("silver_months",sn).put("gold_months",gn).put("level",level);
            Iterator<String[]> entries=pending.iterator();while(entries.hasNext()){String[] e=entries.next();if(BetaCatalog.metric(counts,e[7])>=Integer.parseInt(e[8])){state.getJSONObject(BetaCatalog.title(e[0])?"titles":"honors").put(e[0],at);entries.remove();}}
            if(pending.isEmpty())break;
        }
    }
    static JSONObject upgrade(JSONObject input,long now)throws JSONException{validateIdentity(input,input.getString("account"));JSONObject updated=new JSONObject(input.toString());updated.put("catalog_revision",2);rebuild(updated,now);return updated;}
    private static void earned(JSONObject facts,String id,boolean yes,long at)throws JSONException{if(yes&&!facts.has(id))facts.put(id,at);}
    static JSONObject summary(JSONObject state,long now)throws JSONException{
        JSONObject out=new JSONObject();for(String key:new String[]{"rules","account","enabled","cutover","total","level","progress","next_cost","last_response","honors","titles","equipped","checked_days","completed_weeks","gold_months","gold_years"})out.put(key,state.get(key));
        LocalDate d=day(now);return out.put("day",state.getJSONObject("days").has(d.toString())?1:0).put("week",state.getJSONObject("weeks").optInt(week(d))).put("month",state.getJSONObject("months").optInt(YearMonth.from(d).toString())).put("as_of_day",d.toString());
    }
    static JSONObject restore(JSONObject own,JSONObject incoming,String account,long now)throws JSONException{
        validateIdentity(incoming,account);
        Set<String> allowed=new HashSet<>(Arrays.asList("rules","namespace","account","cutover","enabled","days","equipped","seen_total","total","level","progress","next_cost","last_response","weeks","months","rewards","honors","titles","checked_days","completed_weeks","gold_months","gold_years","catalog_revision","perfect_weeks","bronze_months","silver_months"));Iterator<String> inputKeys=incoming.keys();while(inputKeys.hasNext())if(!allowed.contains(inputKeys.next()))throw new IllegalArgumentException("Unknown backup field; authentication excluded");
        int revision=incoming.optInt("catalog_revision",1);if(revision<1||revision>2)throw new IllegalArgumentException("Newer catalog preserved");JSONObject restored=new JSONObject(incoming.toString()).put("catalog_revision",revision);
        // Derived fields and claimed collectibles must exactly match a rebuilt evidence ledger.
        JSONObject suppliedH=restored.getJSONObject("honors"),suppliedT=restored.getJSONObject("titles");long suppliedTotal=new java.math.BigDecimal(restored.get("total").toString()).longValueExact();int suppliedLevel=new java.math.BigDecimal(restored.get("level").toString()).intValueExact();
        restored.put("honors",new JSONObject()).put("titles",new JSONObject());rebuild(restored,now);
        if(suppliedTotal!=restored.getLong("total")||suppliedLevel!=restored.getInt("level")||!sameKeys(suppliedH,restored.getJSONObject("honors"))||!sameKeys(suppliedT,restored.getJSONObject("titles")))throw new IllegalArgumentException("Backup balance/collectibles");
        for(JSONObject facts:new JSONObject[]{suppliedH,suppliedT}){Iterator<String> ids=facts.keys();while(ids.hasNext()){long at=new java.math.BigDecimal(facts.get(ids.next()).toString()).longValueExact();if(at<=restored.getLong("cutover")||at>now)throw new IllegalArgumentException("Award time");}}
        restored.put("honors",new JSONObject(suppliedH.toString())).put("titles",new JSONObject(suppliedT.toString()));
        if(own!=null){validateIdentity(own,account);if(own.getLong("cutover")!=restored.getLong("cutover"))throw new IllegalArgumentException("Different cutover; preserve current record");
            JSONObject days=restored.getJSONObject("days"),ownDays=own.getJSONObject("days");Iterator<String> keys=ownDays.keys();while(keys.hasNext()){String key=keys.next();JSONObject row=ownDays.getJSONObject(key);if(!days.has(key)||row.getLong("at")<days.getJSONObject(key).getLong("at"))days.put(key,new JSONObject(row.toString()));}
            for(String kind:new String[]{"honors","titles"}){JSONObject facts=restored.getJSONObject(kind),original=own.getJSONObject(kind);Iterator<String> ids=original.keys();while(ids.hasNext()){String id=ids.next();facts.put(id,facts.has(id)?Math.min(facts.getLong(id),original.getLong(id)):original.getLong(id));}}
            restored.put("enabled",own.getBoolean("enabled")).put("equipped",own.getString("equipped")).put("seen_total",own.optLong("seen_total"));rebuild(restored,now);
        }return upgrade(restored,now);
    }
    private static boolean sameKeys(JSONObject a,JSONObject b){Set<String> x=new HashSet<>(),y=new HashSet<>();a.keys().forEachRemaining(x::add);b.keys().forEachRemaining(y::add);return x.equals(y);}
}
