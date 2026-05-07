package com.alol.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class RadiantEyeService extends AccessibilityService {
    private static final String TAG = "RadiantEye";
    public static boolean isRunning = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaProjectionManager mpManager;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isRunning = true;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
        Log.d(TAG, "Radiant Eye activated");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null && event.getPackageName().toString().contains("pubgm")) {
            // هنا يتم التقاط إطار الشاشة عند حدوث حدث معين
            Log.d(TAG, "Game event detected: " + event.getEventType());
        }
    }

    @Override
    public void onInterrupt() {
        isRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
