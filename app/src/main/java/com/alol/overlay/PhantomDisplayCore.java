package com.alol.overlay;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import android.content.Context;

public class PhantomDisplayCore {
    private VirtualDisplay phantomDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int width, height, density;
    private NeuralEngine neuralEngine;

    public PhantomDisplayCore(Context context) {
        HandlerThread thread = new HandlerThread("PhantomDisplay");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        neuralEngine = new NeuralEngine();

        // إنشاء قارئ صور بدون MediaProjection
        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null && neuralEngine != null) {
                neuralEngine.processFrame(image, width, height);
                image.close();
            }
        }, backgroundHandler);

        // إنشاء شاشة افتراضية خفية - السر هنا
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        phantomDisplay = dm.createVirtualDisplay("PhantomDisplay", width, height, density,
                imageReader.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR |
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
    }

    public void stop() {
        if (phantomDisplay != null) phantomDisplay.release();
        if (imageReader != null) imageReader.close();
        if (neuralEngine != null) neuralEngine.close();
    }
}
