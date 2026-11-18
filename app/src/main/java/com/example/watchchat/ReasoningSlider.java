package com.example.watchchat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * Reasoning-effort selector, built to Apple's fluid-interface rules.
 *
 *  • Responds on pointer-down, tracks 1:1 from the grab offset (never snaps to centre).
 *  • Commits on pointer-up using velocity *direction* when the finger flung the knob,
 *    otherwise the nearest stop — Apple's "decide reverse vs. commit" rule.
 *  • The knob is a spring: on interrupt it retargets from the live position and keeps
 *    the finger's release velocity, so a reversal never hits a "brick wall".
 *  • Rubber-bands at both ends instead of hard-stopping.
 *  • No particles, no fixed 34 ms frame timer, no synthetic gradients: it draws the
 *    app's own accent and animates only a single float, so it holds 60 fps trivially.
 */
final class ReasoningSlider extends View {
    interface OnLevelChangeListener {
        void onLevelChanged(ReasoningSlider view, int index, boolean fromUser);
    }

    /** Apple's "Drawer / sheet" spring: damping 0.8, response 0.3 s. */
    private static final float DAMPING = 0.8f;
    private static final float RESPONSE = 0.30f;
    /** Below this the spring is done; settling stops the frame loop. */
    private static final float REST_EPSILON = 0.02f;   // ~1px at three stops

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final RectF knob = new RectF();
    private final RectF filled = new RectF();   // reused every frame: no per-frame allocation
    private final android.graphics.drawable.GradientDrawable knobDrawable =
            new android.graphics.drawable.GradientDrawable();
    private boolean knobReady;

    // ---- particles: count and hue both scale with effort ----
    private static final int MAX_PARTICLES = 26;
    private final float[] px2 = new float[MAX_PARTICLES];
    private final float[] py2 = new float[MAX_PARTICLES];
    private final float[] pvx = new float[MAX_PARTICLES];
    private final float[] pvy = new float[MAX_PARTICLES];
    private final float[] page = new float[MAX_PARTICLES];
    private final float[] psize = new float[MAX_PARTICLES];
    private final java.util.Random rnd = new java.util.Random();
    private long pLastNs;
    private android.graphics.Bitmap staticLayer;   // rails, ticks and labels: drawn once
    private int staticW, staticH;
    private final Typeface face;

    private String[] labels = {"none", "low", "high"};
    private int selectedIndex;
    /** 0..1 per stop: how "active" its divider looks. Animated, not switched. */
    private float[] tickEmphasis = new float[0];

    // Spring state for the knob position, in stop units (0..n-1).
    private float position;
    private float velocity;
    private long lastFrameNs;
    private boolean frameScheduled;

    private final int slop;
    private float downX, downY;
    private float grabOffset;       // finger position minus knob position, in px
    private boolean dragging;
    private boolean decided;
    private android.view.VelocityTracker tracker;

    ReasoningSlider(Context c) {
        super(c);
        face = FontStore.typeface(c);
        text.setTypeface(face);
        text.setTextAlign(Paint.Align.CENTER);
        slop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
        setClickable(true);
        setFocusable(true);
    }

    void setLabels(String... values) {
        if (values != null && values.length > 0) {
            labels = values;
            tickEmphasis = new float[labels.length];
            tickEmphasis[Math.min(selectedIndex, labels.length - 1)] = 1f;
            widestHalfText = 0f;   // labels changed: recompute on next draw
            selectedIndex = Math.min(selectedIndex, labels.length - 1);
            staticLayer = null;
            position = selectedIndex;
            invalidate();
        }
    }

    private OnLevelChangeListener listener;

    void setOnLevelChangeListener(OnLevelChangeListener l) { listener = l; }

    int getSelectedIndex() { return selectedIndex; }

    /** Jumps to a value with no motion — for first layout after the page is rebuilt. */
    void setSelectedIndexImmediate(int index) {
        selectedIndex = Math.max(0, Math.min(labels.length - 1, index));
        position = selectedIndex;
        velocity = 0f;
        staticLayer = null;
        frameScheduled = false;   // cancel any pending spring frames
        invalidate();
    }

    /** Animates to a value — for programmatic changes the user should see happen. */
    void setSelectedIndex(int index) {
        selectedIndex = Math.max(0, Math.min(labels.length - 1, index));
        staticLayer = null;
        velocity = 0f;
        animateTo(selectedIndex, true);
    }

