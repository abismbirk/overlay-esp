package com.alol.overlay;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

public class VirtualContainer {
    private static final String TAG = "VirtualContainer";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int width, height, density;
    private SurfaceView surfaceView;
    private NeuralOverlayer neuralOverlayer;

    public VirtualContainer(SurfaceView surface, MediaProjectionManager mpManager, Intent data) {
        this.surfaceView = surface;
        HandlerThread thread = new HandlerThread("VirtualContainer");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        mediaProjection = mpManager.getMediaProjection(-1, data);
        WindowManager wm = (WindowManager) surface.getContext().getSystemService(WindowManager.class);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        neuralOverlayer = new NeuralOverlayer();

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                neuralOverlayer.processFrame(image, width, height);
                image.close();
            }
        }, backgroundHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay("VirtualContainer",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR |
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface.getHolder().getSurface(), null);
        Log.d(TAG, "VirtualContainer initialized");
    }

    public void stop() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        if (neuralOverlayer != null) neuralOverlayer.close();
    }
}
