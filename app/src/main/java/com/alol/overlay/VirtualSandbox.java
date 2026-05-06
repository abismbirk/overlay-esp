package com.alol.overlay;

import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.WindowManager;

public class VirtualSandbox {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int width, height, density;
    private SurfaceView surfaceView;
    private EternalVision aiVision;

    public VirtualSandbox(SurfaceView surface, MediaProjectionManager mpManager, Intent data) {
        this.surfaceView = surface;
        HandlerThread thread = new HandlerThread("Sandbox");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());
        aiVision = new EternalVision();

        mediaProjection = mpManager.getMediaProjection(-1, data);
        WindowManager wm = (WindowManager) surface.getContext().getSystemService(Service.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                // معالجة AI
                java.util.List<float[]> detections = aiVision.detect(image);
                // إرسال النتائج إلى SpectralOverlay
                SpectralOverlay.updateTargets(detections, width, height);
                image.close();
            }
        }, backgroundHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay("VirtualSandbox",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface.getHolder().getSurface(), null);
    }

    public void stop() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        if (aiVision != null) aiVision.close();
    }
}
