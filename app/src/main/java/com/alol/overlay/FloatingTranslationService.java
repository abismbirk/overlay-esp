package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingTranslationService extends Service {
    private WindowManager windowManager;
    private TextView overlayView;
    private BroadcastReceiver receiver;
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            createOverlay();
            registerTranslationReceiver();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void startForegroundService() {
        String channelId = "floating_translator";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "الترجمة الفورية", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, PermissionActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, channelId)
                .setContentTitle("المترجم يعمل")
                .setContentText("جاري ترجمة الصوت...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pending)
                .build());
    }

    private void createOverlay() {
        overlayView = new TextView(this);
        overlayView.setText("في انتظار الترجمة...");
        overlayView.setTextColor(Color.WHITE);
        overlayView.setBackgroundColor(Color.argb(200, 0, 0, 0));
        overlayView.setPadding(20, 12, 20, 12);
        overlayView.setTextSize(15);
        overlayView.setMaxLines(4);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        // جعل النافذة قابلة للسحب
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = (int) (initialX + event.getRawX() - initialTouchX);
                        params.y = (int) (initialY + event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

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
        try {
            if (receiver != null) unregisterReceiver(receiver);
            if (overlayView != null) windowManager.removeView(overlayView);
        } catch (Exception e) {}
        super.onDestroy();
    }
}
