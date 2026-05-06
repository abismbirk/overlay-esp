package com.alol.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class SpyEyeService extends AccessibilityService {
    private static final String TAG = "ShadowWeaver";
    public static boolean isRunning = false;
    public static SpyEyeService instance = null;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isRunning = true;
        instance = this;
        Log.d(TAG, "Shadow Weaver Eye activated");

        // تهيئة الخدمة لجمع كل الأحداث
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK; // كل الأحداث
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                     AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        }
        info.notificationTimeout = 50; // 50ms فقط – استجابة خاطفة
        setServiceInfo(info);

        // بدء خدمة الخلفية تلقائياً عند تشغيل العين
        Intent bgIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(bgIntent);
        } else {
            startService(bgIntent);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // هنا سنلتقط لقطات الشاشة ونحلل شجرة العرض لاحقاً
        // حالياً، نراقب دخول اللعبة
        if (event.getPackageName() != null && event.getPackageName().toString().contains("rekoo.pubgm")) {
            Log.d(TAG, "Player is inside PUBG");
            // التقاط الشاشة يحدث هنا في المرحلة التالية
        }
    }

    @Override
    public void onInterrupt() {
        isRunning = false;
        instance = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        instance = null;
    }
}
