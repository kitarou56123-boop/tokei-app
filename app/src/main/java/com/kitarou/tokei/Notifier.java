package com.kitarou.tokei;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** 予定の前日の朝に、通知バーへお知らせを出す */
final class Notifier {
    private Notifier() {}

    static final int NOTIFY_HOUR = 8;
    static final String ACTION = "com.kitarou.tokei.NOTIFY";
    private static final String CH = "tomorrow";
    private static final int NID = 1;

    static void schedule(Context c) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime t = LocalDate.now().atTime(LocalTime.of(NOTIFY_HOUR, 0));
        if (!t.isAfter(now)) t = t.plusDays(1);
        long when = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, NotifyReceiver.class).setAction(ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
    }

    static boolean pastNotifyTime() {
        return LocalTime.now().getHour() >= NOTIFY_HOUR;
    }

    /** 予定が変わったとき：明日の予定に関係していれば、すぐに出し直す */
    static void afterChange(Context c, LocalDate... dates) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        for (LocalDate d : dates) {
            if (d != null && d.equals(tomorrow)) {
                if (pastNotifyTime()) postTomorrow(c);
                return;
            }
        }
    }

    static void postTomorrow(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<EventStore.Ev> list = EventStore.on(c, tomorrow);
        if (list.isEmpty()) {
            nm.cancel(NID);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationChannel ch = new NotificationChannel(CH, "前日のお知らせ", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);

        String day = tomorrow.format(DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPAN));
        Notification.InboxStyle style = new Notification.InboxStyle();
        for (EventStore.Ev e : list) style.addLine(line(c, e));
        style.setBigContentTitle("明日 " + day + " の予定");

        Intent open = new Intent(c, CalendarActivity.class);
        open.putExtra("date", tomorrow.toString());
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String text = list.size() == 1 ? line(c, list.get(0))
                : line(c, list.get(0)) + " ほか" + (list.size() - 1) + "件";
        Notification n = new Notification.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("明日 " + day + " の予定")
                .setContentText(text)
                .setStyle(style)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        try {
            nm.notify(NID, n);
        } catch (SecurityException ignored) {
        }
    }

    private static String line(Context c, EventStore.Ev e) {
        String t = Prefs.fmtTime(c, e.min);
        return t.isEmpty() ? e.text : t + "　" + e.text;
    }
}
