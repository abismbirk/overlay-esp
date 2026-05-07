package com.alol.overlay;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class LiveOffsetActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ListView lvData;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_offset);

        tvStatus = findViewById(R.id.tv_status);
        lvData = findViewById(R.id.lv_data);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        lvData.setAdapter(adapter);

        findViewById(R.id.btn_start_server).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShadowServer.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            tvStatus.setText("🟢 Server Active | Port: 50051");
            Toast.makeText(this, "Server started. Waiting for agent...", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_copy_all).setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (String s : dataList) sb.append(s).append("\n");
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("offsets", sb.toString()));
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
        });
    }

    public void addData(String line) {
        runOnUiThread(() -> {
            dataList.add(line);
            adapter.notifyDataSetChanged();
            lvData.smoothScrollToPosition(dataList.size() - 1);
        });
    }
}
