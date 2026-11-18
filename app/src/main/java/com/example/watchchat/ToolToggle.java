package com.example.watchchat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Small on/off gate for a tool row.
 *
 * Why not a CheckBox or Switch widget: the platform switch ships its own track colours, its
 * own thumb shadow and its own 48dp minimum, none of which survive the day/night switch or fit
 * a 44dp row. This draws one rounded track and one round thumb in the app's own colours.
 *
 * Motion: the thumb slides over 180ms on EASE_OUT while the track fill and the thumb colour
 * interpolate on the same clock. A single animator drives all three so they cannot desync —
 * two animators with the same duration still start on different frames.
 */
final class ToolToggle extends View {

    interface OnChange { void onChange(boolean on); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private boolean on;
    private float slide;          // 0 = off, 1 = on; animated
    private float press;          // 0 = idle, 1 = pressed
    private ValueAnimator anim;
    private OnChange listener;
    private final int slop;

    ToolToggle(Context c) {
        super(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        setClickable(true);
    }

    void setOnChange(OnChange l) { listener = l; }

    void setOn(boolean value, boolean animate) {
        on = value;
        if (animate) {
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(slide, value ? 1f : 0f);
            anim.setDuration(180);
            anim.setInterpolator(VisualEffects.EASE_OUT);
            anim.addUpdateListener(a -> { slide = (float) a.getAnimatedValue(); invalidate(); });
            anim.start();
        } else {
            slide = value ? 1f : 0f;
            invalidate();
        }
    }

    boolean isOn() { return on; }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(
                (int) dp(38),
                MeasureSpec.getSize(hSpec) > 0 ? MeasureSpec.getSize(hSpec) : (int) dp(24));
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Press feedback shrinks the whole gate slightly, which is what tells the finger the
        // press landed before the state even changes.
        final float s = 1f - 0.06f * press;
        final int save = canvas.save();
        canvas.scale(s, s, w / 2f, h / 2f);

        // Track is 34 x 20 with a 15dp thumb: the thumb leaves 2dp of visible track on the
        // side it rests against, which is what makes the off state read as "off" rather than
        // as a floating dot. Width was previously computed as 38 - 8 = 30, leaving almost no
        // track around a 27dp thumb.
        float barH = dp(20);
        float barW = dp(34);
        float inset = (w - barW) / 2f;
        track.set(inset, (h - barH) / 2f, inset + barW, (h + barH) / 2f);
        float r = track.height() / 2f;

        boolean light = AppearancePreferences.lightMode(getContext());
        int accent = AppearancePreferences.accent(getContext());

        // Track: off = a quiet outline, on = accent fill. Interpolated, never switched.
        int offColor = light ? 0xFFDDDDD9 : 0xFF2A2A2C;
        int trackColor = blend(offColor, accent, slide);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(trackColor);
        canvas.drawRoundRect(track, r, r, paint);

        if (slide < 0.5f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, dp(1)));
            paint.setColor(blend(light ? 0xFFC9C9C5 : 0xFF3C3C3E, accent, slide));
            canvas.drawRoundRect(track, r, r, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        // Thumb: rides the same 0..1 value, so travel and colour are one gesture.
        float thumbR = r - dp(2.5f);
        float travel = track.width() - thumbR * 2f - dp(3f);
        float cx = track.left + thumbR + dp(1.5f) + travel * slide;
        float cy = track.centerY();
        paint.setColor(blend(light ? 0xFFF7F7F5 : 0xFFF2F2F0, 0xFF121212, slide));
        canvas.drawCircle(cx, cy, thumbR, paint);

        canvas.restoreToCount(save);
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
        int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
        return Color.rgb(
                (int) (ar + (br - ar) * t),
                (int) (ag + (bg - ag) * t),
                (int) (ab + (bb - ab) * t));
    }

    private float downX, downY;
    private boolean tracking;

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                tracking = true;
                press = 1f;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking && Math.abs(e.getY() - downY) > slop
                        && Math.abs(e.getY() - downY) > Math.abs(e.getX() - downX)) {
                    tracking = false;   // a scroll, not a press
                    press = 0f;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                press = 0f;
                invalidate();
                if (tracking
                        && Math.abs(e.getX() - downX) <= slop * 2
                        && Math.abs(e.getY() - downY) <= slop * 2) {
                    on = !on;
                    setOn(on, true);
                    if (listener != null) listener.onChange(on);
                }
                tracking = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                press = 0f;
                tracking = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }
}
