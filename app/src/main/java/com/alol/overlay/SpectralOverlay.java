package com.alol.overlay;

import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SpectralOverlay {
    private static SurfaceView surfaceView;
    private static Canvas canvas;

    public static void init(SurfaceView view) {
        surfaceView = view;
    }

    public static Canvas getCanvas() {
        if (surfaceView != null) {
            SurfaceHolder holder = surfaceView.getHolder();
            canvas = holder.lockCanvas();
        }
        return canvas;
    }

    public static void release() {
        if (canvas != null && surfaceView != null) {
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
            canvas = null;
        }
    }
}
