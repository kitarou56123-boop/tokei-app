package com.kitarou.tokei;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 日本の祝日（現行の法律に基づく計算） */
final class Holidays {
    private Holidays() {}

    private static final Map<Integer, Map<LocalDate, String>> cache = new HashMap<>();

    static String name(LocalDate d) {
        return forYear(d.getYear()).get(d);
    }

    static synchronized Map<LocalDate, String> forYear(int y) {
        Map<LocalDate, String> m = cache.get(y);
        if (m != null) return m;
        m = new TreeMap<>();
        m.put(LocalDate.of(y, 1, 1), "元日");
        m.put(nthMonday(y, 1, 2), "成人の日");
        m.put(LocalDate.of(y, 2, 11), "建国記念の日");
        m.put(LocalDate.of(y, 2, 23), "天皇誕生日");
        m.put(LocalDate.of(y, 3, equinox(y, 20.8431)), "春分の日");
        m.put(LocalDate.of(y, 4, 29), "昭和の日");
        m.put(LocalDate.of(y, 5, 3), "憲法記念日");
        m.put(LocalDate.of(y, 5, 4), "みどりの日");
        m.put(LocalDate.of(y, 5, 5), "こどもの日");
        m.put(nthMonday(y, 7, 3), "海の日");
        m.put(LocalDate.of(y, 8, 11), "山の日");
        m.put(nthMonday(y, 9, 3), "敬老の日");
        m.put(LocalDate.of(y, 9, equinox(y, 23.2488)), "秋分の日");
        m.put(nthMonday(y, 10, 2), "スポーツの日");
        m.put(LocalDate.of(y, 11, 3), "文化の日");
        m.put(LocalDate.of(y, 11, 23), "勤労感謝の日");

        // 国民の休日（祝日にはさまれた平日）
        List<LocalDate> base = new ArrayList<>(m.keySet());
        for (LocalDate d : base) {
            LocalDate mid = d.plusDays(1);
            if (m.containsKey(d.plusDays(2)) && !m.containsKey(mid)
                    && mid.getDayOfWeek() != DayOfWeek.SUNDAY) {
                m.put(mid, "国民の休日");
            }
        }
        // 振替休日
        base = new ArrayList<>(m.keySet());
        for (LocalDate d : base) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                LocalDate s = d.plusDays(1);
                while (m.containsKey(s)) s = s.plusDays(1);
                m.put(s, "振替休日");
            }
        }
        cache.put(y, m);
        return m;
    }

    private static LocalDate nthMonday(int y, int month, int n) {
        return LocalDate.of(y, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, DayOfWeek.MONDAY));
    }

    private static int equinox(int y, double base) {
        return (int) Math.floor(base + 0.242194 * (y - 1980) - Math.floor((y - 1980) / 4.0));
    }
}
