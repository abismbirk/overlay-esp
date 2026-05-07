package com.alol.overlay;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.WindowManager;

public class SystemUIParasite {
    private VirtualDisplay phantomDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int width, height, density;

    public SystemUIParasite(Context context, SurfaceView surfaceView) {
        HandlerThread thread = new HandlerThread("Phantom");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                // هنا سيتم تغذية الذكاء الاصطناعي لاحقاً
                image.close();
            }
        }, backgroundHandler);

        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        phantomDisplay = dm.createVirtualDisplay("Phantom", width, height, density,
                surfaceView.getHolder().getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR);
    }

    public void stop() {
        if (phantomDisplay != null) phantomDisplay.release();
        if (imageReader != null) imageReader.close();
    }
}
