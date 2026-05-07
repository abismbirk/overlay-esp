package com.alol.overlay;

import android.app.Activity;
import android.app.AlertDialog;
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

        // اعتراض أي كراش وعرضه
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringBuilder sb = new StringBuilder();
                sb.append(e.toString() + "\n\n");
                for (StackTraceElement ste : e.getStackTrace()) {
                    sb.append(ste.toString() + "\n");
                }
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Crash Report")
                    .setMessage(sb.toString())
                    .setPositiveButton("OK", (d, w) -> System.exit(1))
                    .show();
                if (oldHandler != null) oldHandler.uncaughtException(t, e);
            }
        });

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
                try {
                    parasite = new SystemUIParasite(this);
                } catch (Exception e) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(e.toString() + "\n\n");
                    for (StackTraceElement ste : e.getStackTrace()) {
                        sb.append(ste.toString() + "\n");
                    }
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(sb.toString())
                        .setPositiveButton("OK", null)
                        .show();
                }
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
