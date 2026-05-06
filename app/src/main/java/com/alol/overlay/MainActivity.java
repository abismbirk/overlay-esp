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
    private static Intent screenCaptureData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("AI ESP - Omega Protocol");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Activate AI ESP");
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_CODE);
            return;
        }
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpManager.createScreenCaptureIntent(), SCREEN_CAPTURE_CODE);
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
        } else if (requestCode == SCREEN_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            screenCaptureData = data;
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

        Intent aiIntent = new Intent(this, AIDetectionService.class);
        aiIntent.putExtra("data", screenCaptureData);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(aiIntent);
        } else {
            startService(aiIntent);
        }

        Toast.makeText(this, "AI ESP is ALIVE!", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }
}
