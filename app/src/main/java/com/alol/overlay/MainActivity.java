package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    private static final int REQUEST_CODE = 101;
    private String gamePackage = "com.rekoo.pubgm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // واجهة تحكم بسيطة
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Shadow Overlay - Game Launcher");
        info.setTextSize(18);
        layout.addView(info);

        // زر تشغيل اللعبة
        Button launchBtn = new Button(this);
        launchBtn.setText("Start Game");
        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });
        layout.addView(launchBtn);

        setContentView(layout);

        // فحص صلاحية النافذة العائمة أولاً
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
                return;
            }
        }

        // تشغيل الخدمة الخلفية
        startBackgroundService();
    }

    private void startGame() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(gamePackage);
        if (launchIntent != null) {
            startActivity(launchIntent);
            // إخفاء تطبيقنا لرؤية اللعبة
            moveTaskToBack(true);
        } else {
            Toast.makeText(this, "Game not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Settings.canDrawOverlays(this)) {
                startBackgroundService();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
