package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

import dev.rikka.shizuku.Shizuku;
import dev.rikka.shizuku.ShizukuBinderWrapper;

public class HawkEyeService extends Service {
    private static final String TAG = "HawkEye";
    private Process screenrecordProcess;
    private boolean running = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        requestShizukuPermission();
    }

    private void startForegroundService() {
        String channelId = "hawk_eye_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "HawkEye",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        startForeground(5, new Notification.Builder(this, channelId)
                .setContentTitle("Hawk Eye Active")
                .setContentText("Video stream capturing...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pending)
                .build());
    }

    private void requestShizukuPermission() {
        if (!Shizuku.isPreV11() || !Shizuku.checkSelfPermission()) {
            Shizuku.requestPermission(0);
            Log.d(TAG, "Requesting Shizuku permission...");
        } else {
            startScreenCapture();
        }
    }

    private void startScreenCapture() {
        running = true;
        new Thread(() -> {
            try {
                // أمر screenrecord يلتقط الشاشة ويرسل دفق h264 إلى stdout
                ProcessBuilder pb = new ProcessBuilder("screenrecord", "--output-format=h264", "-");
                screenrecordProcess = pb.start();
                InputStream videoStream = screenrecordProcess.getInputStream();
                // هنا سنقوم بفك تشفير الدفق باستخدام MediaCodec
                // وسنغذي الإطارات إلى TensorFlow Lite
                byte[] buffer = new byte[65536];
                int len;
                while (running && (len = videoStream.read(buffer)) != -1) {
                    // معالجة الفيديو...
                }
            } catch (Exception e) {
                Log.e(TAG, "Screen capture error", e);
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (screenrecordProcess != null) {
            screenrecordProcess.destroy();
        }
        super.onDestroy();
    }
}
