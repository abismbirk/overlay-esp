package com.alol.overlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private static final int REQUEST_VPN = 1002;
    private static final int REQUEST_OVERLAY = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // --- معالج الكراش العالمي ---
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                StringBuilder sb = new StringBuilder();
                sb.append(throwable.toString()).append("\n\n");
                for (StackTraceElement ste : throwable.getStackTrace()) {
                    sb.append(ste.toString()).append("\n");
                }
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Crash Report")
                    .setMessage(sb.toString())
                    .setPositiveButton("Close", (d,w)-> System.exit(1))
                    .show();
                if (oldHandler != null) oldHandler.uncaughtException(thread, throwable);
            }
        });
        // -------------------------
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_acvs).setOnClickListener(this::onAcvsClick);
        findViewById(R.id.btn_eta).setOnClickListener(this::onEtaClick);
        findViewById(R.id.btn_vse).setOnClickListener(this::onVseClick);
        findViewById(R.id.btn_tva).setOnClickListener(v -> startActivity(new Intent(this, LiveOffsetActivity.class)));

        // باقي الأزرار (اختصار)
        int[] ids = {R.id.btn_rav, R.id.btn_ued, R.id.btn_iga, R.id.btn_dae, R.id.btn_cbg, R.id.btn_per, R.id.btn_ibp};
        String[] msgs = {"🛡️ Activated", "📚 Accessing...", "🧠 Assistant...", "💰 Market...", "🤖 Factory...", "💓 Vitals...", "♾️ Backup..."};
        for (int i=0; i<ids.length; i++) {
            final String msg = msgs[i];
            findViewById(ids[i]).setOnClickListener(v -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        }
    }

    public void onAcvsClick(View v) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mpManager != null) startActivityForResult(mpManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    public void onEtaClick(View v) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) startActivityForResult(intent, REQUEST_VPN);
        else { startService(new Intent(this, PacketInterceptor.class)); Toast.makeText(this, "🌐 ETA Activated", Toast.LENGTH_SHORT).show(); }
    }

    public void onVseClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        } else Toast.makeText(this, "🧪 VSE Ready", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            Intent serviceIntent = new Intent(this, PhantomMirrorService.class);
            serviceIntent.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
            else startService(serviceIntent);
            Toast.makeText(this, "👁️ Phantom Mirror Activated", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_VPN && resultCode == Activity.RESULT_OK) {
            startService(new Intent(this, PacketInterceptor.class));
            Toast.makeText(this, "🌐 VPN Interceptor Started", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_OVERLAY) {
            Toast.makeText(this, "🧪 VSE Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }
}
