package com.kitarou.tokei;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotifyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (Notifier.ACTION.equals(a)) {
            Notifier.postTomorrow(c);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            if (Notifier.pastNotifyTime()) Notifier.postTomorrow(c);
            ClockWidget.updateAll(c);
        }
        Notifier.schedule(c);
    }
}
