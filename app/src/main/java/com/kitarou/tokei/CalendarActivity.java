package com.kitarou.tokei;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 白いカレンダー画面 */
public class CalendarActivity extends Activity {

    private static final int RED = 0xFFE53935;
    private static final int BLUE = 0xFF1E88E5;
    private static final int TEXT = 0xFF333333;
    private static final int LINE = 0xFFDDDDDD;
    private static final int GREEN = 0xFF1AA260;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPAN);
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPAN);

    private YearMonth shown = YearMonth.now();
    private TextView titleV;
    private LinearLayout grid;
    private GestureDetector gd;
    private boolean flung;
    private TextView ampmV, clockV, modeB;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            long now = System.currentTimeMillis();
            h.postDelayed(this, 1000 - (now % 1000) + 2);
        }
    };

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // 見出し：年月と ◀ ▶
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), dp(8), dp(8), dp(8));
        titleV = new TextView(this);
        titleV.setTextSize(26);
        titleV.setTypeface(Typeface.DEFAULT_BOLD);
        titleV.setTextColor(0xFF444444);
        head.addView(titleV, new LinearLayout.LayoutParams(0, -2, 1));
        TextView todayB = navButton("今日", v -> { shown = YearMonth.now(); render(); });
        todayB.setTextSize(17);
        todayB.setTextColor(RED);
        todayB.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(48)));
        head.addView(todayB);
        head.addView(navButton("◀", v -> { shown = shown.minusMonths(1); render(); }));
        head.addView(navButton("▶", v -> { shown = shown.plusMonths(1); render(); }));
        root.addView(head);

        // 曜日
        LinearLayout wk = new LinearLayout(this);
        wk.setOrientation(LinearLayout.HORIZONTAL);
        wk.setBackgroundColor(0xFFF2F2F2);
        String[] names = {"日", "月", "火", "水", "木", "金", "土"};
        for (int i = 0; i < 7; i++) {
            TextView t = new TextView(this);
            t.setText(names[i]);
            t.setTextSize(16);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(6), 0, dp(6));
            t.setTextColor(i == 0 ? RED : i == 6 ? BLUE : TEXT);
            wk.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(wk);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackgroundColor(LINE);
        grid.setPadding(1, 1, 0, 0);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));

        // 下：秒まで出る時計と 24時間切替
        View top = new View(this);
        top.setBackgroundColor(LINE);
        root.addView(top, new LinearLayout.LayoutParams(-1, 1));
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFFF7F7F7);
        bar.setPadding(dp(16), dp(8), dp(12), dp(10));

        ampmV = new TextView(this);
        ampmV.setTextSize(16);
        ampmV.setTextColor(0xFF666666);
        ampmV.setPadding(0, 0, dp(6), 0);
        bar.addView(ampmV);

        clockV = new TextView(this);
        clockV.setTextSize(46);
        clockV.setTextColor(0xFF222222);
        clockV.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockV.setFontFeatureSettings("tnum");
        clockV.setSingleLine(true);
        clockV.setIncludeFontPadding(false);
        bar.addView(clockV, new LinearLayout.LayoutParams(0, -2, 1));

        modeB = new TextView(this);
        modeB.setTextSize(15);
        modeB.setTextColor(TEXT);
        modeB.setGravity(Gravity.CENTER);
        GradientDrawable mbg = new GradientDrawable();
        mbg.setColor(Color.WHITE);
        mbg.setStroke(dp(1), 0xFFBBBBBB);
        mbg.setCornerRadius(dp(24));
        modeB.setBackground(mbg);
        modeB.setOnClickListener(v -> {
            Prefs.set24(this, !Prefs.is24(this));
            ClockWidget.updateAll(this);
            updateClock();
        });
        bar.addView(modeB, new LinearLayout.LayoutParams(dp(112), dp(48)));
        root.addView(bar);

        setContentView(root);

        gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null) return false;
                float dx = e2.getX() - e1.getX(), dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > dp(80) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    shown = dx < 0 ? shown.plusMonths(1) : shown.minusMonths(1);
                    render();
                    flung = true;
                    return true;
                }
                return false;
            }
        });

        Notifier.schedule(this);
        ClockWidget.updateAll(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        openFromIntent();
    }

    private void updateClock() {
        Calendar c = Calendar.getInstance();
        boolean is24 = Prefs.is24(this);
        int hh = c.get(Calendar.HOUR_OF_DAY);
        int shownH = hh;
        if (is24) {
            ampmV.setVisibility(View.GONE);
        } else {
            shownH = hh % 12;
            if (shownH == 0) shownH = 12;
            ampmV.setText(hh < 12 ? "午前" : "午後");
            ampmV.setVisibility(View.VISIBLE);
        }
        clockV.setText(String.format(Locale.US, "%d:%02d:%02d", shownH, c.get(Calendar.MINUTE), c.get(Calendar.SECOND)));
        modeB.setText(is24 ? "24時間表示" : "12時間表示");
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(tick);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getStringExtra("date") == null) {
            shown = YearMonth.now();
            render();
        }
        openFromIntent();
    }

    private void openFromIntent() {
        String d = getIntent().getStringExtra("date");
        if (d != null) {
            getIntent().removeExtra("date");
            try {
                LocalDate day = LocalDate.parse(d);
                shown = YearMonth.from(day);
                render();
                showDay(day);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        h.removeCallbacks(tick);
        tick.run();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) flung = false;
        gd.onTouchEvent(ev);
        if (flung && ev.getActionMasked() == MotionEvent.ACTION_UP) {
            MotionEvent c = MotionEvent.obtain(ev);
            c.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(c);
            c.recycle();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private TextView navButton(String s, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(22);
        t.setTextColor(0xFF666666);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(48)));
        t.setOnClickListener(l);
        return t;
    }

    private TextView barButton(String s, int color, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(18);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(l);
        return t;
    }

    private void render() {
        titleV.setText(shown.getYear() + "年" + shown.getMonthValue() + "月");
        grid.removeAllViews();
        LocalDate first = shown.atDay(1);
        int offset = first.getDayOfWeek().getValue() % 7; // 日曜=0
        int rows = (offset + shown.lengthOfMonth() + 6) / 7;
        LocalDate start = first.minusDays(offset);
        LocalDate today = LocalDate.now();
        Map<String, List<EventStore.Ev>> events = EventStore.byDate(this);

        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, 0, 1);
            rlp.bottomMargin = 1;
            grid.addView(row, rlp);
            for (int c = 0; c < 7; c++) {
                LocalDate d = start.plusDays(r * 7L + c);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -1, 1);
                clp.rightMargin = 1;
                row.addView(cell(d, today, events.get(d.toString())), clp);
            }
        }
    }

    private View cell(LocalDate d, LocalDate today, List<EventStore.Ev> evs) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setPadding(dp(2), dp(2), dp(2), dp(2));
        boolean isToday = d.equals(today);
        if (isToday) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFFDECEC);
            bg.setStroke(dp(2), RED);
            cell.setBackground(bg);
        } else {
            cell.setBackgroundColor(Color.WHITE);
        }
        boolean inMonth = YearMonth.from(d).equals(shown);
        String hol = Holidays.name(d);

        TextView num = new TextView(this);
        num.setText(String.valueOf(d.getDayOfMonth()));
        num.setTextSize(17);
        if (isToday) num.setTypeface(Typeface.DEFAULT_BOLD);
        int col = (d.getDayOfWeek() == DayOfWeek.SUNDAY || hol != null) ? RED
                : d.getDayOfWeek() == DayOfWeek.SATURDAY ? BLUE : TEXT;
        num.setTextColor(col);
        cell.addView(num);

        if (hol != null) cell.addView(label(hol, Color.WHITE, GREEN));
        if (evs != null) {
            for (EventStore.Ev e : evs) cell.addView(label(e.text, 0xFF1A56C4, 0xFFE3ECFD));
        }
        if (!inMonth) cell.setAlpha(0.35f);
        cell.setOnClickListener(v -> showDay(d));
        return cell;
    }

    private TextView label(String s, int fg, int bgc) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(10.5f);
        t.setTextColor(fg);
        t.setSingleLine(true);
        t.setEllipsize(null);
        t.setPadding(dp(2), 0, dp(1), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgc);
        bg.setCornerRadius(dp(3));
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(2);
        t.setLayoutParams(lp);
        return t;
    }

    // ---- 1日の予定一覧 ----
    private void showDay(LocalDate d) {
        List<EventStore.Ev> list = EventStore.on(this, d);
        String hol = Holidays.name(d);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(12), dp(8));

        final AlertDialog[] holder = new AlertDialog[1];
        if (list.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("予定はありません");
            none.setTextSize(17);
            none.setTextColor(0xFF888888);
            none.setPadding(0, dp(12), 0, dp(12));
            box.addView(none);
        }
        for (EventStore.Ev e : list) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView time = new TextView(this);
            String ts = Prefs.fmtTime(this, e.min);
            time.setText(ts.isEmpty() ? "－" : ts);
            time.setTextSize(17);
            time.setTypeface(Typeface.DEFAULT_BOLD);
            time.setTextColor(TEXT);
            time.setMinWidth(dp(Prefs.is24(this) ? 64 : 96));
            row.addView(time);

            TextView txt = new TextView(this);
            txt.setText(e.text);
            txt.setTextSize(17);
            txt.setTextColor(TEXT);
            row.addView(txt, new LinearLayout.LayoutParams(0, -2, 1));

            TextView del = new TextView(this);
            del.setText("削除");
            del.setTextSize(15);
            del.setTextColor(RED);
            del.setPadding(dp(14), dp(8), dp(8), dp(8));
            del.setOnClickListener(v -> {
                holder[0].dismiss();
                confirmDelete(e, () -> showDay(d));
            });
            row.addView(del);

            row.setOnClickListener(v -> {
                holder[0].dismiss();
                showEdit(e, d);
            });
            box.addView(row);
            View sep = new View(this);
            sep.setBackgroundColor(0xFFEEEEEE);
            box.addView(sep, new LinearLayout.LayoutParams(-1, 1));
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        holder[0] = new AlertDialog.Builder(this)
                .setTitle(d.format(DAY_FMT) + (hol != null ? "　" + hol : ""))
                .setView(sv)
                .setPositiveButton("予定を追加", (x, y) -> showEdit(null, d))
                .setNegativeButton("閉じる", null)
                .create();
        holder[0].show();
    }

    private void confirmDelete(EventStore.Ev e, Runnable after) {
        new AlertDialog.Builder(this)
                .setMessage("「" + e.text + "」を削除しますか？")
                .setPositiveButton("削除する", (x, y) -> {
                    EventStore.delete(this, e.id);
                    render();
                    Notifier.afterChange(this, LocalDate.parse(e.date));
                    after.run();
                })
                .setNegativeButton("やめる", (x, y) -> after.run())
                .setOnCancelListener(x -> after.run())
                .show();
    }

    // ---- 予定の追加・変更 ----
    private void showEdit(EventStore.Ev ev, LocalDate defDate) {
        final boolean isNew = ev == null;
        final LocalDate oldDate = isNew ? null : LocalDate.parse(ev.date);
        final LocalDate[] day = {isNew ? defDate : oldDate};
        final int[] min = {(isNew || ev.min < 0) ? 9 * 60 : ev.min};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(0));

        EditText txt = new EditText(this);
        txt.setHint("予定の内容");
        txt.setTextSize(18);
        txt.setMaxLines(3);
        if (!isNew) txt.setText(ev.text);
        box.addView(txt);

        Button dateB = new Button(this);
        dateB.setAllCaps(false);
        dateB.setTextSize(17);
        dateB.setText("日付　" + day[0].format(FULL_FMT));
        dateB.setOnClickListener(v -> new DatePickerDialog(this, (p, y, m, dd) -> {
            day[0] = LocalDate.of(y, m + 1, dd);
            dateB.setText("日付　" + day[0].format(FULL_FMT));
        }, day[0].getYear(), day[0].getMonthValue() - 1, day[0].getDayOfMonth()).show());
        box.addView(dateB);

        Button timeB = new Button(this);
        timeB.setAllCaps(false);
        timeB.setTextSize(17);
        timeB.setText("開始時間　" + Prefs.fmtTime(this, min[0]));
        timeB.setOnClickListener(v -> new TimePickerDialog(this, (p, hh, mm) -> {
            min[0] = hh * 60 + mm;
            timeB.setText("開始時間　" + Prefs.fmtTime(this, min[0]));
        }, min[0] / 60, min[0] % 60, Prefs.is24(this)).show());
        box.addView(timeB);

        CheckBox noTime = new CheckBox(this);
        noTime.setText("時間を決めない");
        noTime.setTextSize(16);
        noTime.setChecked(!isNew && ev.min < 0);
        timeB.setEnabled(!noTime.isChecked());
        noTime.setOnCheckedChangeListener((cb, on) -> timeB.setEnabled(!on));
        box.addView(noTime);

        AlertDialog.Builder bld = new AlertDialog.Builder(this)
                .setTitle(isNew ? "予定を追加" : "予定を変更")
                .setView(box)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", (x, y) -> showDay(day[0]));
        if (!isNew) {
            bld.setNeutralButton("削除", (x, y) -> confirmDelete(ev, () -> showDay(oldDate)));
        }
        AlertDialog dlg = bld.create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = txt.getText().toString().trim();
            if (TextUtils.isEmpty(s)) {
                txt.setError("内容を入れてください");
                return;
            }
            EventStore.Ev e = new EventStore.Ev();
            e.id = isNew ? System.currentTimeMillis() : ev.id;
            e.date = day[0].toString();
            e.min = noTime.isChecked() ? -1 : min[0];
            e.text = s;
            EventStore.put(this, e);
            dlg.dismiss();
            shown = YearMonth.from(day[0]);
            render();
            Notifier.afterChange(this, oldDate, day[0]);
            showDay(day[0]);
        }));
        if (isNew) {
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dlg.show();
    }
}
