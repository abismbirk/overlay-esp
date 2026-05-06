package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
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

import dev.rikka.shizuku.Shizuku;
import dev.rikka.shizuku.ShizukuProvider;

public class MainActivity extends Activity {
    private static final int OVERLAY_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Hawk Eye Protocol Ready");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Activate Hawk Eye");
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

        // 2. صلاحية Shizuku
        if (!Shizuku.isPreV11() || !Shizuku.checkSelfPermission()) {
            Shizuku.requestPermission(0);
            Toast.makeText(this, "Please grant Shizuku permission", Toast.LENGTH_LONG).show();
        } else {
            startServices();
        }
    }

    private void startServices() {
        Intent bgIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(bgIntent);
        } else {
            startService(bgIntent);
        }

        Intent hawkIntent = new Intent(this, HawkEyeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(hawkIntent);
        } else {
            startService(hawkIntent);
        }

        Toast.makeText(this, "Hawk Eye is watching...", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                requestAllPermissions();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
