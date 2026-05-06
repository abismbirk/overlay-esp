package com.alol.overlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
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

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AIDetectionService extends Service {
    private static final String TAG = "AIDetection";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private int screenWidth, screenHeight, screenDensity;
    private Interpreter tflite;
    private GpuDelegate gpuDelegate;
    private boolean modelLoaded = false;

    private static final int INPUT_SIZE = 640;  // YOLOv8n input
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    private static final float NMS_THRESHOLD = 0.4f;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("AIDetection");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        loadModel();
    }

    private void loadModel() {
        try {
            gpuDelegate = new GpuDelegate();
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate);
            options.setNumThreads(4);
            ByteBuffer model = FileUtil.loadMappedFile(this, "yolov8n_float16.tflite");
            tflite = new Interpreter(model, options);
            modelLoaded = true;
            Log.d(TAG, "YOLOv8n model loaded with GPU delegate.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model: " + e.getMessage());
        }
    }

    public void startProjection(Intent data) {
        if (data == null) return;
        MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(-1, data);
        createVirtualDisplay();
    }

    private void createVirtualDisplay() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("AIDetection",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, backgroundHandler);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null && modelLoaded) {
                    processImage(image);
                    image.close();
                }
            }
        }, backgroundHandler);
    }

    private void processImage(Image image) {
        // تحويل Image إلى Bitmap
        int w = image.getWidth();
        int h = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        // تحويل NV21 إلى Bitmap (أسرع باستخدام YuvImage)
        android.graphics.YuvImage yuv = new android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, w, h, null);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuv.compressToJpeg(new android.graphics.Rect(0, 0, w, h), 90, out);
        byte[] jpeg = out.toByteArray();
        Bitmap decoded = android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        if (decoded == null) return;

        // تجهيز الصورة للنموذج
        TensorImage tensorImage = new TensorImage();
        tensorImage.load(decoded);
        ImageProcessor processor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();
        tensorImage = processor.process(tensorImage);

        // تشغيل الاستدلال
        float[][][] output = new float[1][84][8400]; // شكل خرج YOLOv8n
        tflite.run(tensorImage.getBuffer(), output);

        // معالجة النتائج (NMS بسيط)
        List<float[]> boxes = new ArrayList<>();
        for (int i = 0; i < 8400; i++) {
            float maxClassScore = 0;
            int classId = -1;
            for (int c = 0; c < 80; c++) { // COCO classes
                float score = output[0][4 + c][i];
                if (score > maxClassScore) {
                    maxClassScore = score;
                    classId = c;
                }
            }
            if (maxClassScore > CONFIDENCE_THRESHOLD && classId == 0) { // class 0 = person
                float cx = output[0][0][i];
                float cy = output[0][1][i];
                float bw = output[0][2][i];
                float bh = output[0][3][i];
                float x1 = cx - bw/2;
                float y1 = cy - bh/2;
                float x2 = cx + bw/2;
                float y2 = cy + bh/2;
                boxes.add(new float[]{x1, y1, x2, y2, maxClassScore});
            }
        }

        // NMS بسيط (اختياري)
        // ... (سنقوم بتنفيذ NMS لاحقاً أو نستخدم مكتبة)

        // إرسال النتائج إلى Overlay
        synchronized (BackgroundService.lock) {
            BackgroundService.players.clear();
            for (float[] box : boxes) {
                float x1 = box[0];
                float y1 = box[1];
                float x2 = box[2];
                float y2 = box[3];
                float centerX = (x1 + x2) / 2.0f;
                float centerY = (y1 + y2) / 2.0f;

                BackgroundService.Player player = new BackgroundService.Player();
                player.x = centerX; // تطبيع؟ سنقوم بالتطبيع لاحقاً
                player.y = centerY;
                player.isBot = false; // لا يمكن التمييز بالذكاء الاصطناعي
                player.name = "Player";
                player.health = 100.0f;
                player.distance = 0.0f;
                BackgroundService.players.add(player);
            }
        }
        decoded.recycle();
    }

    @Override
    public void onDestroy() {
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        if (tflite != null) tflite.close();
        if (gpuDelegate != null) gpuDelegate.close();
        super.onDestroy();
    }
}
