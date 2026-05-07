package com.alol.overlay;

import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import dev.rikka.shizuku.Shizuku;
import dev.rikka.shizuku.ShizukuRemoteProcess;

public class ShizukuSpectre extends android.app.Service {
    private static final String TAG = "ShizukuSpectre";

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Shizuku.isPreV11() || !Shizuku.checkSelfPermission()) {
            Log.e(TAG, "Shizuku permission not granted!");
            return;
        }
        Log.d(TAG, "Shizuku connected. Starting spectre...");
        // مثال: التقاط logcat تلقائياً
        new Thread(() -> {
            try {
                Process p = ShizukuRemoteProcess.exec("logcat -v time -s ShadowAgent ShadowServer");
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d("ShizukuLogcat", line);
                }
            } catch (Exception e) {
                Log.e(TAG, "logcat error", e);
            }
        }).start();
    }
}
