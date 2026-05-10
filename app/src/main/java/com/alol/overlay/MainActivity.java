package com.alol.overlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // اختيار اللغات
        TextView tvSource = findViewById(R.id.tv_source_lang);
        TextView tvTarget = findViewById(R.id.tv_target_lang);
        tvSource.setOnClickListener(v -> Toast.makeText(this, "اختيار لغة المصدر (قريباً)", Toast.LENGTH_SHORT).show());
        tvTarget.setOnClickListener(v -> Toast.makeText(this, "اختيار لغة الهدف (قريباً)", Toast.LENGTH_SHORT).show());

        // زر الترجمة الفورية
        findViewById(R.id.btn_live_translate).setOnClickListener(v -> {
            Intent intent = new Intent(this, PermissionActivity.class);
            startActivity(intent);
        });

        // ترجمة ملف صوتي
        findViewById(R.id.btn_file_translate).setOnClickListener(v -> 
            Toast.makeText(this, "📁 ترجمة ملف صوتي (سيتم تفعيلها قريباً)", Toast.LENGTH_SHORT).show());

        // سجل الترجمات
        findViewById(R.id.btn_history).setOnClickListener(v -> {
            try {
                java.io.File file = new java.io.File(getExternalFilesDir(null), "translations.txt");
                if (file.exists()) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                    reader.close();
                    new AlertDialog.Builder(this)
                        .setTitle("📜 سجل الترجمات")
                        .setMessage(sb.toString().isEmpty() ? "لا توجد ترجمات محفوظة" : sb.toString())
                        .setPositiveButton("حسناً", null)
                        .show();
                } else {
                    Toast.makeText(this, "لا يوجد سجل بعد", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح الملف", Toast.LENGTH_SHORT).show();
            }
        });

        // الكلمات المحفوظة
        findViewById(R.id.btn_saved).setOnClickListener(v -> 
            Toast.makeText(this, "⭐ كلمات محفوظة (قريباً مع نظام البطاقات التعليمية)", Toast.LENGTH_SHORT).show());

        // الإعدادات
        findViewById(R.id.btn_settings).setOnClickListener(v -> 
            Toast.makeText(this, "⚙️ الإعدادات (تغيير اللغات، نمط النافذة)", Toast.LENGTH_SHORT).show());
    }
}