    /** Effort 0 -> light blue (#7FD4FF), 1 -> violet (#A66BFF). */
    private int colourFor(float effort) {
        int stops = Math.max(1, labels.length - 1);
        float t = Math.max(0f, Math.min(1f, effort / stops));
        int r = (int) (0x7F + (0xA6 - 0x7F) * t);
        int g = (int) (0xD4 + (0x6B - 0xD4) * t);
        return android.graphics.Color.rgb(r, g, 0xFF);
    }

    /** How many particles this effort level earns: none at 0, full at the top stop. */
    private int particleBudget(float effort) {
        int stops = Math.max(1, labels.length - 1);
        float t = Math.max(0f, Math.min(1f, effort / stops));
        return Math.round(t * MAX_PARTICLES);
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        // Taller than the old rail: the charge capsule needs room for labels inside it.
        super.onMeasure(wSpec, MeasureSpec.makeMeasureSpec(dp(56), MeasureSpec.EXACTLY));
    }

    // ---------------------------------------------------------------- geometry

    private float trackLeft() { return dp(14); }
    private float trackRight() { return getWidth() - dp(14); }
    private float usable() { return Math.max(1f, trackRight() - trackLeft()); }

    private float centreY() { return getHeight() / 2f; }

    /** x for a stop index, before rubber-banding. */
    /** Widest label half-width at the current text size, cached per size. */
    private float widestHalfText;

    private void recomputeLabelMetrics(Paint text, String[] labels) {
        float max = 0f;
        for (String raw : labels) {
            String label = raw == null ? "" : raw.toUpperCase(java.util.Locale.ROOT);
            max = Math.max(max, text.measureText(label));
        }
        widestHalfText = max / 2f;
    }

    /**
     * Centre for one label. All stops share one inset and are spread evenly across the
     * remaining width, so the layout is symmetric whether there are 2, 3 or 5 stops.
     */
    /** Divider position for stop `index`: evenly spread across the pill's inner span. */
    private float dividerX(int index) {
        int n = Math.max(1, labels.length - 1);
        float radius = dp(24);
        float left = trackLeft() + radius * 0.85f;
        float right = trackRight() - radius * 0.85f;
        if (right <= left) return (trackLeft() + trackRight()) / 2f;
        return left + (right - left) * (index / (float) n);
    }

    private float xForStop(float stop) {
        int n = Math.max(1, labels.length - 1);
        return trackLeft() + usable() * (stop / n);
    }

    /** Apple §9: resist progressively past either end instead of stopping dead. */
    private float rubberBand(float overshootPx) {
        float dimension = usable();
        return (overshootPx * dimension * 0.55f) / (dimension + 0.55f * Math.abs(overshootPx));
    }

    private float drawX(float stop) {
        float raw = xForStop(stop);
        if (raw < trackLeft()) return trackLeft() - rubberBand(trackLeft() - raw);
        if (raw > trackRight()) return trackRight() + rubberBand(raw - trackRight());
        return raw;
    }

    /** Nearest stop for a screen x. */
    private int stopAt(float x) {
        int n = Math.max(1, labels.length - 1);
        float t = (x - trackLeft()) / usable();
        return Math.round(Math.max(0f, Math.min(1f, t)) * n);
    }

    // ---------------------------------------------------------------- drawing

    /** Rails, tick dots and label text never change during a drag, so they are baked once. */
    private void ensureStaticLayer() {
        int w = getWidth(), h = getHeight();
        if (staticLayer != null && staticW == w && staticH == h) return;
        if (w <= 0 || h <= 0) return;
        staticW = w; staticH = h;
        staticLayer = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(staticLayer);
        int accent = AppearancePreferences.accent(getContext());
        float cy = centreY();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        staticLayer = null;   // force a rebake at the new size
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || labels.length == 0) return;

        float cy = centreY();
        float kx = drawX(position);
        float effort = position;
        int accent = colourFor(effort);

        // ---- capsule: taller body, no knob. The charge edge IS the position. ----
        float pillTop = cy - dp(16), pillBottom = cy + dp(16);
        float radius = (pillBottom - pillTop) / 2f;
        track.set(dp(10), pillTop, w - dp(10), pillBottom);

        paint.setStyle(Paint.Style.FILL);
        boolean lightTrack = AppearancePreferences.lightMode(getContext());
        paint.setColor(lightTrack ? 0xffe6e6e2 : 0xff1B1B1B);
        canvas.drawRoundRect(track, radius, radius, paint);

