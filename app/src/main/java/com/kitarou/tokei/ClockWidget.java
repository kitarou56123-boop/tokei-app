package com.kitarou.tokei;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class ClockWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, views(ctx, mgr.getAppWidgetOptions(id)));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle opts) {
        mgr.updateAppWidget(id, views(ctx, opts));
    }

    /** 置いた枠の大きさに合わせて文字の大きさを決める */
    static RemoteViews views(Context ctx, Bundle opts) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(),
                Prefs.is24(ctx) ? R.layout.widget_24 : R.layout.widget_12);

        float timeSp = 60;
        if (opts != null) {
            int w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            int h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            if (w > 0 && h > 0) {
                float fs = ctx.getResources().getConfiguration().fontScale;
                if (fs <= 0) fs = 1f;
                timeSp = Math.min((w - 8) / 2.9f, (h - 8) / 1.95f) / fs;
            }
        }
        timeSp = Math.max(18, Math.min(130, timeSp));
        float dateSp = Math.max(10, timeSp * 0.28f);
        rv.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_SP, timeSp);
        rv.setTextViewTextSize(R.id.date, TypedValue.COMPLEX_UNIT_SP, dateSp);

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
        if (ids == null) return;
        for (int id : ids) mgr.updateAppWidget(id, views(ctx, mgr.getAppWidgetOptions(id)));
    }
}
