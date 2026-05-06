package com.alol.overlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;
import android.view.SurfaceHolder;
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

public class NeuralOverlayer {
    private static final String TAG = "NeuralOverlayer";
    private Interpreter tflite;
    private GpuDelegate gpuDelegate;
    private boolean loaded = false;
    private Paint paint;

    public NeuralOverlayer() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        try {
            gpuDelegate = new GpuDelegate();
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate).setNumThreads(4);
            tflite = new Interpreter(FileUtil.loadMappedFile(AppContext.get(), "yolov8n_float16.tflite"), options);
            loaded = true;
            Log.d(TAG, "Neural engine ready");
        } catch (IOException e) { Log.e(TAG, "Model load failed", e); }
    }

    public void processFrame(Image image, int width, int height) {
        if (!loaded) return;
        Bitmap bitmap = imageToBitmap(image);
        if (bitmap == null) return;

        TensorImage tensorImage = new TensorImage();
        tensorImage.load(bitmap);
        ImageProcessor processor = new ImageProcessor.Builder()
                .add(new ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR)).build();
        tensorImage = processor.process(tensorImage);

        float[][][] output = new float[1][84][8400];
        tflite.run(tensorImage.getBuffer(), output);
        bitmap.recycle();

        List<float[]> boxes = new ArrayList<>();
        for (int i = 0; i < 8400; i++) {
            float score = 0; int bestClass = -1;
            for (int c = 0; c < 80; c++) {
                if (output[0][4 + c][i] > score) { score = output[0][4 + c][i]; bestClass = c; }
            }
            if (score > 0.5f && bestClass == 0) {
                float cx = output[0][0][i], cy = output[0][1][i], w = output[0][2][i], h = output[0][3][i];
                boxes.add(new float[]{cx - w/2, cy - h/2, cx + w/2, cy + h/2, score});
            }
        }

        // رسم الصناديق على طبقة ESP
        Canvas canvas = SpectralOverlay.getCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT);
            for (float[] box : boxes) {
                canvas.drawRect(box[0], box[1], box[2], box[3], paint);
            }
        }
    }

    private Bitmap imageToBitmap(Image image) {
        ByteBuffer y = image.getPlanes()[0].getBuffer();
        ByteBuffer u = image.getPlanes()[1].getBuffer();
        ByteBuffer v = image.getPlanes()[2].getBuffer();
        byte[] nv21 = new byte[y.remaining() + u.remaining() + v.remaining()];
        y.get(nv21, 0, y.remaining());
        v.get(nv21, y.remaining(), v.remaining());
        u.get(nv21, y.remaining() + v.remaining(), u.remaining());
        android.graphics.YuvImage yuv = new android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
        return android.graphics.BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.toByteArray().length);
    }

    public void close() {
        if (tflite != null) tflite.close();
        if (gpuDelegate != null) gpuDelegate.close();
    }
}
