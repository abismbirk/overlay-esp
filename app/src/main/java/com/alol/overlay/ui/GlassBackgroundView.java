package com.alol.overlay.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class GlassBackgroundView extends View {
    private Paint paint;
    public GlassBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                Color.argb(30, 0, 255, 209),
                Color.argb(10, 0, 0, 0),
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
