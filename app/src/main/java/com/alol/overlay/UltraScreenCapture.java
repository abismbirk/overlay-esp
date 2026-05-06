package com.alol.overlay;

import android.app.Service;
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
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import dev.rikka.shizuku.Shizuku;

public class UltraScreenCapture extends Service {
    private static final String TAG = "UltraCapture";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int screenWidth, screenHeight, screenDensity;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("UltraCapture");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        prepareProjection();
    }

    private void prepareProjection() {
        // صلاحية Shizuku مطلوبة لاستخدام MediaProjection دون نافذة منبثقة
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mpManager.createScreenCaptureIntent();
        
        // استخدام Shizuku لبدء الالتقاط بصمت
        Shizuku.startActivityForResult(intent, 0, null, (requestCode, resultCode, data) -> {
            if (data != null) {
                mediaProjection = mpManager.getMediaProjection(resultCode, data);
                startCapture();
            }
        });
    }

    private void startCapture() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("UltraCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, backgroundHandler);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    // سيتم إرسال الصورة لاحقًا إلى محرك الذكاء الاصطناعي
                    Log.d(TAG, "Frame captured silently...");
                    image.close();
                }
            }
        }, backgroundHandler);
    }

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        super.onDestroy();
    }
}
