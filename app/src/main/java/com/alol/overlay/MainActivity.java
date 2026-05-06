package com.alol.overlay;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
public class MainActivity extends Activity {
    private WindowManager wm;
    private TextView v;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())),101);return;}
        show();finish();
    }
    void show(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        v=new TextView(this);v.setText("OK");v.setTextColor(0xFF00FF00);v.setBackgroundColor(0x88000000);v.setPadding(20,10,20,10);
        int t=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(-2,-2,t,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.START;p.x=100;p.y=200;wm.addView(v,p);
    }
    @Override
    protected void onActivityResult(int rq,int rc,Intent d){super.onActivityResult(rq,rc,d);if(rq==101&&Build.VERSION.SDK_INT>=23&&Settings.canDrawOverlays(this)){show();}finish();}
    @Override protected void onDestroy(){if(v!=null&&wm!=null)wm.removeView(v);super.onDestroy();}
}