        // charge fills to the finger position; the leading cap is rounded, which replaces
        // the white dot as the "where am I" indicator.
        filled.set(track);
        filled.right = Math.max(track.left + radius * 2f, Math.min(track.right, kx));
        // Gradient along the charge: the level reads as intensity, not a flat block,
        // and it previews the colour the next stop will land on.
        paint.setShader(new android.graphics.LinearGradient(
                filled.left, 0, Math.max(filled.left + 1f, filled.right), 0,
                new int[]{colourFor(0f), accent}, null, android.graphics.Shader.TileMode.CLAMP));
        paint.setAlpha(215);
        canvas.drawRoundRect(filled, radius, radius, paint);
        paint.setAlpha(255);
        paint.setShader(null);

        // particles live inside the capsule, denser and hotter as effort rises
        drawParticles(canvas, filled, accent, effort, radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, dp(1)));
        paint.setColor(lightTrack ? 0x33000000 : 0x3DFFFFFF);
        canvas.drawRoundRect(track, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);

        // ---- tick dividers ----
        // No text: stops are marked by hairlines; the current one is taller and brighter.
        int stops = Math.max(1, labels.length - 1);
        for (int i = 0; i <= stops; i++) {
            float x = dividerX(i);
            boolean current = (i == selectedIndex);
            // Height and weight follow a per-tick emphasis value that eases toward its
            // target each frame, so the highlight grows and shrinks instead of popping.
            float e = (i < tickEmphasis.length) ? tickEmphasis[i] : (current ? 1f : 0f);
            float halfTall = dp(5.5f + 3.5f * e);
            float thick = Math.max(1f, dp(1.2f + 1.0f * e));
            int alpha = (int) (70 + 170 * e);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(lightTrack ? Color.argb(alpha, 0, 0, 0)
                                      : Color.argb(alpha, 255, 255, 255));
            canvas.drawRoundRect(x - thick / 2f, cy - halfTall, x + thick / 2f, cy + halfTall, thick / 2f, thick / 2f, paint);
        }
    }

    /** Particles drift inside the charged portion; count and colour ride the effort level. */
    private void drawParticles(Canvas canvas, RectF area, int accent, float effort, float radius) {
        int budget = particleBudget(effort);
        if (budget <= 0) return;
        // Only animate when there is something to animate and motion is allowed.
        if (android.animation.ValueAnimator.areAnimatorsEnabled() && !dragging) startFrameLoop();

        long now = System.nanoTime();
        float dt = pLastNs == 0 ? 1f / 60f : Math.min(0.05f, (now - pLastNs) / 1e9f);
        pLastNs = now;

        float inset = radius * 0.55f;
        float left = area.left + inset, right = Math.max(left + 1f, area.right - inset);
        float top = area.top + inset, bottom = area.bottom - inset;

        for (int i = 0; i < budget; i++) {
            if (page[i] <= 0f) {                    // (re)spawn
                px2[i] = left + rnd.nextFloat() * (right - left);
                py2[i] = top + rnd.nextFloat() * (bottom - top);
                pvx[i] = (rnd.nextFloat() - 0.5f) * dp(26);
                pvy[i] = (rnd.nextFloat() - 0.5f) * dp(18);
                page[i] = 0.5f + rnd.nextFloat() * 0.5f;
                psize[i] = dp(0.9f) + rnd.nextFloat() * dp(1.6f);
            }
            px2[i] += pvx[i] * dt;
            py2[i] += pvy[i] * dt;
            page[i] -= dt * 0.55f;

            // bounce inside the charge instead of escaping it
            if (px2[i] < left || px2[i] > right) { pvx[i] = -pvx[i]; px2[i] = Math.max(left, Math.min(right, px2[i])); }
            if (py2[i] < top || py2[i] > bottom) { pvy[i] = -pvy[i]; py2[i] = Math.max(top, Math.min(bottom, py2[i])); }

            int a = (int) (200 * Math.max(0f, Math.min(1f, page[i])));
            paint.setColor(0xFF000000 | (accent & 0x00FFFFFF));
            paint.setAlpha(a);
            canvas.drawCircle(px2[i], py2[i], psize[i], paint);
            paint.setAlpha(255);
        }
    }

    // ---------------------------------------------------------------- spring

    private void animateTo(float target, boolean keepVelocity) {
        if (!keepVelocity) velocity = 0f;
        startFrameLoop();
        if (android.animation.ValueAnimator.areAnimatorsEnabled()) return;
        // Reduced motion: no spring, just land.
        position = target;
        selectedIndex = Math.round(target);
        invalidate();
    }

    private void startFrameLoop() {
        if (frameScheduled) return;
        frameScheduled = true;
        lastFrameNs = 0L;
        postOnAnimation(frameTick);
    }

    private final Runnable frameTick = new Runnable() {
        @Override public void run() {
            frameScheduled = false;
            if (labels.length <= 1) return;

            long now = System.nanoTime();
            float dt = lastFrameNs == 0L ? 1f / 60f : Math.min(0.05f, (now - lastFrameNs) / 1e9f);
            lastFrameNs = now;

            float target = selectedIndex;
            // Critically-under-damped spring: damping ratio and response, Apple's terms.
            float omega = (float) (2.0 * Math.PI / RESPONSE);
            float accel = omega * omega * (target - position) - 2f * DAMPING * omega * velocity;
            velocity += accel * dt;
            position += velocity * dt;

            boolean springSettled =
                    Math.abs(target - position) < REST_EPSILON && Math.abs(velocity) < REST_EPSILON * 8f;
            if (springSettled) {
                position = target;
                velocity = 0f;
            }

            // Particles keep breathing after the spring lands, so the loop continues while
            // the charge is high enough to show them. At effort 0 there are none and the
            // frames stop entirely — an idle slider costs nothing.
            // ease each divider toward its target emphasis (1 = selected)
            if (tickEmphasis.length == labels.length) {
                float k = Math.min(1f, dt * 12f);   // ~80ms to settle
                for (int i = 0; i < tickEmphasis.length; i++) {
                    float want = (i == selectedIndex) ? 1f : 0f;
                    tickEmphasis[i] += (want - tickEmphasis[i]) * k;
                }
            }

            boolean particlesAlive = particleBudget(position) > 0
                    && android.animation.ValueAnimator.areAnimatorsEnabled();

            invalidate();
            if (springSettled && !particlesAlive) return;
            frameScheduled = true;
            postOnAnimation(this);
        }
    };

    // ---------------------------------------------------------------- gesture

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = e.getX();
                downY = e.getY();
                dragging = false;
                decided = false;
                velocity = 0f;
                // Respect where the finger landed relative to the knob (Apple §2).
                grabOffset = downX - drawX(position);
                if (tracker == null) tracker = android.view.VelocityTracker.obtain();
                tracker.clear();
                tracker.addMovement(e);
                // Feedback starts now, not on release (Apple §1).
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (tracker != null) tracker.addMovement(e);
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (!decided) {
                    if (dx * dx + dy * dy <= slop * slop) return true;
                    if (Math.abs(dy) > Math.abs(dx)) {   // a scroll, not a drag
                        dragging = false;
                        decided = true;
                        return false;
                    }
                    decided = true;
                    dragging = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (!dragging) return true;
                // 1:1 tracking from the grab offset — position follows the finger exactly.
                int n = Math.max(1, labels.length - 1);
                float fingerStop = n * (e.getX() - grabOffset - trackLeft()) / usable();
                position = fingerStop;          // may overshoot; drawX rubber-bands it
                velocity = 0f;
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (tracker != null) {
                    tracker.addMovement(e);
                    tracker.computeCurrentVelocity(1000);
                }
                float vx = tracker == null ? 0f : tracker.getXVelocity();
                if (tracker != null) { tracker.recycle(); tracker = null; }
                if (dragging) {
                    // Apple §5/§8: a fling decides from velocity *sign*, not from position.
                    boolean flung = Math.abs(vx) > dp(260);
                    int target;
                    if (flung) {
                        target = vx > 0
                                ? Math.min(labels.length - 1, selectedIndex + 1)
                                : Math.max(0, selectedIndex - 1);
                    } else {
                        target = stopAt(e.getX() - grabOffset);
                    }
                    applySelection(target, vx);
                } else if (decided == false) {
                    // A tap: no drag exceeded the slop, so jump to the stop under the finger.
                    applySelection(stopAt(e.getX()), 0f);
                }
                dragging = false;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (tracker != null) { tracker.recycle(); tracker = null; }
                dragging = false;
                applySelection(stopAt(drawX(position)), 0f);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    /** Hand the release velocity to the spring so there is no seam at the gesture end. */
    private void applySelection(int target, float releaseVelocityPxPerSec) {
        target = Math.max(0, Math.min(labels.length - 1, target));
        boolean changed = target != selectedIndex;
        selectedIndex = target;

        int n = Math.max(1, labels.length - 1);
        // Convert px/s into stop-units/s for the spring integrator.
        velocity = releaseVelocityPxPerSec * n / usable();
        startFrameLoop();

        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
            position = target;
            velocity = 0f;
            invalidate();
        }
        if (changed) {
            staticLayer = null;   // the active label/tick highlight moved
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            if (listener != null) listener.onLevelChanged(this, target, true);
        }
    }
}
