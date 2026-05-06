package com.alol.overlay;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

public class SurfaceFlingerCore {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int width, height, density;
    private Surface surface;
    private NeuralEngine neuralEngine;

    public SurfaceFlingerCore(MediaProjectionManager mpManager, Intent data, Surface inputSurface) {
        HandlerThread thread = new HandlerThread("SurfaceFlingerCore");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        mediaProjection = mpManager.getMediaProjection(-1, data);
        width = 1080;  // يمكن تعديلها
        height = 1920;
        density = 420;
        this.surface = inputSurface;
        neuralEngine = new NeuralEngine();

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                neuralEngine.processFrame(image, width, height);
                image.close();
            }
        }, backgroundHandler);

        virtualDisplay = mediaProjection.createVirtualDisplay("SurfaceFlingerCore",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null);
    }

    public void stop() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        if (neuralEngine != null) neuralEngine.close();
    }
}
