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
        info.setText("Observer Protocol Ready");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Start Observation");
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, PacketInterceptor.class);
            startService(intent);
        });
        layout.addView(startBtn);

        setContentView(layout);
    }
}
