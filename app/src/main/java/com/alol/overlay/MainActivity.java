package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_MEDIA_PROJECTION = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // اختيار اللغات (تفعله لاحقًا)
        TextView tvSource = findViewById(R.id.tv_source_lang);
        TextView tvTarget = findViewById(R.id.tv_target_lang);
        tvSource.setOnClickListener(v -> Toast.makeText(this, "Source language picker", Toast.LENGTH_SHORT).show());
        tvTarget.setOnClickListener(v -> Toast.makeText(this, "Target language picker", Toast.LENGTH_SHORT).show());

        // زر الترجمة الفورية
        findViewById(R.id.btn_live_translate).setOnClickListener(v -> {
            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mpManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        });

        // الأزرار الأخرى
        findViewById(R.id.btn_file_translate).setOnClickListener(v -> Toast.makeText(this, "File translation - coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_history).setOnClickListener(v -> Toast.makeText(this, "History - coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_saved).setOnClickListener(v -> Toast.makeText(this, "Saved words - coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_settings).setOnClickListener(v -> Toast.makeText(this, "Settings - coming soon", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            // بدء خدمات الترجمة
            Intent floatIntent = new Intent(this, FloatingTranslationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(floatIntent);
            else startService(floatIntent);

            Intent captureIntent = new Intent(this, AudioCaptureService.class);
            captureIntent.putExtra("data", data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(captureIntent);
            else startService(captureIntent);

            Toast.makeText(this, "🌐 Translation started! Open YouTube.", Toast.LENGTH_LONG).show();
            moveTaskToBack(true);
        }
    }
}
