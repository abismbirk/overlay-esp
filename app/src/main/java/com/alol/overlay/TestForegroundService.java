package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class TestForegroundService extends Service {
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = "test_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "اختبار", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, channelId)
                .setContentTitle("الخدمة تعمل")
                .setContentText("تم بدء الخدمة بنجاح")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }
}
