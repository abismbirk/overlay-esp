package com.alol.overlay;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.util.Log;
import dev.rikka.shizuku.Shizuku;
import dev.rikka.shizuku.ShizukuBinderWrapper;
import android.hardware.input.IInputManager;

public class OmniTouch {
    private static IInputManager im;

    public static boolean init() {
        try {
            im = IInputManager.Stub.asInterface(new ShizukuBinderWrapper(
                    android.os.ServiceManager.getService("input")));
            return true;
        } catch (Exception e) {
            Log.e("OmniTouch", "Shizuku not available", e);
            return false;
        }
    }

    public static void tap(float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_UP, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            im.injectInputEvent(down, 0);
            im.injectInputEvent(up, 0);
        } catch (Exception e) {
            Log.e("OmniTouch", "Failed to inject touch", e);
        }
    }

    public static void swipe(float x1, float y1, float x2, float y2, int steps) {
        long downTime = SystemClock.uptimeMillis();
        float stepX = (x2 - x1) / steps;
        float stepY = (y2 - y1) / steps;
        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x1, y1, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try { im.injectInputEvent(down, 0); } catch (Exception e) {}
        for (int i = 1; i < steps; i++) {
            long moveTime = downTime + 16 * i;
            MotionEvent move = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, x1 + stepX * i, y1 + stepY * i, 0);
            move.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try { im.injectInputEvent(move, 0); } catch (Exception e) {}
        }
        MotionEvent up = MotionEvent.obtain(downTime, downTime + 16 * steps, MotionEvent.ACTION_UP, x1 + stepX * steps, y1 + stepY * steps, 0);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try { im.injectInputEvent(up, 0); } catch (Exception e) {}
    }
}
