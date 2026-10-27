package ru.dima.batterywidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Держит живые приёмники (в манифесте их регистрировать нельзя):
 * - ACTION_BATTERY_CHANGED: обновляет виджет (пока экран выключен, render() ничего не делает)
 * - ACTION_SCREEN_ON: сразу обновляет виджет при включении экрана
 * Плюс один разовый отложенный вызов, чтобы через 10 сек после подключения зарядки
 * молния сменилась значениями. Никакого периодического опроса.
 */
public class BatteryService extends Service {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver batteryRcv;
    private BroadcastReceiver screenRcv;
    private Notification note;

    private final Runnable settleRefresh = new Runnable() {
        @Override
        public void run() {
            BatteryWidget.render(BatteryService.this, stickyBattery(), false);
        }
    };

    private Intent stickyBattery() {
        return registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(
                new NotificationChannel("bat", "Виджет батареи", NotificationManager.IMPORTANCE_MIN));
        note = new Notification.Builder(this, "bat")
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle("Виджет батареи")
                .build();

        batteryRcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                BatteryWidget.render(c, i, false);
                long left = BatteryWidget.settleLeft();
                handler.removeCallbacks(settleRefresh);
                if (left > 0) handler.postDelayed(settleRefresh, left + 300);
            }
        };
        screenRcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                BatteryWidget.render(c, stickyBattery(), true);
            }
        };
        registerReceiver(batteryRcv, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(screenRcv, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, note);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(batteryRcv);
            unregisterReceiver(screenRcv);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }
}
