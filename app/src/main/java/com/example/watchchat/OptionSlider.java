package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Discrete slider: snaps to labelled steps and reports the selected index. */
final class OptionSlider extends View {
    interface OnPick { void onPick(int index); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private String[] labels = new String[0];
    private int index = 0;
    private float pos = 0f;
    private OnPick listener;
    private android.animation.ValueAnimator anim;
    private final android.graphics.Typeface face;

    OptionSlider(Context c) {
        super(c);
        face = FontStore.typeface(c);
        setClickable(true);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(face);
    }

    void setLabels(String[] values) { labels = values == null ? new String[0] : values; invalidate(); }

    void setIndex(int i, boolean animate) {
        index = Math.max(0, Math.min(labels.length - 1, i));
        float target = labels.length <= 1 ? 0f : index / (float) (labels.length - 1);
        if (animate) animateTo(target); else { pos = target; invalidate(); }
    }

    int index() { return index; }

    void setOnPick(OnPick l) { listener = l; }

    private void animateTo(float target) {
        if (anim != null) anim.cancel();
        anim = android.animation.ValueAnimator.ofFloat(pos, target);
        anim.setDuration(180);
        anim.setInterpolator(VisualEffects.EASE_OUT);
        anim.addUpdateListener(a -> { pos = (float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    /** Text size is a layout-affecting property: set it once, never inside onDraw. */
    private void ensureTextSize() {
        int want = dp(11);
        if ((int) label.getTextSize() != want) label.setTextSize(want);
    }

    @Override protected void onMeasure(int w, int h) {
        int height = dp(52);
        super.onMeasure(w, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || labels.length == 0) return;
        float barH = h * 0.34f;
        float barTop = (h - barH) / 2f;
        track.set(dp(6), barTop, w - dp(6), barTop + barH);
        float r = barH / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AppearancePreferences.lightMode(getContext()) ? 0xffe6e6e2 : 0xff141414);
        canvas.drawRoundRect(track, r, r, paint);

        int accent = AppearancePreferences.accent(getContext());
        RectF filled = new RectF(track);
        filled.right = track.left + track.width() * Math.max(0.001f, pos);
        if (pos <= 0.001f) filled.right = track.left + r;
        paint.setColor(accent);
        paint.setAlpha(200);
        canvas.drawRoundRect(filled, r, r, paint);
        paint.setAlpha(255);

        // step dots
        paint.setColor(0x44FFFFFF);
        for (int i = 0; i < labels.length; i++) {
            float t = labels.length <= 1 ? 0f : i / (float) (labels.length - 1);
            float x = track.left + r + (track.width() - r * 2f) * t;
            canvas.drawCircle(x, track.centerY(), Math.max(1.5f, barH * 0.08f), paint);
        }

        // knob
        float knobR = barH * 0.72f;
        float cx = track.left + r + (track.width() - r * 2f) * pos;
        paint.setColor(accent);
        canvas.drawCircle(cx, track.centerY(), knobR, paint);

        // labels above the bar
        ensureTextSize();
        for (int i = 0; i < labels.length; i++) {
            float t = labels.length <= 1 ? 0f : i / (float) (labels.length - 1);
            float x = Math.max(dp(14), Math.min(w - dp(14), track.left + r + (track.width() - r * 2f) * t));
            label.setColor(i == index ? accent : 0x99FFFFFF);
            canvas.drawText(labels[i], x, barTop - dp(4), label);
        }
    }

    private float downX, downY;
    private boolean dragStarted;
    private boolean committed;
    private final int slop = android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private int pendingIndex = -1;

    /**
     * Follows ViewDragHelper: the gesture is only claimed once the pointer has moved past the
     * touch slop in any direction, and a vertical drag always belongs to the list. Until then
     * the control is inert, so scrolling past it never changes the value.
     */
    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                dragStarted = false;
                committed = false;
                pendingIndex = -1;
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (!dragStarted) {
                    // Euclidean slop test, exactly as ViewDragHelper.checkTouchSlop does.
                    if (dx * dx + dy * dy <= slop * slop) return true;
                    if (Math.abs(dy) > Math.abs(dx)) return true;   // a scroll: stay inert
                    dragStarted = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                pendingIndex = indexAt(e.getX());
                return true;
            }

            case MotionEvent.ACTION_UP: {
                boolean wasDrag = dragStarted;
                boolean upIsTap = Math.abs(e.getX() - downX) <= slop && Math.abs(e.getY() - downY) <= slop;
                int target = wasDrag ? pendingIndex : (upIsTap ? indexAt(e.getX()) : -1);
                if (target >= 0 && target != index) {
                    index = target;
                    animateTo(labels.length <= 1 ? 0f : index / (float) (labels.length - 1));
                    if (listener != null) listener.onPick(index);
                }
                dragStarted = false;
                pendingIndex = -1;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                dragStarted = false;
                pendingIndex = -1;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(e);
    }

    /** Index under an x coordinate. */
    private int indexAt(float x) {
        float r = (getHeight() * 0.34f) / 2f;
        float usable = getWidth() - dp(12) - r * 2f;
        float t = usable <= 0 ? 0f : (x - dp(6) - r) / usable;
        return Math.round(Math.max(0f, Math.min(1f, t)) * Math.max(1, labels.length - 1));
    }
}
