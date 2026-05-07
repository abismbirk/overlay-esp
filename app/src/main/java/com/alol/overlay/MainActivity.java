package com.alol.overlay;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    private SystemUIParasite parasite;
    private SurfaceView surfaceView;

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

        surfaceView = new SurfaceView(this);
        surfaceView.setZOrderOnTop(true);
        layout.addView(surfaceView, 800, 600);

        Button startBtn = new Button(this);
        startBtn.setText("Activate Phantom");
        startBtn.setOnClickListener(v -> {
            if (parasite == null) {
                parasite = new SystemUIParasite(this, surfaceView);
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
