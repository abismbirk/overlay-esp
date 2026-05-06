package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int OVERLAY_CODE = 101;
    private static final int SCREEN_CAPTURE_CODE = 102;
    private VirtualContainer container;
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Neutronium Protocol Ready");
        info.setTextSize(18);
        layout.addView(info);

        surfaceView = new SurfaceView(this);
        layout.addView(surfaceView, 800, 600);

        Button startBtn = new Button(this);
        startBtn.setText("Activate");
        startBtn.setOnClickListener(v -> requestAllPermissions());
        layout.addView(startBtn);

        setContentView(layout);
        SpectralOverlay.init(surfaceView);
        OmniTouch.init();
    }

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_CODE);
            return;
        }
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpManager.createScreenCaptureIntent(), SCREEN_CAPTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_CODE && Settings.canDrawOverlays(this)) {
            requestAllPermissions();
        } else if (requestCode == SCREEN_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            container = new VirtualContainer(surfaceView, mpManager, data);
            Toast.makeText(this, "Neutronium Core Active", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (container != null) container.stop();
        super.onDestroy();
    }
}
