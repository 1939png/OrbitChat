package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

/**
 * Circular back affordance: frosted fill, hairline rim, and a chevron stroked as a path
 * so it stays optically centred at any size (a text "‹" sits low and off-axis).
 * Press feedback is a spring-backed scale, matching the pager.
 */
final class BackKey extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chevron = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int size;
    private boolean pressed;

    BackKey(Context c, int sizePx, int fillColor, int strokeColor) {
        super(c);
        size = sizePx;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(fillColor);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(Math.max(1f, getResources().getDisplayMetrics().density));
        rim.setColor(Color.argb(70, 255, 255, 255));
        chevron.setStyle(Paint.Style.STROKE);
        chevron.setStrokeCap(Paint.Cap.ROUND);
        chevron.setStrokeJoin(Paint.Join.ROUND);
        chevron.setColor(strokeColor);
        chevron.setStrokeWidth(2.4f * getResources().getDisplayMetrics().density);
        setClickable(true);
        setFocusable(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float r = Math.min(w, h) / 2f - 1f;
        canvas.drawCircle(cx, cy, r, fill);
        canvas.drawCircle(cx, cy, r, rim);
        // chevron: two strokes meeting at a point left of centre
        float arm = r * 0.42f;
        float x = cx - arm * 0.34f;
        path.reset();
        path.moveTo(x + arm * 0.72f, cy - arm);
        path.lineTo(x - arm * 0.28f, cy);
        path.lineTo(x + arm * 0.72f, cy + arm);
        canvas.drawPath(path, chevron);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                animate().scaleX(.97f).scaleY(.97f).setDuration(100)
                    .setInterpolator(VisualEffects.EASE_OUT).start();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pressed) {
                    pressed = false;
                    animate().scaleX(1f).scaleY(1f).setDuration(140)
                        .setInterpolator(VisualEffects.EASE_OUT).start();
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override public boolean performClick() { return super.performClick(); }
}
