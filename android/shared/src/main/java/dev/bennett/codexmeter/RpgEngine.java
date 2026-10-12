package dev.bennett.codexmeter;

import java.time.*;
import java.util.*;
import org.json.*;

/** Account-owned, monotone award targets. UI reads summary only; reconciliation runs in its worker. */
public final class RpgEngine {
    public static final int VERSION=1;
    public static final String MODEL="rpg-2026q2-v1";
    public static final long[] TOKEN_MILESTONES={1_000_000L,10_000_000L,100_000_000L,1_000_000_000L,10_000_000_000L};
    public static final long[] MILESTONE_XP={25,75,250,1000,1500};
    private RpgEngine(){}
    public static JSONObject state(JSONObject document)throws JSONException {
        JSONObject future=document.getJSONObject("future"),state=future.optJSONObject("rpg");if(state!=null){if(state.getInt("version")!=VERSION)throw new IllegalStateException("Preserve newer RPG");return state;}
        state=new JSONObject().put("version",VERSION).put("model",MODEL).put("xp",0).put("awards",new JSONObject()).put("quota_days",new JSONObject()).put("seen",new JSONObject())
            .put("tokens",RpgTokens.empty()).put("activity",new JSONObject()).put("weeks",new JSONObject()).put("achievements",new JSONObject()).put("KR",RpgTier.empty("KR")).put("WORLD",RpgTier.empty("WORLD"));
        future.put("rpg",state);return state;
    }
    private static boolean award(JSONObject state,String id,long target)throws JSONException {
        if(target<0)throw new IllegalArgumentException("XP target");JSONObject awards=state.getJSONObject("awards");long old=awards.optLong(id);if(target<=old)return false;
        state.put("xp",Math.addExact(state.getLong("xp"),target-old));awards.put(id,target);return true;
    }
    public static int level(long xp){if(xp<0)throw new IllegalArgumentException("XP");return 1+(int)Math.min(Integer.MAX_VALUE-1,Math.floor((Math.sqrt(1+8*(xp/100d))-1)/2));}
    public static long threshold(int level){if(level<1)throw new IllegalArgumentException("Level");return Math.multiplyExact(50L,Math.multiplyExact((long)level,level-1L));}
    public static long tokenXp(long tokens){return tokens<=0?0:Math.min(75,(long)Math.floor(15*Math.log10(1+tokens/1000d)));}
    public static long quotaXp(double points){return points<=0?0:Math.min(25,(long)Math.floor(25*Math.log1p(points)/Math.log(101)));}
    public static void reconcile(JSONObject document,List<LedgerRecord> records,List<LedgerAggregation.Day> daily,List<Long> credits,List<FunInsights.Window> weeks,long now,boolean fresh)throws JSONException {
        JSONObject state=state(document),seen=state.getJSONObject("seen"),activity=state.getJSONObject("activity"),quota=state.getJSONObject("quota_days");
        Set<String> dirty=new HashSet<>();boolean changed=AchievementJourney.capture(document,records,daily,credits,weeks,now);changed|=TitleJourney.capture(document,records,daily,credits,weeks,now);long floor=document.optLong("floor");
        List<LedgerRecord> rows=new ArrayList<>(records);rows.sort(Comparator.comparing((LedgerRecord r)->r.meter).thenComparingLong(r->r.at));
        String observedPolicy="";long observedPolicyStart=0;for(LedgerRecord r:rows)if(r.meter.equals("weekly")&&r.at>floor&&r.at<=now&&!r.source.equals("legacy")){if(!r.policy().equals(observedPolicy)){observedPolicy=r.policy();observedPolicyStart=r.at;}}
        long observed=0;for(LedgerRecord r:rows)if(r.at<=now&&r.at>floor&&r.meter.equals("weekly"))observed=Math.max(observed,r.at);state.put("quota_observed_at",observed).put("quota_fresh",fresh);
        if(!observedPolicy.isEmpty()&&!observedPolicy.equals(state.optString("current_policy"))){state.put("current_policy",observedPolicy).put("policy_changed_at",observedPolicyStart);}
        for(LedgerRecord r:rows)if(r.at>floor&&r.at<=now&&!"legacy".equals(r.source)){String key=r.meter+"@"+r.at;if(!seen.has(key)){seen.put(key,true);dirty.add(LedgerAggregation.day(r.at).toString());}}
        // Recompute only affected days (including successor) so late historical inserts are safe.
        Set<String> originallyDirty=new HashSet<>(dirty);
        for(int i=0;i+1<rows.size();i++)if(rows.get(i).meter.equals(rows.get(i+1).meter)&&originallyDirty.contains(LedgerAggregation.day(rows.get(i).at).toString()))dirty.add(LedgerAggregation.day(rows.get(i+1).at).toString());
        Map<String,Double> measured=new HashMap<>();long examined=0;
        for(int i=1;i<rows.size();i++){LedgerRecord a=rows.get(i-1),b=rows.get(i);String date=LedgerAggregation.day(b.at).toString();if(!dirty.contains(date)||!a.meter.equals(b.meter)||a.at<=floor||b.at>now||"legacy".equals(a.source)||"legacy".equals(b.source))continue;examined++;
            if(LedgerAggregation.classify(a,b,credits)==LedgerAggregation.Change.NORMAL&&LedgerAggregation.day(a.at).equals(LedgerAggregation.day(b.at))&&b.used>a.used){
                if(!activity.has(date)){activity.put(date,b.at);changed=true;}
                if(Instant.ofEpochMilli(b.at).atZone(LedgerAggregation.ZONE).getHour()<6)state.put("dawn",true);
                if(b.meter.equals("weekly")||b.meter.equals("monthly"))measured.merge(b.policy()+"@"+date,b.used-a.used,Double::sum);
            }
        }
        for(LedgerAggregation.Day d:daily)if(!d.legacy&&d.first>floor&&d.last<=now&&d.points>0){if(!activity.has(d.date)){activity.put(d.date,d.last);changed=true;}if(d.policy.startsWith("weekly|")||d.policy.startsWith("monthly|"))measured.merge(d.policy+"@"+d.date,d.points,Math::max);}
        for(Map.Entry<String,Double> e:measured.entrySet())if(e.getValue()>quota.optDouble(e.getKey())){quota.put(e.getKey(),e.getValue());String date=e.getKey().substring(e.getKey().lastIndexOf('@')+1);changed|=award(state,"quota-day:"+date,quotaXp(e.getValue()));}
        JSONObject valid=state.getJSONObject("weeks");for(FunInsights.Window w:weeks)if(w.span.end<=now&&w.span.end>floor){String key=Long.toString(w.span.end);if(!valid.has(key)){valid.put(key,new JSONObject().put("start",w.span.start).put("end",w.span.end).put("policy",w.policy).put("used",w.used.toPlainString()));changed|=award(state,"valid-week:"+key,50);}}
        JSONObject heritage=document.getJSONObject("heritage"),imported=state.optJSONObject("heritage_seen");if(imported==null){imported=new JSONObject();state.put("heritage_seen",imported);}
        for(String id:AccountProgress.keys(heritage))if(!imported.has(id)){imported.put(id,true);JSONObject fun=heritage.getJSONObject(id).optJSONObject("fun");JSONArray archived=fun==null?null:fun.optJSONArray("settlements");if(archived==null)continue;
            for(int i=0;i<archived.length();i++){JSONObject w=archived.optJSONObject(i);if(w==null||!"near_boundary_observations_no_interpolation".equals(w.optString("quality"))||w.optInt("observations")<3)continue;
                long start=w.optLong("start"),end=w.optLong("end");String oldId=w.optString("id");int split=oldId.lastIndexOf('@');if(split<0)continue;String policy=oldId.substring(0,split),key=Long.toString(end);
                if(!policy.matches("weekly\\|[a-z0-9_-]{1,40}\\|604800")||!oldId.substring(split+1).equals(key)||end-start!=TierEvolution.WEEK||start<=floor||end>now||valid.has(key))continue;
                double used=w.optDouble("used",-1);if(!Double.isFinite(used)||used<0||used>100)continue;
                valid.put(key,new JSONObject().put("start",start).put("end",end).put("policy",policy).put("used",w.getString("used")).put("source","archived_certified_week"));changed|=award(state,"valid-week:"+key,50);
            }
        }
        String stamp=document.getJSONObject("settlements").toString().hashCode()+":"+document.getJSONObject("tier").optInt("highest",-1)+":"+LedgerAggregation.day(now)+":"+RpgCatalog.REVISION;
        if(!changed&&dirty.isEmpty()&&stamp.equals(state.optString("input_stamp"))){evaluateAvailableWindows(document,now,fresh);return;}
        state.put("input_stamp",stamp).put("pairs_last_update",examined).put("migration","confirmed_lower_bound_no_fabricated_days").put("demotion_status","held_until_comparable_token_calendar");
        refresh(document,now,fresh,changed);
        evaluateAvailableWindows(document,now,fresh);
    }
    public static boolean ingestOfficial(JSONObject document,String account,JSONObject envelope,long now)throws JSONException {
        JSONObject state=state(document);if(!RpgTokens.official(state.getJSONObject("tokens"),account,envelope,now))return false;
        JSONArray rows=envelope.optJSONArray("local_rows");if(rows!=null){if(rows.length()>24)throw new IllegalArgumentException("Local token metadata bound");for(int i=0;i<rows.length();i++)RpgTokens.local(state.getJSONObject("tokens"),account,rows.getJSONObject(i),now);}
        refresh(document,now,false,true);evaluateAvailableWindows(document,now,true);return true;
    }
    public static boolean ingestLocal(JSONObject document,String account,JSONObject fact,long now)throws JSONException {
        JSONObject state=state(document);if(!RpgTokens.local(state.getJSONObject("tokens"),account,fact,now))return false;
        // No fake daily activity from a session-wide counter or 'last response'.
        refresh(document,now,false,true);return true;
    }
    /** Complete timestamp-verified account calendar from a trusted collector.
     * Observed service startDate buckets do NOT satisfy this contract.
     * Absence is unknown, never an implicit zero. */
    public static boolean evaluateCertifiedWindow(JSONObject document,String policy,long end,
            Map<LocalDate,Long> seoulTokens,boolean completeAccountScope,boolean fresh,long now)throws JSONException {
        if(!completeAccountScope||!fresh||end>now)return false;JSONObject s=state(document),week=s.getJSONObject("weeks").optJSONObject(Long.toString(end));
        if(week==null||!policy.equals(week.getString("policy"))||week.getLong("end")-week.getLong("start")!=TierEvolution.WEEK)return false;
        // Discard boundary calendar days rather than interpolate them; require explicit values.
        LocalDate until=LedgerAggregation.day(end),from=until.minusDays(30);long tokens=0;Set<String> days=new HashSet<>();
        for(LocalDate date=from;date.isBefore(until);date=date.plusDays(1)){Long value=seoulTokens.get(date);if(value==null||value<0)return false;tokens=Math.addExact(tokens,value);if(value>0)days.add(date.toString());}
        int active=Math.max(days.size(),inRange(new HashSet<>(AccountProgress.keys(s.getJSONObject("activity"))),from,until)),count=0;
        for(String id:AccountProgress.keys(s.getJSONObject("weeks"))){long at=Long.parseLong(id);if(at<=end&&at>end-8*TierEvolution.WEEK&&policy.equals(s.getJSONObject("weeks").getJSONObject(id).getString("policy")))count++;}
        if(count<4)return false;
        for(String region:new String[]{"KR","WORLD"})RpgTier.week(s.getJSONObject(region),policy,end,RpgTier.score(region,active,count,tokens,week.getDouble("used"),true),true);
        s.put("demotion_status","comparable_window_evaluated");return true;
    }
    /** Called by production snapshot reconciliation and account token ingestion, never by UI. */
    private static void evaluateAvailableWindows(JSONObject document,long now,boolean fresh)throws JSONException {
        JSONObject s=state(document),official=s.getJSONObject("tokens").getJSONObject("official");String closed="";for(String k:sortedEnds(s.getJSONObject("weeks")))if(Long.parseLong(k)<=now)closed=k;boolean quotaCurrent=s.optBoolean("quota_fresh")&&s.optLong("quota_observed_at")>0&&now-s.optLong("quota_observed_at")<=900000L,tokenCurrent=official.optLong("at")>0&&now-official.optLong("at")<=86400000L;String input=official.optLong("at")+":"+s.optLong("quota_observed_at")+":"+fresh+":"+quotaCurrent+":"+tokenCurrent+":"+s.optString("current_policy")+":"+closed;if(input.equals(s.optString("demotion_input")))return;s.put("demotion_input",input).put("demotion_checked_at",now);
        if(!fresh||!s.optBoolean("quota_fresh")||s.optLong("quota_observed_at")<=0||now-s.optLong("quota_observed_at")>900000L||official.optLong("at")<=0||now-official.optLong("at")>86400000L){s.put("demotion_status","held_stale_or_failed_observation");return;}
        JSONObject proof=official.optJSONObject("calendar_certification");
        if(proof==null||!"Asia/Seoul".equals(proof.optString("zone"))||!"account".equals(proof.optString("scope"))||!proof.optBoolean("complete")){s.put("demotion_status","held_unverified_timezone_or_account_coverage");return;}
        Map<LocalDate,Long> calendar=new HashMap<>();JSONObject days=official.getJSONObject("certified_days");for(String day:AccountProgress.keys(days))calendar.put(LocalDate.parse(day),days.getLong(day));
        boolean evaluated=false;for(String key:sortedEnds(s.getJSONObject("weeks"))){JSONObject week=s.getJSONObject("weeks").getJSONObject(key);long end=week.getLong("end");
            // Old archives/restored years and incomplete/early windows never become current penalties.
            if(end<=now-TierEvolution.WEEK||end>now||!week.getString("policy").equals(s.optString("current_policy"))||end<=s.optLong("policy_changed_at")+2*TierEvolution.WEEK)continue;
            LocalDate until=LedgerAggregation.day(end),from=until.minusDays(30);
            if(LocalDate.parse(proof.getString("start")).isAfter(from)||LocalDate.parse(proof.getString("end")).isBefore(until))continue;
            evaluated|=evaluateCertifiedWindow(document,week.getString("policy"),end,calendar,true,true,now);
        }
        if(!evaluated)s.put("demotion_status","held_missing_comparable_completed_window");
    }
    private static int streak(Set<String> dates){List<String> sorted=new ArrayList<>(dates);Collections.sort(sorted);int best=0,n=0;LocalDate prior=null;for(String date:sorted){LocalDate d=LocalDate.parse(date);n=prior!=null&&prior.plusDays(1).equals(d)?n+1:1;best=Math.max(best,n);prior=d;}return best;}
    private static int inRange(Set<String> dates,LocalDate start,LocalDate end){int n=0;for(String s:dates){LocalDate d=LocalDate.parse(s);if(!d.isBefore(start)&&d.isBefore(end))n++;}return n;}
    private static Set<String> tokenDates(JSONObject days)throws JSONException {Set<String> set=new HashSet<>();for(String day:AccountProgress.keys(days))if(days.getLong(day)>0)set.add(day);return set;}
    private static void refresh(JSONObject document,long now,boolean fresh,boolean changed)throws JSONException {
        JSONObject s=state(document);JSONObject inherited=document.getJSONObject("tier");
        if(!s.optBoolean("kr_legacy_seeded")&&inherited.optInt("tier",-1)>=0){AccountTier.promote(s.getJSONObject("KR"),inherited.getInt("tier"),now);s.getJSONObject("KR").put("highest",Math.max(s.getJSONObject("KR").getInt("highest"),inherited.optInt("highest",-1)));s.put("kr_legacy_seeded",true);}
        JSONObject tokens=s.getJSONObject("tokens"),official=tokens.getJSONObject("official"),od=official.optJSONObject("days");if(od==null)od=new JSONObject();
        Set<String> dates=new HashSet<>(AccountProgress.keys(s.getJSONObject("activity"))),service=tokenDates(od);
        // Unknown service timezone: keep the populations separate; max is a conservative bound, never union them.
        int days=Math.max(s.optInt("activity_days"),Math.max(dates.size(),service.size())),longest=Math.max(s.optInt("longest_observed_streak"),Math.max(streak(dates),streak(service)));
        changed|=award(s,"activity-day-count",Math.multiplyExact(20L,days));
        for(String day:service)changed|=award(s,"token-day:"+day,tokenXp(od.getLong(day)));
        int valid=Math.max(s.optInt("valid_weeks"),s.getJSONObject("weeks").length());Set<String> plans=new HashSet<>();double util=0;Double recentUtil=null;long latestStart=0;
        JSONObject settlements=document.getJSONObject("settlements");for(String id:AccountProgress.keys(settlements)){JSONObject recap=settlements.getJSONObject(id),bill=recap.getJSONObject("bill");plans.add(bill.getString("plan"));JSONObject buckets=recap.getJSONObject("buckets");
            if(recap.optBoolean("mixed_policies")||buckets.length()!=1)continue;
            JSONObject b=buckets.getJSONObject(AccountProgress.keys(buckets).get(0));double p=b.getDouble("percent");util=Math.max(util,p);
            long start=LocalDate.parse(bill.getString("start")).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli(),end=LocalDate.parse(bill.getString("end")).atStartOfDay(LedgerAggregation.ZONE).toInstant().toEpochMilli();
            if(start<=now&&now<end&&start>=latestStart){latestStart=start;recentUtil=p;}
        }
        for(String key:AccountProgress.keys(s.getJSONObject("quota_days")))plans.add(key.split("\\|")[1]);
        String quotaBasis=recentUtil==null?"unavailable":"actual_billing_period_utilization";
        if(recentUtil==null){List<String> ends=sortedEnds(s.getJSONObject("weeks"));double sum=0;int count=0;
            for(int i=ends.size()-1;i>=0&&count<8;i--){long at=Long.parseLong(ends.get(i));if(at>now||at<=now-8*TierEvolution.WEEK)continue;JSONObject week=s.getJSONObject("weeks").getJSONObject(ends.get(i));sum+=week.getDouble("used");count++;}
            if(count>=4){recentUtil=sum/count;quotaBasis="certified_week_scores; billing_period_unknown";}
        }
        s.put("quota_score_basis",quotaBasis);
        LocalDate end=LedgerAggregation.day(now).plusDays(1),start=end.minusDays(30);int recentDays=Math.max(inRange(dates,start,end),inRange(service,start,end));long monthlyTokens=0;for(String d:service)if(!LocalDate.parse(d).isBefore(start)&&LocalDate.parse(d).isBefore(end))monthlyTokens=Math.addExact(monthlyTokens,od.getLong(d));
        int recentTokenDays=0;for(String day:AccountProgress.keys(od))if(!LocalDate.parse(day).isBefore(start)&&LocalDate.parse(day).isBefore(end))recentTokenDays++;
        int recentWeeks=0;for(String key:AccountProgress.keys(s.getJSONObject("weeks")))if(Long.parseLong(key)<=now&&Long.parseLong(key)>now-8*TierEvolution.WEEK)recentWeeks++;
        boolean known=RpgTokens.known(tokens),tokenFresh=official.optLong("at")>0&&now-official.optLong("at")<=86400000L;
        boolean comparable=recentWeeks>=4&&recentUtil!=null&&((fresh&&inRange(dates,start,end)>=7)||(tokenFresh&&recentTokenDays>=14));
        s.put("promotion_evidence",comparable?"comparable_observed_activity_and_quota":"held_until_four_valid_weeks_and_recent_activity");
        for(String region:new String[]{"KR","WORLD"}){
            JSONObject tier=s.getJSONObject(region);double score=RpgTier.score(region,recentDays,recentWeeks,monthlyTokens,recentUtil,known&&official.has("lifetimeTokens")&&recentTokenDays>0);
            // A lifetime-only counter cannot certify current intensity or justify a high placement.
            RpgTier.current(tier,score,now,comparable,changed&&(recentDays>0));

        }
        int peak=document.getJSONObject("tier").optInt("highest",-1);for(String region:new String[]{"KR","WORLD"})peak=Math.max(peak,s.getJSONObject(region).optInt("highest",-1));
        int weekend=0;for(String day:dates)if(LocalDate.parse(day).getDayOfWeek().getValue()>=6)weekend++;
        Map<String,Long> metrics=new HashMap<>();metrics.put("tokens",RpgTokens.milestoneTotal(tokens));metrics.put("days",(long)days);metrics.put("streak",(long)longest);metrics.put("weeks",(long)valid);metrics.put("tier",(long)peak+1);metrics.put("utilization",(long)Math.floor(util));metrics.put("plans",(long)plans.size());metrics.put("dawn",s.optBoolean("dawn")?1L:0L);metrics.put("weekend",(long)weekend);
        // 'heritage' requires actual prior evidence, not merely an empty imported plan entry.
        metrics.put("heritage",peak>=0||days>0||valid>0?1L:0L);
        AchievementJourney.tokens(s,now);int achievementDays=days,achievementWeeks=valid;boolean optIn=AchievementJourney.journey(s)!=null;
        if(optIn){metrics=AchievementJourney.metrics(s,fresh);achievementDays=metrics.get("days").intValue();achievementWeeks=metrics.get("weeks").intValue();}
        JSONObject achievements=s.getJSONObject("achievements");for(RpgCatalog.Entry e:RpgCatalog.ALL){long progress=metrics.get(e.metric);JSONObject a=achievements.optJSONObject(e.id);if(a==null){a=new JSONObject().put("id",e.id).put("name_ko",e.ko).put("name_en",e.en).put("description_ko",e.descriptionKo()).put("description_en",e.descriptionEn()).put("condition",e.metric+">="+e.target+";days>="+e.days+";weeks>="+e.weeks).put("rarity",e.rarity).put("reward_xp",e.bonus).put("unlocked",false);achievements.put(e.id,a);}
            a.put("progress",Math.max(a.optLong("progress"),progress)).put("target",e.target).put("required_days",e.days).put("required_weeks",e.weeks);
            a.put("progress_days",Math.max(a.optInt("progress_days"),achievementDays)).put("progress_weeks",Math.max(a.optInt("progress_weeks"),achievementWeeks));
            double ratio=Math.min(1,progress/(double)e.target);if(e.days>0)ratio=Math.min(ratio,achievementDays/(double)e.days);if(e.weeks>0)ratio=Math.min(ratio,achievementWeeks/(double)e.weeks);a.put("progress_ratio",Math.max(a.optDouble("progress_ratio",0),ratio));
            if(!a.getBoolean("unlocked")&&(!optIn||AchievementJourney.enabled(s))&&CollectionTierGoals.allowed(s,e,metrics)&&progress>=e.target&&achievementDays>=e.days&&achievementWeeks>=e.weeks){a.put("unlocked",true).put("first_earned_at",now).put("time_basis","first_verified_by_rpg_engine").put("data_basis",e.metric.equals("tokens")?(official.has("lifetimeTokens")?"official_backend_activity":"verified_local_processed_lower_bound"):"confirmed_account_history").put("rule_version",VERSION);changed|=award(s,"achievement:"+e.id,e.bonus);}
        }
        s.put("activity_days",days).put("active_days_30",recentDays).put("valid_weeks",valid).put("longest_observed_streak",longest).put("token_status",known?"observed":"unknown").put("token_total",known?RpgTokens.milestoneTotal(tokens):JSONObject.NULL)
            .put("token_basis",official.has("lifetimeTokens")?"official_backend_activity":"local_processed_lower_bound").put("token_observed_at",official.optLong("at")).put("daily_basis","Seoul ledger / service dates retained separately; max, not sum").put("updated_at",now).put("level",level(s.getLong("xp")));
        long lifetime=RpgTokens.milestoneTotal(tokens);for(int i=0;i<TOKEN_MILESTONES.length;i++)if(lifetime>=TOKEN_MILESTONES[i])award(s,"milestone-v25:tokens:"+TOKEN_MILESTONES[i],MILESTONE_XP[i]);
        s.put("level",level(s.getLong("xp")));if(TitleJourney.journey(s)==null)RpgTitleCatalog.reconcile(s,now);else TitleJourney.reconcile(s,now);
        if(s.optInt("collection_revision")<CollectionRestart.REVISION&&!s.has("recommended_title"))recommend(document,new Random());
    }
    private static List<String> sortedEnds(JSONObject weeks){List<String> keys=AccountProgress.keys(weeks);keys.sort(Comparator.comparingLong(Long::parseLong));return keys;}
    public static List<String> eligibleTitles(JSONObject document)throws JSONException {return RpgTitleCatalog.owned(state(document));}
    public static String recommend(JSONObject document,Random random)throws JSONException {List<String> eligible=eligibleTitles(document);if(eligible.isEmpty())return "";String id=eligible.get(random.nextInt(eligible.size()));state(document).put("recommended_title",id);return id;}
    public static boolean equip(JSONObject document,String id)throws JSONException {if(!id.isEmpty()&&!eligibleTitles(document).contains(id))return false;JSONObject s=state(document);s.put("title_selection_set",true);if(id.isEmpty())s.remove("equipped_title");else s.put("equipped_title",id);return true;}
    /** Merge only an account-bound trusted journal (backup/paired device), never add its XP total.
     * Step 3 may expose this through existing authenticated sync. It is not a public upload endpoint. */
    public static JSONObject export(JSONObject document,String account)throws JSONException {
        return new JSONObject().put("account",account).put("version",VERSION).put("rpg",new JSONObject(state(document).toString()));
    }
    public static boolean merge(JSONObject document,String account,JSONObject journal,long now)throws JSONException {
        if(journal==null||!account.equals(journal.optString("account"))||journal.optInt("version")!=VERSION)return false;
        JSONObject staged=new JSONObject(document.toString());mergeInto(staged,account,journal,now);
        document.getJSONObject("future").put("rpg",staged.getJSONObject("future").getJSONObject("rpg"));return true;
    }
    private static void mergeInto(JSONObject document,String account,JSONObject journal,long now)throws JSONException {
        JSONObject imported=journal.getJSONObject("rpg");if(imported.getInt("version")!=VERSION)throw new IllegalArgumentException("RPG journal version");
        AchievementJourney.validate(imported,now);CollectionRestart.mergeScopes(state(document),imported);TitleJourney.merge(state(document),imported,now);
        JSONObject s=state(document),awards=imported.getJSONObject("awards");JSONObject ownJourney=AchievementJourney.journey(s),incomingJourney=AchievementJourney.journey(imported);
        if(ownJourney==null&&incomingJourney!=null)s.put(AchievementJourney.KEY,new JSONObject(incomingJourney.toString()));
        if(incomingJourney==null&&AchievementJourney.journey(s)!=null){JSONObject legacy=AchievementJourney.journey(s).getJSONObject("legacy").getJSONObject("achievements");for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject f=imported.getJSONObject("achievements").optJSONObject(e.id);if(f!=null&&f.optBoolean("unlocked")){long at=f.optLong("first_earned_at");if(at<=0||at>now)throw new IllegalArgumentException("Legacy medal time");JSONObject old=legacy.optJSONObject(e.id);if(old==null||!old.optBoolean("unlocked")||at<old.optLong("first_earned_at"))legacy.put(e.id,new JSONObject(f.toString()));}}}
        boolean mergeMedals=AchievementJourney.journey(s)==null||AchievementJourney.same(s,imported);AchievementJourney.merge(s,imported);Map<String,RpgCatalog.Entry> definitions=new HashMap<>();for(RpgCatalog.Entry e:RpgCatalog.ALL)definitions.put(e.id,e);
        // Reject malformed/imported arbitrary XP events before touching the document.
        for(String key:AccountProgress.keys(awards)){long n=awards.getLong(key),max;
            if(key.equals("activity-day-count"))max=Math.multiplyExact(20L,imported.optInt("activity_days"));
            else if(key.matches("token-day:\\d{4}-\\d{2}-\\d{2}")){LocalDate.parse(key.substring(10));max=75;}
            else if(key.matches("quota-day:\\d{4}-\\d{2}-\\d{2}")){LocalDate.parse(key.substring(10));max=25;}
            else if(key.matches("valid-week:\\d+"))max=50;
            else if(key.startsWith("milestone-v25:tokens:")){max=-1;for(int i=0;i<TOKEN_MILESTONES.length;i++)if(key.equals("milestone-v25:tokens:"+TOKEN_MILESTONES[i])&&RpgTokens.milestoneTotal(imported.getJSONObject("tokens"))>=TOKEN_MILESTONES[i])max=MILESTONE_XP[i];if(max<0)throw new IllegalArgumentException("Milestone evidence");}
            else if(key.startsWith("achievement:")&&definitions.containsKey(key.substring(12)))max=definitions.get(key.substring(12)).bonus;
            else throw new IllegalArgumentException("Unrecognized RPG award");
            if(n<0||n>max)throw new IllegalArgumentException("Invalid RPG reward target");
        }
        for(String key:AccountProgress.keys(awards))award(s,key,awards.getLong(key));
        JSONObject dates=imported.getJSONObject("activity");for(String date:AccountProgress.keys(dates)){LocalDate.parse(date);long at=dates.getLong(date);if(at<=0||at>now)throw new IllegalArgumentException("Activity timestamp");if(!s.getJSONObject("activity").has(date))s.getJSONObject("activity").put(date,at);}
        JSONObject quota=imported.getJSONObject("quota_days");for(String key:AccountProgress.keys(quota)){double points=quota.getDouble(key);if(!Double.isFinite(points)||points<0||!key.matches("(?:weekly|monthly)\\|[a-z0-9_-]{1,40}\\|[0-9]+@\\d{4}-\\d{2}-\\d{2}"))throw new IllegalArgumentException("Quota fact");s.getJSONObject("quota_days").put(key,Math.max(points,s.getJSONObject("quota_days").optDouble(key)));}
        JSONObject importedWeeks=imported.getJSONObject("weeks");for(String key:AccountProgress.keys(importedWeeks)){JSONObject w=importedWeeks.getJSONObject(key);if(w.getLong("end")!=Long.parseLong(key)||w.getLong("end")-w.getLong("start")!=TierEvolution.WEEK||w.getLong("end")>now||!w.getString("policy").startsWith("weekly|"))throw new IllegalArgumentException("Week fact");if(!s.getJSONObject("weeks").has(key))s.getJSONObject("weeks").put(key,new JSONObject().put("start",w.getLong("start")).put("end",w.getLong("end")).put("policy",w.getString("policy")).put("used",w.getString("used")));}
        RpgTokens.mergeJournal(s.getJSONObject("tokens"),imported.getJSONObject("tokens"),account,now);
        JSONObject earned=s.getJSONObject("achievements"),other=imported.getJSONObject("achievements");for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject a=other.optJSONObject(e.id);if(!mergeMedals||a==null||!a.optBoolean("unlocked"))continue;JSONObject own=earned.optJSONObject(e.id);
            if(own==null||!own.optBoolean("unlocked"))earned.put(e.id,new JSONObject().put("id",e.id).put("name_ko",e.ko).put("name_en",e.en).put("description_ko",e.descriptionKo()).put("description_en",e.descriptionEn()).put("condition",e.metric+">="+e.target+";days>="+e.days+";weeks>="+e.weeks).put("target",e.target).put("progress",a.optLong("progress")).put("rarity",e.rarity).put("reward_xp",e.bonus).put("unlocked",true).put("first_earned_at",a.getLong("first_earned_at")).put("time_basis","verified_trusted_journal").put("rule_version",VERSION));
        }
        for(String region:new String[]{"KR","WORLD"}){JSONObject incoming=imported.getJSONObject(region),own=s.getJSONObject(region);int grade=incoming.getInt("tier"),peak=incoming.getInt("highest");if(grade< -1||grade>9||peak<grade||peak>9)throw new IllegalArgumentException("Tier range");
            int highest=Math.max(own.getInt("highest"),peak);if(incoming.optLong("evaluated_at")>own.optLong("evaluated_at")){JSONObject clean=RpgTier.empty(region);for(String key:new String[]{"tier","highest","protected_until","end","low_streak","score","count","warning","latest_score","provisional","evaluated_at","policy","weeks","evaluations"})if(incoming.has(key))clean.put(key,new JSONObject(incoming.toString()).get(key));s.put(region,clean);}s.getJSONObject(region).put("highest",highest);
        }
        for(String field:new String[]{"activity_days","active_days_30","valid_weeks"})s.put(field,Math.max(s.optInt(field),imported.optInt(field)));
        JSONObject importedTitles=imported.optJSONObject("titles"),ownedTitles=s.optJSONObject("titles");if(ownedTitles==null){ownedTitles=new JSONObject();s.put("titles",ownedTitles);}if(TitleJourney.journey(s)==null&&importedTitles!=null)for(String id:AccountProgress.keys(importedTitles)){RpgTitleCatalog.Title def=RpgTitleCatalog.find(id);JSONObject fact=importedTitles.getJSONObject(id);if(def==null||fact.optBoolean("unlocked")&&titleEvidence(def,imported)<1||fact.optBoolean("unlocked")&&(fact.getLong("first_earned_at")<=0||fact.getLong("first_earned_at")>now))throw new IllegalArgumentException("Title evidence");JSONObject own=ownedTitles.optJSONObject(id);if(own==null)ownedTitles.put(id,new JSONObject(fact.toString()));else if(fact.optBoolean("unlocked")){long first=own.optBoolean("unlocked")?Math.min(own.getLong("first_earned_at"),fact.getLong("first_earned_at")):fact.getLong("first_earned_at");ownedTitles.put(id,new JSONObject(fact.toString()).put("first_earned_at",first));}}
        String title=imported.optString("equipped_title");if(!s.optBoolean("title_selection_set")&&!s.has("equipped_title")&&eligibleTitles(document).contains(title))s.put("equipped_title",title);
        JSONObject ownFacts=s.getJSONObject("achievements");for(RpgCatalog.Entry e:RpgCatalog.ALL){JSONObject otherFact=other.optJSONObject(e.id),ownFact=ownFacts.optJSONObject(e.id);if(!mergeMedals||otherFact==null)continue;
            if(ownFact==null){ownFact=new JSONObject(otherFact.toString());ownFacts.put(e.id,ownFact);}
            for(String field:new String[]{"progress","progress_days","progress_weeks"})ownFact.put(field,Math.max(ownFact.optLong(field),otherFact.optLong(field)));
            ownFact.put("progress_ratio",Math.max(ownFact.optDouble("progress_ratio",0),otherFact.optDouble("progress_ratio",0)));
            if(otherFact.optBoolean("unlocked")&&ownFact.optBoolean("unlocked"))ownFact.put("first_earned_at",Math.min(ownFact.optLong("first_earned_at",Long.MAX_VALUE),otherFact.getLong("first_earned_at")));
        }
        s.put("dawn",s.optBoolean("dawn")||imported.optBoolean("dawn"));s.put("longest_observed_streak",Math.max(s.optInt("longest_observed_streak"),imported.optInt("longest_observed_streak")));
        if(!s.has("recommended_title")&&eligibleTitles(document).contains(imported.optString("recommended_title")))s.put("recommended_title",imported.getString("recommended_title"));
        s.put("kr_legacy_seeded",s.optBoolean("kr_legacy_seeded")||imported.optBoolean("kr_legacy_seeded"));
        s.put("level",level(s.getLong("xp"))).put("restored_at",now);refresh(document,now,false,false);
    }
    private static double titleEvidence(RpgTitleCatalog.Title t,JSONObject s)throws JSONException {
        double ratio=t.ratio(s);JSONObject j=AchievementJourney.journey(s),legacy=j==null?null:j.optJSONObject("legacy");if(legacy!=null&&legacy.has("achievements")){JSONObject old=new JSONObject(s.toString()).put("achievements",legacy.getJSONObject("achievements"));ratio=Math.max(ratio,t.ratio(old));}return ratio;
    }
    public static JSONObject summary(JSONObject document)throws JSONException {JSONObject s=state(document),out=new JSONObject();for(String k:new String[]{"version","model","collection_revision","xp","level","activity_days","active_days_30","valid_weeks","token_status","token_total","token_basis","token_observed_at","daily_basis","quota_score_basis","demotion_status","demotion_checked_at","promotion_evidence","updated_at","recommended_title","equipped_title"})if(s.has(k))out.put(k,s.get(k));
        JSONObject journey=AchievementJourney.journey(s);if(journey!=null)out.put("achievement_enabled",journey.getBoolean("enabled")).put("achievement_started_at",journey.getLong("first_started_at"));
        JSONObject tj=TitleJourney.journey(s);if(tj!=null)out.put("title_enabled",tj.optBoolean("enabled")).put("title_started_at",tj.optLong("first_started_at"));
        for(String region:new String[]{"KR","WORLD"}){JSONObject t=s.getJSONObject(region),v=new JSONObject();for(String k:new String[]{"tier","highest","protected_until","count","latest_score","provisional","evaluated_at","model","warning","label"})if(t.has(k))v.put(k,t.get(k));out.put(region,v);}out.put("unlocked_achievements",RpgGrowthFacts.earned(s).size()).put("unlocked_ids",new JSONArray(RpgGrowthFacts.earned(s))).put("unlocked_titles",new JSONArray(eligibleTitles(document)));return out;}
}
