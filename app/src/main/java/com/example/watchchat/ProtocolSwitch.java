package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * Two-state protocol switch. Both option names are printed inside the track, one on each
 * side, and an elliptical thumb slides over them — a wide thumb rather than a circle, so it
 * reads as a labelled segment selector instead of a toggle.
 */
final class ProtocolSwitch extends View {
    interface OnChange { void onChange(boolean anthropic); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final RectF thumb = new RectF();
    private final Typeface face;
    private boolean anthropic;
    private float slide = 0f;
    private android.animation.ValueAnimator anim;
    private OnChange listener;

    private static final int GREEN = 0xff7BD88F;
    private static final int ORANGE = 0xffF0A64B;
    private static final String LEFT_LABEL = "OpenAI";
    private static final String RIGHT_LABEL = "Anthropic";

    ProtocolSwitch(Context c) {
        super(c);
        slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
        face = FontStore.typeface(c);
        text.setTypeface(face);
        text.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
    }

    void setOnChange(OnChange l) { listener = l; }

    void setAnthropic(boolean value, boolean animate) {
        anthropic = value;
        if (animate) animateTo(value ? 1f : 0f);
        else { slide = value ? 1f : 0f; invalidate(); }
    }

    boolean isAnthropic() { return anthropic; }

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
        int active = slide < 0.5f ? GREEN : ORANGE;

        paint.setStyle(Paint.Style.FILL);
        boolean light = AppearancePreferences.lightMode(getContext());
        paint.setColor(light ? 0xffe6e6e2 : 0xff141414);
        canvas.drawRoundRect(track, r, r, paint);

        // elliptical thumb: half the track minus padding, so it covers exactly one label
        float thumbW = track.width() / 2f;
        float thumbH = track.height() * 0.86f;
        float cy = track.centerY();
        float leftEdge = track.left + (track.width() - thumbW) * slide;
        thumb.set(leftEdge, cy - thumbH / 2f, leftEdge + thumbW, cy + thumbH / 2f);
        paint.setColor(active);
        canvas.drawRoundRect(thumb, thumbH / 2f, thumbH / 2f, paint);

        // labels sit inside their own half; the covered one flips to dark for contrast.
        // Size is set only when it actually changes, so a sliding frame does no text layout.
        float wantSize = h * 0.30f;
        if (Math.abs(text.getTextSize() - wantSize) > 0.5f) text.setTextSize(wantSize);
        float leftCx = track.left + track.width() * 0.25f;
        float rightCx = track.left + track.width() * 0.75f;
        float baseline = cy - (text.descent() + text.ascent()) / 2f;
        boolean onLeft = slide < 0.5f;
        int inactive = AppearancePreferences.lightMode(getContext()) ? 0xff000000 : 0xff9A9A9A;
        text.setColor(onLeft ? 0xff101010 : inactive);
        canvas.drawText(LEFT_LABEL, leftCx, baseline, text);
        text.setColor(onLeft ? inactive : 0xff101010);
        canvas.drawText(RIGHT_LABEL, rightCx, baseline, text);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, h * 0.02f));
        paint.setColor(0x3DFFFFFF);
        canvas.drawRoundRect(track, r, r, paint);
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
                // Do not commit here: a vertical scroll that grazes the switch would flip it.
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking && Math.abs(e.getY() - downY) > slop
                        && Math.abs(e.getY() - downY) > Math.abs(e.getX() - downX)) {
                    tracking = false;   // clearly a scroll, hand it back to the list
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (tracking
                        && Math.abs(e.getX() - downX) <= slop * 2
                        && Math.abs(e.getY() - downY) <= slop * 2) {
                    boolean next = e.getX() > getWidth() / 2f;
                    if (next != anthropic) {
                        anthropic = next;
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
