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

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Shadow Overlay Service");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Start Service & Hide");
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBackgroundService();
                moveTaskToBack(true);
            }
        });
        layout.addView(startBtn);

        setContentView(layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
                return;
            }
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
        if (requestCode == REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && Settings.canDrawOverlays(this)) {
            startBackgroundService();
        } else {
            Toast.makeText(this, "Need overlay permission", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
