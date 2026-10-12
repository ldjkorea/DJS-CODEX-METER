package dev.bennett.codexmeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/* JADX INFO: loaded from: classes.dex */
public final class WidgetRefreshReceiver extends BroadcastReceiver {
    private static long clicked=-10000;
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent != null && AppConstants.ACTION_REFRESH_WIDGET.equals(intent.getAction())) {
            synchronized(WidgetRefreshReceiver.class){long now=android.os.SystemClock.elapsedRealtime();if(now-clicked<5000)return;clicked=now;}
            RefreshScheduler.scheduleImmediate(context);
            WidgetRenderer.updateAll(context);
        }
    }
}
