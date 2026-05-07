package com.alol.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class PhantomMirrorService extends Service {
    private static final String TAG = "PhantomMirror";
    private MediaProjection mediaProjection;
    private VirtualDisplay phantomDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int width, height, density;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("PhantomMirror");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        density = metrics.densityDpi;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("data")) {
            Intent data = intent.getParcelableExtra("data");
            startMirroring(data);
        }
        return START_STICKY;
    }

    private void startMirroring(Intent data) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(-1, data);
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Log.d(TAG, "Frame captured for ACVS");
                image.close();
            }
        }, backgroundHandler);
        phantomDisplay = mediaProjection.createVirtualDisplay("PhantomMirror",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, backgroundHandler);
    }

    @Override
    public void onDestroy() {
        if (phantomDisplay != null) phantomDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        super.onDestroy();
    }
}
