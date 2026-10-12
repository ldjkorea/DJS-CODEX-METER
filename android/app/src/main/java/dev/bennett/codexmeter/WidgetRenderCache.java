package dev.bennett.codexmeter;

import android.content.Context;
import android.os.Bundle;
import java.lang.reflect.Modifier;
import java.util.*;

/** Process-only delivery markers. No bitmap, token, disk write, or network operation. */
final class WidgetRenderCache {
    private static final Map<String,String> SENT=new LinkedHashMap<String,String>(64,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<String,String> entry){return size()>64;}
    };
    static synchronized boolean unchanged(String id,String signature){return signature.equals(SENT.get(id));}
    static synchronized void delivered(String id,String signature){SENT.put(id,signature);}
    static synchronized void forget(String id){SENT.remove(id);}
    static boolean freshUsage(Context c){
        UsageSnapshot snapshot=AppPreferences.loadSnapshot(c);long now=System.currentTimeMillis();
        if(snapshot==null||!AppPreferences.getLastError(c).isEmpty()||UsageInsights.freshness(snapshot.fetchedAtMillis,now,RefreshScheduler.effectiveRefreshMinutes(c))!=UsageInsights.Freshness.FRESH)return false;
        for(UsageWindow window:new UsageWindow[]{snapshot.fiveHour,snapshot.weekly,snapshot.monthly})if(window!=null&&window.effectiveResetAtMillis(snapshot.fetchedAtMillis)>0&&window.effectiveResetAtMillis(snapshot.fetchedAtMillis)<=now)return false;
        return true;
    }
    static String environment(Context c,Bundle options){
        StringBuilder s=new StringBuilder(c.getResources().getConfiguration().toString());
        s.append('|').append(android.text.format.DateFormat.is24HourFormat(c));
        if(options!=null)for(String key:new TreeSet<>(options.keySet()))s.append('|').append(key).append('=').append(options.get(key));
        return s.toString();
    }
    static String usage(Context c,int id,Bundle size,WidgetOptions options){
        StringBuilder s=new StringBuilder(environment(c,size));
        try{for(java.lang.reflect.Field field:WidgetOptions.class.getFields())if(!Modifier.isStatic(field.getModifiers()))s.append('|').append(field.getName()).append('=').append(field.get(options));
            UsageSnapshot snapshot=AppPreferences.loadSnapshot(c);ResetCreditsSnapshot credits=AppPreferences.loadResetCredits(c);
            s.append('|').append(snapshot==null?"":snapshot.toJson()).append('|').append(credits==null?"":credits.toJson());
            if(snapshot!=null){long now=System.currentTimeMillis();s.append('|').append(UsageInsights.freshness(snapshot.fetchedAtMillis,now,RefreshScheduler.effectiveRefreshMinutes(c)));
                for(UsageWindow window:new UsageWindow[]{snapshot.fiveHour,snapshot.weekly,snapshot.monthly})if(window!=null)s.append('|').append(window.effectiveResetAtMillis(snapshot.fetchedAtMillis)>now);
            }
        }catch(Exception impossible){return UUID.randomUUID().toString();} // Render rather than hide an unexpected change.
        s.append('|').append(SubscriptionStore.accountKey(c)).append('|').append(TierTheme.tier(c)).append('|').append(WidgetCrest.enabled(c,id))
         .append('|').append(AppPreferences.getWidgetTapAction(c,id)).append('|').append(AppPreferences.usageDecimalPlaces(c))
         .append('|').append(AppPreferences.showDashboardFiveHour(c)).append('|').append(AppPreferences.showDashboardWeekly(c))
         .append('|').append(AppPreferences.showDashboardMonthly(c)).append('|').append(AppPreferences.showDashboardAdditionalLimits(c))
         .append('|').append(AppPreferences.getLastError(c)).append('|').append(AppPreferences.getResetCreditsError(c))
         .append('|').append(RefreshScheduler.effectiveRefreshMinutes(c)).append('|').append(System.currentTimeMillis()/60000);
        return s.toString();
    }
}
