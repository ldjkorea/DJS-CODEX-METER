package dev.bennett.codexmeter;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

/** Separate provider; both sources reuse their existing stores, authentication and schedulers. */
public final class CombinedStatusWidget extends AppWidgetProvider {
    private static final String REFRESH="dev.bennett.codexmeter.COMBINED_REFRESH";
    static boolean installed(Context c){return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,CombinedStatusWidget.class)).length>0;}
    static void update(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,CombinedStatusWidget.class)))draw(c,m,id);}
    static RemoteViews render(Context c,int width,int height){
        RemoteViews v=TaskStatusWidget.render(c,width,Math.max(140,height-(int)(60*Math.max(1,c.getResources().getConfiguration().fontScale))),TierTheme.tier(c),TaskStatusStore.snapshot(c),TaskStatusStore.received(c),TaskStatusStore.enabled(c),TaskStatusStore.failed(c));
        v.setViewVisibility(R.id.combined_usage,View.VISIBLE);v.setViewVisibility(R.id.combined_usage_time,View.VISIBLE);
        UsageSnapshot snapshot=SubscriptionStore.accountKey(c)==null?null:AppPreferences.loadSnapshot(c);UsageWindow w=snapshot==null||!AppPreferences.showDashboardWeekly(c)?null:snapshot.weekly;
        String text=c.getString(R.string.combined_widget_unknown);
        if(w!=null){boolean fresh=UsageEvidence.state(snapshot,w,!AppPreferences.getLastError(c).isEmpty(),System.currentTimeMillis(),RefreshScheduler.effectiveRefreshMinutes(c))==UsageEvidence.State.CONFIRMED;
            text=c.getString(R.string.combined_widget_usage,UsagePrecision.remaining(w,0))+(fresh?"":" · "+c.getString(R.string.evo_stale));
        }
        v.setTextViewText(R.id.combined_usage,text);v.setTextColor(R.id.combined_usage,TierTheme.accent(c,true));
        v.setTextViewText(R.id.combined_usage_time,snapshot==null?"—":c.getString(R.string.combined_widget_usage_time,V3Display.time(snapshot.fetchedAtMillis)));
        return v;
    }
    private static void draw(Context c,AppWidgetManager m,int id){Bundle o=m.getAppWidgetOptions(id);String key="combined:"+id;
        UsageSnapshot s=AppPreferences.loadSnapshot(c);String signature=WidgetRenderCache.environment(c,o)+"|"+SubscriptionStore.accountKey(c)+"|"+LiveUsageStore.signature(c,s)+"|"+AppPreferences.showDashboardWeekly(c)+"|"+TierTheme.tier(c)+"|"+AppPreferences.getLastError(c)+"|"+System.currentTimeMillis()/60000+"|"+TaskStatusData.widgetFingerprint(TaskStatusStore.snapshot(c),TaskStatusStore.received(c),TaskStatusStore.failed(c),System.currentTimeMillis(),TaskStatusStore.enabled(c));
        if(WidgetRenderCache.unchanged(key,signature))return;
        RemoteViews v=render(c,Math.max(160,o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320)),Math.max(200,o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,260)));
        v.setOnClickPendingIntent(R.id.task_status_root,PendingIntent.getActivity(c,id,new Intent(c,TaskStatusActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.combined_usage,PendingIntent.getActivity(c,id,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.task_status_refresh,PendingIntent.getBroadcast(c,id,new Intent(c,CombinedStatusWidget.class).setAction(REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(id,v);WidgetRenderCache.delivered(key,signature);
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids){WidgetRenderCache.forget("combined:"+id);draw(c,m,id);}RefreshScheduler.schedulePeriodic(c);TaskStatusJobService.schedule(c);TaskStatusWidget.scheduleExpiry(c);if(!WidgetRenderCache.freshUsage(c))RefreshScheduler.scheduleImmediate(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle o){WidgetRenderCache.forget("combined:"+id);draw(c,m,id);}
    @Override public void onDisabled(Context c){RefreshScheduler.schedulePeriodic(c);TaskStatusJobService.schedule(c);TaskStatusWidget.scheduleExpiry(c);}
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids)WidgetRenderCache.forget("combined:"+id);}
    @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(REFRESH.equals(i.getAction())){RefreshScheduler.scheduleImmediate(c);if(TaskStatusStore.enabled(c))LanSync.taskStatus(c,ok->update(c));}}
}
