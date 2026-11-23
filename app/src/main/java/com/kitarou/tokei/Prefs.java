package com.kitarou.tokei;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("tokei", Context.MODE_PRIVATE);
    }

    static boolean is24(Context c) {
        return sp(c).getBoolean("is24", true);
    }

    static void set24(Context c, boolean v) {
        sp(c).edit().putBoolean("is24", v).apply();
    }

    /** 分(0-1439) を表示用文字列に。-1 は時間なし。 */
    static String fmtTime(Context c, int min) {
        if (min < 0) return "";
        int h = min / 60, m = min % 60;
        if (is24(c)) return h + ":" + (m < 10 ? "0" : "") + m;
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return (h < 12 ? "午前" : "午後") + h12 + ":" + (m < 10 ? "0" : "") + m;
    }
}
