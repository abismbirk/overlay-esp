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

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import dev.rikka.shizuku.Shizuku;

public class ShizukuScreenCapture extends Service {
    private static final String TAG = "ShizukuEye";
    private MediaProjectionManager mpManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int screenWidth, screenHeight, screenDensity;
    private boolean opencvLoaded = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ShizukuEye");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        if (OpenCVLoader.initDebug()) {
            opencvLoaded = true;
            Log.d(TAG, "Shizuku Eye: OpenCV ready.");
        }

        mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        requestScreenCapture();
    }

    private void requestScreenCapture() {
        if (!Shizuku.isPreV11() || !Shizuku.checkSelfPermission()) {
            Log.e(TAG, "Shizuku permission not granted!");
            return;
        }

        try {
            // باستخدام صلاحيات Shizuku، يمكننا بدء MediaProjection دون نافذة منبثقة
            Intent intent = mpManager.createScreenCaptureIntent();
            Shizuku.startActivityForResult(intent, 0, new Shizuku.OnActivityResultListener() {
                @Override
                public void onActivityResult(int requestCode, int resultCode, Intent data) {
                    if (resultCode == RESULT_OK) {
                        mediaProjection = mpManager.getMediaProjection(resultCode, data);
                        startVirtualDisplay();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Shizuku error: " + e.getMessage());
        }
    }

    private void startVirtualDisplay() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("ShizukuEye",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, backgroundHandler);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null && opencvLoaded) {
                    processImage(image);
                    image.close();
                }
            }
        }, backgroundHandler);
    }

    // (يتم تضمين وظائف processImage و detectAndSend هنا)
    private void processImage(Image image) {}
    private void detectAndSend(Mat mask, boolean isBot) {}

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        super.onDestroy();
    }
}
