package dev.bennett.codexmeter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.*;
import android.view.View;
import android.widget.RemoteViews;
import org.json.*;

/** Independent provider; never touches IDs/options belonging to the usage/tier widgets. */
public final class TaskStatusWidget extends AppWidgetProvider {
    private static final String REFRESH="dev.bennett.codexmeter.TASK_WIDGET_REFRESH";
    private static final String EXPIRE="dev.bennett.codexmeter.TASK_WIDGET_EXPIRE";
    static void update(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);for(int id:manager.getAppWidgetIds(new ComponentName(c,TaskStatusWidget.class)))draw(c,manager,id);scheduleExpiry(c);}
    /** One local, non-wakeup repaint; no extra PC or quota request. Android may defer it. */
    static void scheduleExpiry(Context c){
        android.app.AlarmManager alarms=(android.app.AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(alarms==null)return;
        PendingIntent tick=PendingIntent.getBroadcast(c,0,new Intent(c,TaskStatusWidget.class).setAction(EXPIRE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        alarms.cancel(tick);long now=System.currentTimeMillis(),received=TaskStatusStore.received(c);JSONObject data=TaskStatusStore.snapshot(c);
        if(AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,TaskStatusWidget.class)).length==0||!TaskStatusStore.enabled(c)||!TaskStatusStore.connected(c,data,received,now))return;
        long expires=Math.min(received+120001,data.optLong("checked")+120001);
        if(expires>now)alarms.set(android.app.AlarmManager.RTC,expires,tick);
    }
    private static void draw(Context c,AppWidgetManager manager,int id){Bundle options=manager.getAppWidgetOptions(id);
        int width=Math.max(160,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280)),height=Math.max(140,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,200));
        RemoteViews views=render(c,width,height,TierTheme.tier(c),TaskStatusStore.snapshot(c),TaskStatusStore.received(c),TaskStatusStore.enabled(c),TaskStatusStore.failed(c));
        views.setOnClickPendingIntent(R.id.task_status_root,PendingIntent.getActivity(c,id,new Intent(c,TaskStatusActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        views.setOnClickPendingIntent(R.id.task_status_refresh,PendingIntent.getBroadcast(c,id,new Intent(c,TaskStatusWidget.class).setAction(REFRESH),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));manager.updateAppWidget(id,views);
    }
    static RemoteViews render(Context c,int width,int height,int tier,JSONObject data,long received,boolean enabled){
        return render(c,width,height,tier,data,received,enabled,0);
    }
    static RemoteViews render(Context c,int width,int height,int tier,JSONObject data,long received,boolean enabled,long failed){
        RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.task_status_widget);views.removeAllViews(R.id.task_status_tasks);
        views.setTextColor(R.id.task_status_tier,tier>=0?WidgetTierPalette.accent(tier):0xffc2c9d0);
        views.setTextViewText(R.id.task_status_tier,tier>=0?TierPresentation.ENGLISH[tier]:c.getString(R.string.fun_placing));
        Bitmap emblem=Bitmap.createBitmap(144,144,Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable crest=androidx.appcompat.content.res.AppCompatResources.getDrawable(c,TierTheme.EMBLEMS[Math.max(0,tier)]);
        crest.setBounds(0,0,144,144);crest.setAlpha(tier<0?100:255);crest.draw(new Canvas(emblem));views.setImageViewBitmap(R.id.task_status_emblem,emblem);
        views.setImageViewBitmap(R.id.task_status_background,background(width,height,tier));
        boolean connected=enabled&&failed<=received&&TaskStatusData.connected(data,received,System.currentTimeMillis());
        views.setTextViewText(R.id.task_status_connection,c.getString(!enabled?R.string.task_trial_off:connected?R.string.task_trial_connected:R.string.task_trial_disconnected));
        views.setTextColor(R.id.task_status_connection,connected?0xff86dfb6:0xffc2c9d0);
        views.setViewVisibility(R.id.task_status_connection,View.VISIBLE);
        views.setTextViewText(R.id.task_status_copy,received>0?c.getString(R.string.task_trial_last,time(c,received)):c.getString(R.string.task_trial_unverified));
        JSONArray rows=TaskStatusData.displayRows(data);float font=Math.max(1,c.getResources().getConfiguration().fontScale);
        int columns=TaskStatusData.widgetColumns((int)(width/font)),maximum=TaskStatusData.widgetCapacity(width,height,font);
        int count=enabled&&rows!=null?Math.min(maximum,rows.length()):0;
        boolean current=connected&&data!=null&&data.optBoolean("verified");
        int running=0;if(current)for(int i=0;i<rows.length();i++)if("running".equals(rows.optJSONObject(i).optString("state")))running++;
        views.setTextViewText(R.id.task_status_heading,running>0?c.getString(R.string.ws_running,running):c.getString(R.string.task_named_heading));
        RemoteViews line=null;
        for(int i=0;i<Math.max(1,count);i++){
            if(i%columns==0)line=new RemoteViews(c.getPackageName(),R.layout.task_status_row);
            RemoteViews item=new RemoteViews(c.getPackageName(),R.layout.task_status_item);
            if(count==0){item.setTextViewText(R.id.task_status_name,c.getString(!enabled?R.string.task_trial_off:!connected?R.string.task_trial_disconnected:!current?R.string.task_trial_unverified:R.string.task_named_empty));item.setViewVisibility(R.id.task_status_state,View.GONE);item.setViewVisibility(R.id.task_status_dot,View.GONE);}
            else {JSONObject row=rows.optJSONObject(i);if(row==null)continue;
                String name=TaskStatusStore.title(row),state=current?TaskStatusStore.state(c,row.optString("state")):c.getString(connected?R.string.task_trial_unverified:R.string.task_trial_disconnected);
                int color=TaskStatusData.widgetColor(current,row.optString("state"));
                String issue=current?TaskStatusStore.issue(c,row):"";
                item.setTextViewText(R.id.task_status_name,name);item.setTextViewText(R.id.task_status_state,issue.isEmpty()?state:state+" · "+issue);
                item.setTextColor(R.id.task_status_dot,color);item.setTextColor(R.id.task_status_state,color);
                item.setContentDescription(R.id.task_status_name,name+" · "+state+" · "+time(c,row.optLong("at"))+" · "+TaskStatusStore.issue(c,row));
            }
            line.addView(R.id.task_status_row_items,item);
            if(i%columns==columns-1||i==Math.max(1,count)-1)views.addView(R.id.task_status_tasks,line);
        }
        return views;
    }
    private static String time(Context c,long at){return (System.currentTimeMillis()-at>=86400000?android.text.format.DateFormat.getDateFormat(c).format(new java.util.Date(at))+" ":"")+android.text.format.DateFormat.getTimeFormat(c).format(new java.util.Date(at));}
    private static Bitmap background(int width,int height,int tier){int w=Math.min(360,width),h=Math.min(480,height);Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.setDensity(160);Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0,0,0,h,tier<0?0xff252c33:WidgetTierPalette.top(tier),tier<0?0xff12171d:WidgetTierPalette.bottom(tier),Shader.TileMode.CLAMP));canvas.drawRoundRect(new RectF(0,0,w,h),20,20,paint);
        TierTheme.Frame frame=new TierTheme.Frame(tier,true,false);frame.setBounds(0,0,w,h);frame.draw(canvas);return bitmap;}
    @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)draw(c,manager,id);scheduleExpiry(c);TaskStatusJobService.schedule(c);if(TaskStatusStore.enabled(c))LanSync.taskStatus(c,ok->update(c));}
    @Override public void onDisabled(Context c){scheduleExpiry(c);TaskStatusJobService.schedule(c);}
    @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);if(EXPIRE.equals(intent.getAction()))update(c);if(REFRESH.equals(intent.getAction())&&TaskStatusStore.enabled(c))LanSync.taskStatus(c,ok->update(c));}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,Bundle options){draw(c,manager,id);}
}
