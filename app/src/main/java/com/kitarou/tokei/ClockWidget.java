package com.kitarou.tokei;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class ClockWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        mgr.updateAppWidget(ids, views(ctx));
    }

    static RemoteViews views(Context ctx) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(),
                Prefs.is24(ctx) ? R.layout.widget_24 : R.layout.widget_12);
        Intent i = new Intent(ctx, CalendarActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.root, pi);
        return rv;
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, ClockWidget.class));
        if (ids != null && ids.length > 0) mgr.updateAppWidget(ids, views(ctx));
    }
}
