package ru.dima.batterywidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/** Держит живой приёмник ACTION_BATTERY_CHANGED (в манифесте его регистрировать нельзя). */
public class BatteryService extends Service {

    private BroadcastReceiver receiver;
    private Notification note;

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

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                BatteryWidget.render(c, i, false);
            }
        };
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, note);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }
}
