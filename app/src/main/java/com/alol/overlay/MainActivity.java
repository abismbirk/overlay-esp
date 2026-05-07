package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private static final int REQUEST_VPN = 1002;
    private static final int REQUEST_OVERLAY = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط الأزرار
        findViewById(R.id.btn_acvs).setOnClickListener(this::onAcvsClick);
        findViewById(R.id.btn_eta).setOnClickListener(this::onEtaClick);
        findViewById(R.id.btn_vse).setOnClickListener(this::onVseClick);
        findViewById(R.id.btn_rav).setOnClickListener(this::onRavClick);
        findViewById(R.id.btn_ued).setOnClickListener(this::onUedClick);
        findViewById(R.id.btn_iga).setOnClickListener(this::onIgaClick);
        findViewById(R.id.btn_dae).setOnClickListener(this::onDaeClick);
        findViewById(R.id.btn_cbg).setOnClickListener(this::onCbgClick);
        findViewById(R.id.btn_per).setOnClickListener(this::onPerClick);
        findViewById(R.id.btn_ibp).setOnClickListener(this::onIbpClick);
    }

    // ========== معالجات الأزرار ==========

    public void onAcvsClick(View v) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mpManager != null) {
            startActivityForResult(mpManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }
    }

    public void onEtaClick(View v) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN);
        } else {
            startService(new Intent(this, PacketInterceptor.class));
            Toast.makeText(this, "🌐 ETA Activated", Toast.LENGTH_SHORT).show();
        }
    }

    public void onVseClick(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        } else {
            Toast.makeText(this, "🧪 VSE Ready", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRavClick(View v) {
        Toast.makeText(this, "🛡️ Self-Healing Armor Active", Toast.LENGTH_SHORT).show();
    }

    public void onUedClick(View v) {
        Toast.makeText(this, "📚 Accessing Universal Exploit Database...", Toast.LENGTH_SHORT).show();
    }

    public void onIgaClick(View v) {
        Toast.makeText(this, "🧠 Intelligent Game Assistant is listening...", Toast.LENGTH_SHORT).show();
    }

    public void onDaeClick(View v) {
        Toast.makeText(this, "💰 Entering Shadow Asset Exchange...", Toast.LENGTH_SHORT).show();
    }

    public void onCbgClick(View v) {
        Toast.makeText(this, "🤖 Custom Persona Factory booting up...", Toast.LENGTH_SHORT).show();
    }

    public void onPerClick(View v) {
        Toast.makeText(this, "💓 Scanning vitals...", Toast.LENGTH_SHORT).show();
    }

    public void onIbpClick(View v) {
        Toast.makeText(this, "♾️ Quantum Backup Initialized", Toast.LENGTH_SHORT).show();
    }

    // ========== نتائج الأذونات ==========

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            Intent serviceIntent = new Intent(this, PhantomMirrorService.class);
            serviceIntent.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "👁️ Phantom Mirror Activated", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_VPN && resultCode == Activity.RESULT_OK) {
            startService(new Intent(this, PacketInterceptor.class));
            Toast.makeText(this, "🌐 VPN Interceptor Started", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_OVERLAY) {
            Toast.makeText(this, "🧪 VSE Permission Granted", Toast.LENGTH_SHORT).show();
        }
    }
}
