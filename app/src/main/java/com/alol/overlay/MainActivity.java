package com.alol.overlay;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Shadow Translator Ready");
        tv.setTextSize(20);
        tv.setGravity(Gravity.CENTER);
        setContentView(tv);
    }
}
