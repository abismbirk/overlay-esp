package com.alol.overlay;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.content.Context;

public class SystemUIParasite {
    private VirtualDisplay siegeDisplay;
    private ImageReader siegeReader;
    private Handler siegeHandler;
    private int width, height, density;
    private NeuralEngine neuralEngine;

    public SystemUIParasite(Context context) {
        HandlerThread thread = new HandlerThread("SiegeThread");
        thread.start();
        siegeHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        neuralEngine = new NeuralEngine();
        siegeReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
        siegeReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                neuralEngine.processFrame(image, width, height);
                image.close();
            }
        }, siegeHandler);

        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        siegeDisplay = dm.createVirtualDisplay("SystemUISiege", width, height, density,
                siegeReader.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR |
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
    }

    public void stop() {
        if (siegeDisplay != null) siegeDisplay.release();
        if (siegeReader != null) siegeReader.close();
        if (neuralEngine != null) neuralEngine.close();
    }
}
