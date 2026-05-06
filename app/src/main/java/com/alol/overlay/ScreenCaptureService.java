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

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCapture";
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
        HandlerThread thread = new HandlerThread("ScreenCapture");
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
            Log.d(TAG, "OpenCV ready");
        }
    }

    public void startProjection(Intent data) {
        if (data == null) return;
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(Activity.RESULT_OK, data);
        createVirtualDisplay();
    }

    private void createVirtualDisplay() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
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

        Mat bgr = new Mat();
        Imgproc.cvtColor(matRgba, bgr, Imgproc.COLOR_RGBA2BGR);

        Mat redMask = new Mat();
        Mat greenMask = new Mat();
        Core.inRange(bgr, new Scalar(0, 0, 200), new Scalar(80, 80, 255), redMask);
        Core.inRange(bgr, new Scalar(0, 200, 0), new Scalar(80, 255, 80), greenMask);

        detectAndSend(redMask, false);
        detectAndSend(greenMask, true);

        redMask.release();
        greenMask.release();
        bgr.release();
        matRgba.release();
    }

    private void detectAndSend(Mat mask, boolean isBot) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.width < 10 || rect.height < 10) continue;

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
