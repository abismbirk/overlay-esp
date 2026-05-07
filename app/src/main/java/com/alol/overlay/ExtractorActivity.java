package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExtractorActivity extends AppCompatActivity {

    private static final int PICK_FILE = 2001;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private ListView lvOffsets;
    private Button btnStart, btnExport;
    private ArrayAdapter<String> adapter;
    private List<String> displayList = new ArrayList<>();
    private List<OffsetItem> offsetList = new ArrayList<>();
    private String selectedFilePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extractor);

        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progressBar);
        lvOffsets = findViewById(R.id.lv_offsets);
        btnStart = findViewById(R.id.btn_start_analysis);
        btnExport = findViewById(R.id.btn_export_json);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvOffsets.setAdapter(adapter);

        findViewById(R.id.btn_select_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE);
        });

        btnStart.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                startAnalysis();
            } else {
                Toast.makeText(this, "Select libanogs.so first", Toast.LENGTH_SHORT).show();
            }
        });

        btnExport.setOnClickListener(v -> exportToJson());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                File tempFile = new File(getCacheDir(), "libanogs_temp.so");
                FileOutputStream fos = new FileOutputStream(tempFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
                is.close();
                fos.close();
                selectedFilePath = tempFile.getAbsolutePath();
                tvStatus.setText("✅ File loaded: " + tempFile.getName());
            } catch (Exception e) {
                tvStatus.setText("❌ Error loading file");
            }
        }
    }

    private void startAnalysis() {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("🔍 Analyzing...");
        btnStart.setEnabled(false);
        offsetList.clear();
        displayList.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            IsolatedLabAnalyzer analyzer = new IsolatedLabAnalyzer(this);
            offsetList.addAll(analyzer.analyzeFile(selectedFilePath));
            for (OffsetItem item : offsetList) {
                displayList.add(item.name + "\nOffset: " + item.offset + " | " + item.threatLevel);
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("✅ Found " + offsetList.size() + " symbols");
                btnStart.setEnabled(true);
                Toast.makeText(this, "Analysis complete!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void exportToJson() {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < offsetList.size(); i++) {
            OffsetItem item = offsetList.get(i);
            json.append("  {\"name\":\"").append(item.name).append("\",")
                .append("\"offset\":\"").append(item.offset).append("\",")
                .append("\"size\":").append(item.size).append(",")
                .append("\"threat\":\"").append(item.threatLevel).append("\"}");
            if (i < offsetList.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        try {
            File file = new File(getExternalFilesDir(null), "offsets_export.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.toString().getBytes());
            fos.close();
            Toast.makeText(this, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    public static class OffsetItem {
        public String name, offset, threatLevel;
        public int size;
        public OffsetItem(String name, String offset, int size, String threat) {
            this.name = name; this.offset = offset; this.size = size; this.threatLevel = threat;
        }
    }
}
