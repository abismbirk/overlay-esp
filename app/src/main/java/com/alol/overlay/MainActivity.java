package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WindowManager wm;
    private View overlayView;
    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // واجهة بسيطة جدًا
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Overlay Active");
        info.setTextSize(18);
        layout.addView(info);

        Button hideBtn = new Button(this);
        hideBtn.setText("Hide App");
        hideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveTaskToBack(true);
            }
        });
        layout.addView(hideBtn);

        setContentView(layout);

        // فحص الصلاحية
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
                return;
            }
        }

        // بدء النافذة العائمة
        showOverlay();
        // إخفاء النشاط مباشرة لرؤية النافذة
        moveTaskToBack(true);
    }

    private void showOverlay() {
        if (wm != null) return;

        try {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);

            TextView tv = new TextView(this);
            tv.setText("OVERLAY WORKS!");
            tv.setTextColor(0xFF00FF00);
            tv.setBackgroundColor(0xAA000000);
            tv.setPadding(30, 15, 30, 15);
            tv.setTextSize(20);

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
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 100;
            params.y = 200;

            wm.addView(tv, params);
            overlayView = tv;
        } catch (Exception e) {
            Toast.makeText(this, "Overlay error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Settings.canDrawOverlays(this)) {
                showOverlay();
                moveTaskToBack(true);
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (overlayView != null && wm != null) {
            try {
                wm.removeView(overlayView);
            } catch (Exception ignored) {}
        }
        overlayView = null;
        wm = null;
    }
}
