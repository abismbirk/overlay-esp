package com.alol.overlay.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LiquidSurfaceView extends View {
    private Paint paint = new Paint();
    private List<Particle> particles = new ArrayList<>();
    private Random rand = new Random();
    private PointF touchPoint = new PointF(-100, -100);

    public LiquidSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        // إنشاء جسيمات البداية
        for (int i = 0; i < 80; i++) {
            particles.add(new Particle());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchPoint.set(event.getX(), event.getY());
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.argb(255, 10, 10, 26)); // خلفية داكنة

        paint.setAntiAlias(true);
        for (Particle p : particles) {
            // تحريك الجسيمات نحو نقطة اللمس
            float dx = touchPoint.x - p.x;
            float dy = touchPoint.y - p.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 300 && dist > 0) {
                p.x += dx / dist * 5;
                p.y += dy / dist * 5;
            } else {
                p.x += p.vx;
                p.y += p.vy;
            }
            // حدود الشاشة
            if (p.x < 0) { p.x = 0; p.vx *= -1; }
            if (p.x > getWidth()) { p.x = getWidth(); p.vx *= -1; }
            if (p.y < 0) { p.y = 0; p.vy *= -1; }
            if (p.y > getHeight()) { p.y = getHeight(); p.vy *= -1; }
            p.vx *= 0.99f;
            p.vy *= 0.99f;

            paint.setColor(Color.argb(p.alpha, 0, 255, 209));
            canvas.drawCircle(p.x, p.y, p.size, paint);
        }
        invalidate(); // إعادة رسم مستمرة
    }

    class Particle {
        float x, y, vx, vy, size;
        int alpha;
        Particle() {
            x = rand.nextFloat() * 1080;
            y = rand.nextFloat() * 1920;
            vx = (rand.nextFloat() - 0.5f) * 2;
            vy = (rand.nextFloat() - 0.5f) * 2;
            size = rand.nextFloat() * 4 + 1;
            alpha = rand.nextInt(100) + 50;
        }
    }
}
