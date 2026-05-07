package com.alol.overlay;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Shadow Server Ready");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Start Server");
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShadowServer.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        });
        layout.addView(startBtn);

        setContentView(layout);
    }
}
