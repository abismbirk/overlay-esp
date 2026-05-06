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
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class GhostLungService extends Service {
    private static final String TAG = "GhostLung";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int screenWidth, screenHeight, screenDensity;
    private boolean opencvLoaded = false;

    // نطاقات ألوان تكيفية (سيتم تحديثها بالتحليل المباشر)
    private Scalar lowerRed1 = new Scalar(0, 0, 200, 0);
    private Scalar upperRed1 = new Scalar(80, 80, 255, 255);
    private Scalar lowerRed2 = new Scalar(0, 0, 100, 0);
    private Scalar upperRed2 = new Scalar(50, 50, 255, 255);
    private Scalar lowerGreen = new Scalar(0, 100, 0, 0);
    private Scalar upperGreen = new Scalar(80, 255, 80, 255);
    private Scalar lowerArrowRed = new Scalar(0, 0, 150, 0);
    private Scalar upperArrowRed = new Scalar(100, 100, 255, 255);
    private Scalar lowerArrowGreen = new Scalar(0, 150, 0, 0);
    private Scalar upperArrowGreen = new Scalar(100, 255, 100, 255);

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

        if (OpenCVLoader.initDebug()) {
            opencvLoaded = true;
            Log.d(TAG, "FireBrain V2.0 activated!");
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
        int w = image.getWidth();
        int h = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        java.nio.ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Mat matRgba = new Mat(h, w, CvType.CV_8UC4);
        matRgba.put(0, 0, bytes);

        // تحويل إلى BGR لمعالجة أفضل
        Mat bgr = new Mat();
        Imgproc.cvtColor(matRgba, bgr, Imgproc.COLOR_RGBA2BGR);

        // فلتر لتقليل الضوضاء
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(bgr, blurred, new Size(3, 3), 0);

        // استخراج الأقنعة الملونة
        Mat redMask1 = new Mat();
        Mat redMask2 = new Mat();
        Mat redMask = new Mat();
        Mat greenMask = new Mat();
        Mat arrowRedMask = new Mat();
        Mat arrowGreenMask = new Mat();

        Core.inRange(blurred, lowerRed1, upperRed1, redMask1);
        Core.inRange(blurred, lowerRed2, upperRed2, redMask2);
        Core.bitwise_or(redMask1, redMask2, redMask);
        Core.inRange(blurred, lowerGreen, upperGreen, greenMask);
        Core.inRange(blurred, lowerArrowRed, upperArrowRed, arrowRedMask);
        Core.inRange(blurred, lowerArrowGreen, upperArrowGreen, arrowGreenMask);

        // جمع المراكز
        detectAndSend(redMask, false);
        detectAndSend(greenMask, true);
        detectAndSend(arrowRedMask, false);
        detectAndSend(arrowGreenMask, true);

        // تحرير الذاكرة
        redMask1.release();
        redMask2.release();
        redMask.release();
        greenMask.release();
        arrowRedMask.release();
        arrowGreenMask.release();
        blurred.release();
        bgr.release();
        matRgba.release();
    }

    private void detectAndSend(Mat mask, boolean isBot) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            // تجاهل المساحات الصغيرة جداً (الضوضاء)
            if (rect.width < 5 || rect.height < 5) continue;
            // تجاهل المساحات الكبيرة جداً
            if (rect.width > screenWidth/2 || rect.height > screenHeight/2) continue;

            double centerX = rect.x + rect.width / 2.0;
            double centerY = rect.y + rect.height / 2.0;

            synchronized (BackgroundService.lock) {
                BackgroundService.Player player = new BackgroundService.Player();
                player.x = (float) centerX / screenWidth;
                player.y = (float) centerY / screenHeight;
                player.isBot = isBot;
                player.name = isBot ? "BOT" : "ENEMY";
                player.health = 100.0f;
                player.distance = 0.0f;
                BackgroundService.players.add(player);
            }
        }

        contours.clear();
        hierarchy.release();
    }

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        super.onDestroy();
    }
}
