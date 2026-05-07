package com.alol.overlay.healing;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class SelfHealing implements Thread.UncaughtExceptionHandler {
    private Context ctx;
    public SelfHealing(Context c) {
        ctx = c;
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e("SelfHealing", "Crash detected: " + e.getMessage() + ". Restarting in 1s...");
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        PendingIntent pending = PendingIntent.getActivity(ctx, 123, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pending);
        Process.killProcess(Process.myPid());
    }
}
