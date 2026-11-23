package com.kitarou.tokei;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** 時計をタップしたときの画面（秒まで表示） */
public class MainActivity extends Activity {

    private static final int FG = 0xFFE6ECFF;

    private TextView dateV, ampmV, timeV;
    private Button modeB;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy年M月d日(E)", Locale.JAPAN);

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
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
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        dateV = new TextView(this);
        dateV.setTextColor(0xFFC8D0E8);
        dateV.setTextSize(24);
        dateV.setGravity(Gravity.CENTER);
        root.addView(dateV);

        ampmV = new TextView(this);
        ampmV.setTextColor(0xFFC8D0E8);
        ampmV.setTextSize(22);
        ampmV.setGravity(Gravity.CENTER);
        ampmV.setPadding(0, dp(16), 0, 0);
        root.addView(ampmV);

        timeV = new TextView(this);
        timeV.setTextColor(FG);
        timeV.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        timeV.setFontFeatureSettings("tnum");
        timeV.setSingleLine(true);
        timeV.setIncludeFontPadding(false);
        timeV.setGravity(Gravity.CENTER);
        int w = getResources().getDisplayMetrics().widthPixels;
        timeV.setTextSize(TypedValue.COMPLEX_UNIT_PX, w * 0.92f / 4.4f);
        root.addView(timeV, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(56), 0, 0);

        modeB = makeButton("");
        modeB.setOnClickListener(v -> {
            Prefs.set24(this, !Prefs.is24(this));
            ClockWidget.updateAll(this);
            update();
        });
        Button calB = makeButton("カレンダー");
        calB.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(150), dp(56));
        lp.setMargins(dp(10), 0, dp(10), 0);
        row.addView(modeB, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(dp(150), dp(56));
        lp2.setMargins(dp(10), 0, dp(10), 0);
        row.addView(calB, lp2);
        root.addView(row);

        setContentView(root);

        Notifier.schedule(this);
        ClockWidget.updateAll(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTextColor(FG);
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C2030);
        bg.setStroke(dp(1), 0xFF59607A);
        bg.setCornerRadius(dp(28));
        b.setBackground(bg);
        return b;
    }

    private void update() {
        Calendar c = Calendar.getInstance();
        boolean is24 = Prefs.is24(this);
        int hh = c.get(Calendar.HOUR_OF_DAY), mm = c.get(Calendar.MINUTE), ss = c.get(Calendar.SECOND);
        int shown = hh;
        if (!is24) {
            shown = hh % 12;
            if (shown == 0) shown = 12;
            ampmV.setText(hh < 12 ? "午前" : "午後");
            ampmV.setVisibility(View.VISIBLE);
        } else {
            ampmV.setVisibility(View.GONE);
        }
        timeV.setText(String.format(Locale.US, "%d:%02d:%02d", shown, mm, ss));
        dateV.setText(dateFmt.format(c.getTime()));
        modeB.setText(is24 ? "24時間表示" : "12時間表示");
    }

    @Override
    protected void onResume() {
        super.onResume();
        h.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(tick);
    }
}
