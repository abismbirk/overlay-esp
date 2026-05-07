package com.alol.overlay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private static final int REQUEST_VPN = 1002;
    private static final int REQUEST_OVERLAY = 1003;
    private static final int REQUEST_USAGE_STATS = 1004;

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
        requestMediaProjection();
    }

    public void onEtaClick(View v) {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN);
        } else {
            startVpnService();
        }
    }

    public void onVseClick(View v) {
        checkOverlayPermission();
    }

    public void onRavClick(View v) {
        Toast.makeText(this, "🛡️ جارٍ تفعيل الحماية الذاتية...", Toast.LENGTH_SHORT).show();
    }

    public void onUedClick(View v) {
        Toast.makeText(this, "📚 جارٍ تحميل قاعدة بيانات الثغرات...", Toast.LENGTH_SHORT).show();
    }

    public void onIgaClick(View v) {
        Toast.makeText(this, "🧠 جارٍ تشغيل المساعد الذكي...", Toast.LENGTH_SHORT).show();
    }

    public void onDaeClick(View v) {
        Toast.makeText(this, "💰 جارٍ فتح سوق الظل...", Toast.LENGTH_SHORT).show();
    }

    public void onCbgClick(View v) {
        Toast.makeText(this, "🤖 جارٍ تشغيل مصنع العباقرة...", Toast.LENGTH_SHORT).show();
    }

    public void onPerClick(View v) {
        Toast.makeText(this, "💓 جارٍ تحليل المشاعر...", Toast.LENGTH_SHORT).show();
    }

    public void onIbpClick(View v) {
        Toast.makeText(this, "♾️ جارٍ تفعيل بروتوكول الخلود...", Toast.LENGTH_SHORT).show();
    }

    // ========== منطق الأذونات والخدمات ==========

    private void requestMediaProjection() {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mpManager != null) {
            startActivityForResult(mpManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, PacketInterceptor.class);
        startService(intent);
        Toast.makeText(this, "🌐 تم بدء اعتراض الحزم", Toast.LENGTH_SHORT).show();
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        } else {
            startVirtualSandbox();
        }
    }

    private void startVirtualSandbox() {
        Toast.makeText(this, "🧪 بيئة الاختبار الافتراضية جاهزة", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            // بدء خدمة المرآة
            Intent serviceIntent = new Intent(this, PhantomMirrorService.class);
            serviceIntent.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "👁️ تم تفعيل العين الإلهية", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_VPN && resultCode == Activity.RESULT_OK) {
            startVpnService();
        }
    }
}
