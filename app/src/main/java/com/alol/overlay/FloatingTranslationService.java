package com.alol.overlay;

import android.app.Service;
import android.content.Intent;
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

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            overlayView = new TextView(this);
            overlayView.setText("✅ النافذة العائمة تعمل");
            overlayView.setTextColor(Color.WHITE);
            overlayView.setBackgroundColor(Color.argb(180, 0, 0, 0));
            overlayView.setPadding(20, 10, 20, 10);
            overlayView.setTextSize(15);

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 50;
            params.y = 200;
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (overlayView != null) windowManager.removeView(overlayView);
        } catch (Exception e) {}
        super.onDestroy();
    }
}
