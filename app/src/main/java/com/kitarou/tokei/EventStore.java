package com.kitarou.tokei;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EventStore {
    private EventStore() {}

    static final class Ev {
        long id;
        String date;   // yyyy-MM-dd
        int min;       // 開始時間（分）。-1 は時間なし
        String text;
    }

    static List<Ev> all(Context c) {
        List<Ev> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(Prefs.sp(c).getString("events", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Ev e = new Ev();
                e.id = o.getLong("id");
                e.date = o.getString("date");
                e.min = o.optInt("min", -1);
                e.text = o.optString("text", "");
                list.add(e);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static void saveAll(Context c, List<Ev> list) {
        JSONArray a = new JSONArray();
        try {
            for (Ev e : list) {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("date", e.date);
                o.put("min", e.min);
                o.put("text", e.text);
                a.put(o);
            }
        } catch (Exception ignored) {
        }
        Prefs.sp(c).edit().putString("events", a.toString()).apply();
    }

    /** バックアップ用：全部の予定を文字にする */
    static String exportJson(Context c) {
        return Prefs.sp(c).getString("events", "[]");
    }

    /** バックアップの読み込み：同じ予定は重ねずに足す。足した件数を返す */
    static int importJson(Context c, String json) throws Exception {
        JSONArray a = new JSONArray(json.trim());
        List<Ev> list = all(c);
        java.util.Set<String> have = new java.util.HashSet<>();
        for (Ev e : list) have.add(e.id + "|" + e.date + "|" + e.min + "|" + e.text);
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (Ev e : list) ids.add(e.id);
        int added = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            Ev e = new Ev();
            e.id = o.getLong("id");
            e.date = o.getString("date");
            e.min = o.optInt("min", -1);
            e.text = o.optString("text", "");
            java.time.LocalDate.parse(e.date);
            if (have.contains(e.id + "|" + e.date + "|" + e.min + "|" + e.text)) continue;
            while (ids.contains(e.id)) e.id++;
            ids.add(e.id);
            list.add(e);
            added++;
        }
        saveAll(c, list);
        return added;
    }

    static void sort(List<Ev> list) {
        Collections.sort(list, (x, y) -> {
            int d = x.date.compareTo(y.date);
            if (d != 0) return d;
            if (x.min != y.min) return Integer.compare(x.min, y.min);
            return Long.compare(x.id, y.id);
        });
    }

    static List<Ev> on(Context c, LocalDate day) {
        String key = day.toString();
        List<Ev> out = new ArrayList<>();
        for (Ev e : all(c)) if (e.date.equals(key)) out.add(e);
        sort(out);
        return out;
    }

    static Map<String, List<Ev>> byDate(Context c) {
        List<Ev> list = all(c);
        sort(list);
        Map<String, List<Ev>> m = new HashMap<>();
        for (Ev e : list) {
            List<Ev> l = m.get(e.date);
            if (l == null) {
                l = new ArrayList<>();
                m.put(e.date, l);
            }
            l.add(e);
        }
        return m;
    }

    static void put(Context c, Ev ev) {
        List<Ev> list = all(c);
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == ev.id) {
                list.set(i, ev);
                found = true;
                break;
            }
        }
        if (!found) list.add(ev);
        saveAll(c, list);
    }

    static void delete(Context c, long id) {
        List<Ev> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id == id) list.remove(i);
        }
        saveAll(c, list);
    }
}
