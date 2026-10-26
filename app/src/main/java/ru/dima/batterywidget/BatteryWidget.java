package ru.dima.batterywidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class BatteryWidget extends AppWidgetProvider {

    private static String last = "";

    @Override
    public void onEnabled(Context c) {
        startSvc(c);
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c, c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)), true);
        startSvc(c);
    }

    @Override
    public void onDisabled(Context c) {
        c.stopService(new Intent(c, BatteryService.class));
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            int[] ids = AppWidgetManager.getInstance(c)
                    .getAppWidgetIds(new ComponentName(c, BatteryWidget.class));
            if (ids.length > 0) startSvc(c);
        }
    }

    private static void startSvc(Context c) {
        try {
            c.startForegroundService(new Intent(c, BatteryService.class));
        } catch (Exception ignored) {
        }
    }

    static void render(Context c, Intent b, boolean force) {
        if (b == null) return;

        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int pct = scale > 0 ? level * 100 / scale : level;
        boolean charging = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                == BatteryManager.BATTERY_STATUS_CHARGING;

        int bg;
        int fg = Color.WHITE;
        if (pct > 50) {
            bg = R.drawable.bg_green;
        } else if (pct >= 15) {
            bg = R.drawable.bg_yellow;
            fg = 0xDD000000;
        } else {
            bg = R.drawable.bg_red;
        }

        String text;
        float size;
        if (charging) {
            text = chargeText(c, pct);
            size = 16;
        } else {
            text = pct + "%";
            size = 30;
        }

        String key = bg + "|" + text;
        if (!force && key.equals(last)) return;
        last = key;

        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget);
        rv.setInt(R.id.root, "setBackgroundResource", bg);
        rv.setTextViewText(R.id.txt, text);
        rv.setTextColor(R.id.txt, fg);
        rv.setTextViewTextSize(R.id.txt, TypedValue.COMPLEX_UNIT_SP, size);
        AppWidgetManager.getInstance(c)
                .updateAppWidget(new ComponentName(c, BatteryWidget.class), rv);
    }

    private static String chargeText(Context c, int pct) {
        BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
        long cur = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        long cnt = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

        String l1 = "— мА";
        String l2 = "~—";
        if (cur != Long.MIN_VALUE && cur != 0) {
            cur = Math.abs(cur);
            if (cur >= 20000) cur /= 1000;          // мкА -> мА
            if (cur > 0) {
                l1 = cur + " мА";
                if (cnt > 0 && pct > 0) {
                    if (cnt >= 20000) cnt /= 1000;  // мкА·ч -> мА·ч
                    double full = cnt * 100.0 / pct;
                    int min = (int) Math.round((full - cnt) / cur * 60);
                    l2 = "~" + fmt(min);
                }
            }
        }
        return l1 + "\n" + l2;
    }

    private static String fmt(int min) {
        if (min < 1) return "<1м";
        if (min > 1440) return "—";
        int h = min / 60, m = min % 60;
        return h > 0 ? h + "ч " + m + "м" : m + "м";
    }
}
