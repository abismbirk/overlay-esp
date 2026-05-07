package com.alol.overlay;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class SystemUIParasite {
    private MediaProjection mediaProjection;
    private VirtualDisplay maelstromDisplay;
    private int width, height, density;

    public SystemUIParasite(Context context, Intent data) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        MediaProjectionManager mpManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(-1, data);

        maelstromDisplay = mediaProjection.createVirtualDisplay("Maelstrom", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null, null, null);
    }

    public void stop() {
        if (maelstromDisplay != null) maelstromDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
    }
}
