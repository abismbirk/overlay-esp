package com.alol.overlay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingTranslationService extends Service {
    private WindowManager windowManager;
    private TextView overlayView;
    private BroadcastReceiver receiver;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlay();
        registerTranslationReceiver();
    }

    private void createOverlay() {
        overlayView = new TextView(this);
        overlayView.setText("Waiting for translation...");
        overlayView.setTextColor(Color.WHITE);
        overlayView.setBackgroundColor(Color.argb(180, 0, 0, 0));
        overlayView.setPadding(16, 8, 16, 8);
        overlayView.setTextSize(14);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        windowManager.addView(overlayView, params);
    }

    private void registerTranslationReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = intent.getStringExtra("text");
                if (text != null && overlayView != null) {
                    overlayView.setText(text);
                }
            }
        };
        registerReceiver(receiver, new IntentFilter("SHADOW_TRANSLATION"));
    }

    @Override
    public void onDestroy() {
        if (receiver != null) unregisterReceiver(receiver);
        if (overlayView != null) windowManager.removeView(overlayView);
        super.onDestroy();
    }
}
