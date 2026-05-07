package com.alol.overlay;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    private SystemUIParasite parasite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Phantom Core Ready");
        info.setTextSize(18);
        layout.addView(info);

        Button startBtn = new Button(this);
        startBtn.setText("Activate Phantom");
        startBtn.setOnClickListener(v -> {
            if (parasite == null) {
                parasite = new SystemUIParasite(this);
            }
        });
        layout.addView(startBtn);

        setContentView(layout);
    }

    @Override
    protected void onDestroy() {
        if (parasite != null) parasite.stop();
        super.onDestroy();
    }
}
