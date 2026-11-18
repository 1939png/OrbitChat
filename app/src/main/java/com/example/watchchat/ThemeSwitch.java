package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Two-state day/night switch, built to the same rules as ProtocolSwitch: labels live inside
 * the track and a wide elliptical thumb slides over them.
 *
 * Both glyphs are drawn, not typed. A sun and a moon as text characters fall back to a
 * system face on Android and render at inconsistent weight — and the app's own face has no
 * U+263C/U+263E at all, so they would come out as missing-glyph boxes.
 */
final class ThemeSwitch extends View {
    interface OnChange { void onChange(boolean light); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final RectF thumb = new RectF();
    private final Path path = new Path();
    private boolean light;
    private float slide = 0f;
    private android.animation.ValueAnimator anim;
    private OnChange listener;

    private static final int NIGHT = 0xff5A6ACF;   // violet-blue
    private static final int DAY = 0xffF0A64B;     // warm amber

    ThemeSwitch(Context c) {
        super(c);
        slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
        setClickable(true);
    }

    void setOnChange(OnChange l) { listener = l; }

    void setLight(boolean value, boolean animate) {
        light = value;
        if (animate) animateTo(value ? 1f : 0f);
        else { slide = value ? 1f : 0f; invalidate(); }
    }

    boolean isLight() { return light; }

    private void animateTo(float target) {
        if (anim != null) anim.cancel();
        anim = android.animation.ValueAnimator.ofFloat(slide, target);
        anim.setDuration(220);
        anim.setInterpolator(VisualEffects.EASE_OUT);
        anim.addUpdateListener(a -> { slide = (float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        super.onMeasure(wSpec, MeasureSpec.makeMeasureSpec(dp(46), MeasureSpec.EXACTLY));
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float pad = h * 0.10f;
        track.set(pad, pad, w - pad, h - pad);
        float r = track.height() / 2f;
        int active = slide < 0.5f ? NIGHT : DAY;

        paint.setStyle(Paint.Style.FILL);
        final boolean lightTrack = AppearancePreferences.lightMode(getContext());
        paint.setColor(lightTrack ? 0xffe6e6e2 : 0xff141414);
        canvas.drawRoundRect(track, r, r, paint);

        float thumbW = track.width() / 2f;
        float thumbH = track.height() * 0.86f;
        float cy = track.centerY();
        float leftEdge = track.left + (track.width() - thumbW) * slide;
        thumb.set(leftEdge, cy - thumbH / 2f, leftEdge + thumbW, cy + thumbH / 2f);
        paint.setColor(active);
        canvas.drawRoundRect(thumb, thumbH / 2f, thumbH / 2f, paint);

        // Icons sit at the centre of each half, in the same slot the labels occupy in
        // ProtocolSwitch, so the two switches read as one control family.
        float iconR = h * 0.15f;
        boolean onLeft = slide < 0.5f;
        final int moonInk = onLeft ? 0xff1A1A2E : 0xff8E8E96;
        final int behind = lightTrack ? 0xffe6e6e2 : 0xff141414;
        drawMoon(canvas, track.left + track.width() * 0.25f, cy, iconR, moonInk, behind);
        drawSun(canvas, track.left + track.width() * 0.75f, cy, iconR, onLeft ? 0xff9A9A9A : 0xff2A1A00);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, h * 0.02f));
        paint.setColor(0x3DFFFFFF);
        canvas.drawRoundRect(track, r, r, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Crescent: a filled disc with a second disc punched out of it. */
    private void drawMoon(Canvas canvas, float cx, float cy, float r, int color, int behind) {
        paint.setColor(color);
        canvas.drawCircle(cx, cy, r, paint);
        // punch the bite with the thumb colour, which is what sits behind at this point
        paint.setColor(slide < 0.5f ? NIGHT : behind);
        canvas.drawCircle(cx + r * 0.62f, cy - r * 0.30f, r * 0.86f, paint);
    }

    private void drawSun(Canvas canvas, float cx, float cy, float r, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, r * 0.34f));
        canvas.drawCircle(cx, cy, r * 0.52f, paint);
        paint.setStyle(Paint.Style.FILL);
        // eight rays
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4.0;
            float x1 = cx + (float) (Math.cos(a) * r * 0.95f);
            float y1 = cy + (float) (Math.sin(a) * r * 0.95f);
            float x2 = cx + (float) (Math.cos(a) * r * 1.42f);
            float y2 = cy + (float) (Math.sin(a) * r * 1.42f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, r * 0.30f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private float downX, downY;
    private boolean tracking;
    private final int slop;

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                tracking = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking && Math.abs(e.getY() - downY) > slop
                        && Math.abs(e.getY() - downY) > Math.abs(e.getX() - downX)) {
                    tracking = false;   // clearly a scroll, hand it back to the list
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (tracking
                        && Math.abs(e.getX() - downX) <= slop * 2
                        && Math.abs(e.getY() - downY) <= slop * 2) {
                    boolean next = e.getX() > getWidth() / 2f;
                    if (next != light) {
                        light = next;
                        animateTo(next ? 1f : 0f);
                        if (listener != null) listener.onChange(next);
                    }
                }
                tracking = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                return true;
        }
        return super.onTouchEvent(e);
    }
}
