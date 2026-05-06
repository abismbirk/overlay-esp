package com.alol.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import java.util.List;

public class SpectralOverlay {
    private static final Paint paint = new Paint();
    private static List<float[]> targets;
    private static int width, height;

    public static void updateTargets(List<float[]> detections, int w, int h) {
        targets = detections;
        width = w;
        height = h;
    }

    public static void draw(Canvas canvas) {
        if (targets == null) return;
        canvas.save();
        // قلب الإحداثيات لتناسب العرض
        canvas.scale(1, -1, width / 2f, height / 2f);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        for (float[] box : targets) {
            canvas.drawRect(box[0], box[1], box[2], box[3], paint);
        }
        canvas.restore();
    }
}
