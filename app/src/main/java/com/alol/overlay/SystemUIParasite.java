package com.alol.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.WindowManager;

public class SystemUIParasite {
    private MediaProjection mediaProjection;
    private VirtualDisplay maelstromDisplay;
    private SurfaceView invisibleSurface;
    private WindowManager windowManager;
    private int width, height, density;

    public SystemUIParasite(Context context, Intent data) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        // إنشاء نافذة غير مرئية بحجم 1x1 بكسل كحامل للسطح
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        invisibleSurface = new SurfaceView(context);
        windowManager.addView(invisibleSurface, params);

        MediaProjectionManager mpManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(-1, data);

        maelstromDisplay = mediaProjection.createVirtualDisplay("Maelstrom", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                invisibleSurface.getHolder().getSurface(), null, null);
    }

    public void stop() {
        if (maelstromDisplay != null) maelstromDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (invisibleSurface != null) windowManager.removeView(invisibleSurface);
    }
}
