package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {

    private WindowManager wm;
    private TextView v;
    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
                return;
            }
        }

        showOverlay();
        // لا تستدع finish() هنا
    }

    private void showOverlay() {
        if (wm != null) return; // تجنب الإضافة المزدوجة

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        v = new TextView(this);
        v.setText("OK");
        v.setTextColor(0xFF00FF00);
        v.setBackgroundColor(0x88000000);
        v.setPadding(20, 10, 20, 10);

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

        wm.addView(v, params);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Settings.canDrawOverlays(this)) {
                showOverlay();
                return;
            }
        }
        // إذا لم يتم منح الإذن، أغلق التطبيق
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (v != null && wm != null) {
            try {
                wm.removeView(v);
            } catch (Exception ignored) {}
        }
        v = null;
        wm = null;
    }
}
