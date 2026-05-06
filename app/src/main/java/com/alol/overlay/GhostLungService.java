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
import android.os.Build;
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
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class GhostLungService extends Service {
    private static final String TAG = "GhostLung";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int screenWidth, screenHeight, screenDensity;
    private boolean opencvLoaded = false;
    Mat mRgba;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("GhostLung");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        // تحميل OpenCV
        if (OpenCVLoader.initDebug()) {
            opencvLoaded = true;
            Log.d(TAG, "FireBrain activated!");
        }
    }

    public void startProjection(Intent data) {
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(-1, data);

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("GhostLung",
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

    private void processImage(Image image) {
        // تحويل الصورة إلى Mat
        int w = image.getWidth();
        int h = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        java.nio.ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Mat matRgba = new Mat(h, w, CvType.CV_8UC4);
        matRgba.put(0, 0, bytes);

        // كشف الألوان – الأحمر للإنسان، الأخضر للبوت
        Mat redMask = new Mat();
        Mat greenMask = new Mat();

        Core.inRange(matRgba, new Scalar(200, 0, 0, 0), new Scalar(255, 100, 100, 255), redMask);
        Core.inRange(matRgba, new Scalar(0, 200, 0, 0), new Scalar(100, 255, 100, 255), greenMask);

        // إعادة الإحداثيات إلى BackgroundService
        foundTargets(redMask, false);
        foundTargets(greenMask, true);

        redMask.release();
        greenMask.release();
        matRgba.release();
    }

    private void foundTargets(Mat mask, boolean isBot) {
        // هنا يمكن تحويل البكسلات إلى إحداثيات وإرسالها إلى OverlayView للرسم
        Mat points = new Mat();
        Core.findNonZero(mask, points);
        double total = points.total();
        if (total > 10) {
            double avgX = 0, avgY = 0;
            for (int i = 0; i < total; i++) {
                double[] p = points.get(i, 0);
                avgX += p[0];
                avgY += p[1];
            }
            avgX /= total;
            avgY /= total;

            synchronized (BackgroundService.lock) {
                BackgroundService.players.clear();
                BackgroundService.Player player = new BackgroundService.Player();
                player.x = (float) avgX / screenWidth;
                player.y = (float) avgY / screenHeight;
                player.isBot = isBot;
                player.name = isBot ? "BOT" : "ENEMY";
                player.health = 100.0f;
                player.distance = 0.0f;
                BackgroundService.players.add(player);
            }
        }
        points.release();
    }

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        super.onDestroy();
    }
}
