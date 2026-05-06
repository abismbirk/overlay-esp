package com.alol.overlay;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.view.SurfaceView;

public class VirtualEnvironment {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int width, height, density;
    private SurfaceView surfaceView;

    public void initialize(MediaProjectionManager mpManager, Intent data, SurfaceView surface) {
        this.surfaceView = surface;
        mediaProjection = mpManager.getMediaProjection(-1, data);
        width = 1080;  // يمكن تعديل الدقة حسب جهازك
        height = 1920;
        density = 420;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                // تغذية الذكاء الاصطناعي هنا
                image.close();
            }
        }, null);

        virtualDisplay = mediaProjection.createVirtualDisplay("VirtualEnv",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface.getHolder().getSurface(), null);
    }

    public void stop() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
    }
}
