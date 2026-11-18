package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Ten-step "charged capsule" slider. The track is a rounded pill; the filled portion is the
 * charge, and the level you are on snaps to whole steps. Dragging anywhere on the capsule
 * changes the level, which suits a 500px watch far better than a thin slider thumb.
 */
final class SizeCapsule extends View {
    interface OnLevelChange { void onLevel(int level); }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF inner = new RectF();
    private int level = FontStore.DEFAULT_SIZE;
    private OnLevelChange listener;
    private float chargeLevel = -1f;   // animated charge position, in levels
    private android.animation.ValueAnimator chargeAnim;

    SizeCapsule(Context c) {
        super(c);
        level = FontStore.sizeLevel(c);
        chargeLevel = level;
        track.setStyle(Paint.Style.FILL);
        fill.setStyle(Paint.Style.FILL);
        tick.setStyle(Paint.Style.FILL);
        setClickable(true);
    }

    void setOnLevelChange(OnLevelChange l) { listener = l; }

    int level() { return level; }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float pad = h * 0.16f;
        rect.set(pad, pad, w - pad, h - pad);
        float radius = rect.height() / 2f;

        track.setColor(AppearancePreferences.lightMode(getContext()) ? 0xffe6e6e2 : 0xff1C1C1C);
        canvas.drawRoundRect(rect, radius, radius, track);

        // charge fill
        int accent = AppearancePreferences.accent(getContext());
        float fraction = (FontStore.SIZE_LEVELS <= 1) ? 1f
                : Math.max(0f, Math.min(1f, chargeLevel / (FontStore.SIZE_LEVELS - 1)));
        float fillRight = rect.left + radius * 2f + (rect.width() - radius * 2f) * fraction;
        inner.set(rect.left, rect.top, Math.max(rect.left + radius * 2f, fillRight), rect.bottom);
        fill.setColor(accent);
        canvas.drawRoundRect(inner, radius, radius, fill);

        // leading edge glow
        if (fraction > 0.02f) {
            glow.setColor(0x55FFFFFF);
            canvas.drawCircle(inner.right - radius, rect.centerY(), radius * 0.42f, glow);
        }

        // ten discrete ticks, so the steps are legible
        tick.setColor(0x33FFFFFF);
        float tickR = Math.max(1f, h * 0.03f);
        for (int i = 0; i < FontStore.SIZE_LEVELS; i++) {
            float t = i / (float) (FontStore.SIZE_LEVELS - 1);
            float x = rect.left + radius + (rect.width() - radius * 2f) * t;
            canvas.drawCircle(x, rect.centerY(), tickR, tick);
        }

        // hairline rim
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(Math.max(1f, h * 0.02f));
        track.setColor(0x3DFFFFFF);
        canvas.drawRoundRect(rect, radius, radius, track);
        track.setStyle(Paint.Style.FILL);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                float w = getWidth();
                float pad = getHeight() * 0.16f;
                float usable = w - pad * 2f;
                float t = usable <= 0 ? 0f : (e.getX() - pad) / usable;
                int next = Math.round(Math.max(0f, Math.min(1f, t)) * (FontStore.SIZE_LEVELS - 1));
                if (next != level) {
                    level = next;
                    FontStore.setSizeLevel(getContext(), level);
                    animateCharge(level);
                    if (listener != null) listener.onLevel(level);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void animateCharge(float target) {
        if (chargeAnim != null) chargeAnim.cancel();
        chargeAnim = android.animation.ValueAnimator.ofFloat(chargeLevel, target);
        chargeAnim.setDuration(180);
        chargeAnim.setInterpolator(VisualEffects.EASE_OUT);
        chargeAnim.addUpdateListener(a -> { chargeLevel = (float) a.getAnimatedValue(); invalidate(); });
        chargeAnim.start();
    }
}
