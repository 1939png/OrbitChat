package com.example.watchchat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Boot sequence for OPEN CHAT · WATCH.
 *
 * Ported from OpenChat-Watch-启动动画.html, keeping its timeline verbatim:
 *
 *   0.18s  OPEN CHAT assembles — each pixel flies in from a random offset and fades up
 *   1.10s  WATCH locks in — pixels drop 5-9px and land with a scanline split
 *   1.12s  the beam sweeps the width of OPEN CHAT, leaving its baseline behind it
 *   1.18s  WATCH's rule draws from the right
 *   1.02s  viewfinder corners fade in
 *   1.48s  cursor starts blinking
 *   2.50s  outro begins — every pixel disperses outward from the centre
 *   3.40s  the frame goes to black
 *
 * Everything is one animator over a single elapsed clock, because sixteen independent
 * ValueAnimators would each need their own frame callback for a three-second intro.
 *
 * Only pixels are drawn per frame: ~195 small rects, no text layout, no allocation.
 */
public class SplashView extends View {

    // ---- timeline, in ms, straight from the reference -------------------------------
    private static final long OPEN_BASE = 180;
    private static final long OPEN_SPAN = 700;
    private static final long OPEN_DUR = 380;
    private static final long WATCH_BASE = 1100;
    private static final long WATCH_GAP = 14;
    private static final long WATCH_DUR = 300;
    private static final long OUTRO_AT = 2500;
    private static final long OFF_AT = 3600;
    private static final long DISP_DUR = 380;
    private static final long TOTAL_MS = OFF_AT + 60;

    private static final int ROWS = 7;
    private static final int GAP_CELL = 2;
    private static final int SPACE_CELL = 3;

    private static final int FG = 0xFFF2F2F0;
    private static final int MUTED = 0xFF8A8A86;
    private static final int LINE_A = 0x38F2F2F0;   // ~0.22 alpha

    /** 5x7 bitmaps. Same shapes as the reference so the letterforms match exactly. */
    private static final String[][] FONT = {
            {"O", " .###.|#...#|#...#|#...#|#...#|#...#|.###."},
            {"P", " ####.|#...#|#...#|####.|#....|#....|#...."},
            {"E", " #####|#....|#....|####.|#....|#....|#####"},
            {"N", " #...#|##..#|##..#|#.#.#|#..##|#..##|#...#"},
            {"C", " .###.|#...#|#....|#....|#....|#...#|.###."},
            {"H", " #...#|#...#|#...#|#####|#...#|#...#|#...#"},
            {"A", " .###.|#...#|#...#|#####|#...#|#...#|#...#"},
            {"T", " #####|..#..|..#..|..#..|..#..|..#..|..#.."},
            {"W", " #...#|#...#|#...#|#.#.#|#.#.#|##.##|#...#"},
    };

    private static final String LINE_OPEN = "OPEN CHAT";
    private static final String LINE_WATCH = "WATCH";

    private final Paint px = new Paint();
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final Random rnd = new Random(20260101L);   // fixed seed: same motion every launch

    private final List<Pixel> open = new ArrayList<>();
    private final List<Pixel> watch = new ArrayList<>();
    private final List<float[]> noise = new ArrayList<>();
    private final List<Long> tickDelays = new ArrayList<>();

    private float cell = 6f;
    private float openW, watchW;          // in px
    private float centerX, centerY;
    private float openTop, watchTop;
    private ValueAnimator anim;
    private Runnable onDone;
    private boolean finished;
    /** Title mode draws only the settled OPEN CHAT row: no clock, no HUD.
     *  The chat header keeps one of these alive so the mark that flew there stays.
     */
    private boolean titleMode;
    private long elapsed;

    private static final class Pixel {
        float x, y;        // resting position, px
        float dx, dy;      // scatter offset at spawn
        float odx, ody;    // dispersal offset at outro
        long delay, dur, odelay;
        boolean scan;      // WATCH pixels are drawn with the scanline split
    }

    public SplashView(Context c, int accentColor, Runnable completion) {
        super(c);
        this.onDone = completion;
        px.setStyle(Paint.Style.FILL);
        px.setAntiAlias(false);          // hard pixel edges
        text.setTypeface(FontStore.typeface(c));
        text.setTextAlign(Paint.Align.LEFT);
        setBackgroundColor(Color.BLACK);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Switches to the settled single-row mark used as the chat header. */
    void setTitleMode(boolean on) {
        titleMode = on;
        if (on) {
            if (anim != null) { anim.cancel(); anim = null; }
            elapsed = TOTAL_MS;
        }
        requestLayout();
        invalidate();
    }

    /** Kept for source compatibility; this sequence does not lift the mark. */
    @Deprecated public void setLiftTarget(float screenY) { }

    // ---- layout ---------------------------------------------------------------------

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float v = Math.min(w, h);
        cell = Math.max(3f, Math.min(9f, 1.2f * v / 100f + 0.5f));
        if (titleMode) {
            // In the header the row must span the given box, so the cell is derived from
            // width instead of from the screen size.
            int cols = measureCols(LINE_OPEN);
            cell = Math.max(1.5f, w / (float) Math.max(1, cols));
            openW = cols * cell;
            watchW = 0;
            centerX = w / 2f;
            centerY = h / 2f;
            openTop = (h - ROWS * cell) / 2f;
            watchTop = openTop;
            buildPixels();
            return;
        }

        int openCols = measureCols(LINE_OPEN);
        int watchCols = measureCols(LINE_WATCH);
        openW = openCols * cell;
        watchW = watchCols * cell;

        centerX = w / 2f;
        // The mark sits slightly above centre: the reference reserves the lower third for
        // the HUD and the long drop-shadow line beneath the title.
        float stackH = ROWS * cell * 2 + cell * 8;
        centerY = h * 0.44f;
        openTop = centerY - stackH / 2f;
        watchTop = openTop + ROWS * cell + cell * 8f;

        buildPixels();
        buildNoise();
        buildTicks();
    }

    private int measureCols(String s) {
        int col = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == ' ') { col += SPACE_CELL; continue; }
            String[] g = glyph(ch);
            if (g == null) continue;
            col += g[0].length() + GAP_CELL;
        }
        return Math.max(1, col - GAP_CELL);
    }

    private static String[] glyph(char ch) {
        for (String[] e : FONT) {
            if (e[0].charAt(0) == ch) {
                String[] rows = e[1].trim().split("\\|");
                String[] out = new String[rows.length];
                for (int i = 0; i < rows.length; i++) out[i] = rows[i].trim();
                return out;
            }
        }
        return null;
    }

    private void buildPixels() {
        open.clear();
        watch.clear();
        lay(open, LINE_OPEN, openW, openTop, false);
        lay(watch, LINE_WATCH, watchW, watchTop, true);
    }

    private void lay(List<Pixel> out, String line, float width, float top, boolean scan) {
        int total = measureCols(line);
        float left = centerX - width / 2f;
        int colCursor = 0;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') { colCursor += SPACE_CELL; continue; }
            String[] g = glyph(ch);
            if (g == null) continue;
            int cols = g[0].length();

            // delay for this character's left edge
            long charDelay;
            if (scan) {
                charDelay = WATCH_BASE + (long) colCursor * WATCH_GAP;
            } else {
                float ratio = Math.min(1f, colCursor / (float) total);
                charDelay = OPEN_BASE + (long) (ratio * OPEN_SPAN);
            }

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < cols; c++) {
                    if (g[r].charAt(c) != '#') continue;
                    Pixel p = new Pixel();
                    p.x = left + (colCursor + c) * cell;
                    p.y = top + r * cell;
                    p.scan = scan;

                    if (scan) {
                        p.dy = 5 + rnd.nextFloat() * 4;
                        p.dx = 0;
                    } else {
                        double ang = rnd.nextDouble() * Math.PI * 2;
                        float dist = 16 + rnd.nextFloat() * 40;
                        p.dx = (float) Math.cos(ang) * dist;
                        p.dy = (float) Math.sin(ang) * dist;
                    }

                    p.delay = charDelay + (long) c * 5 + (long) r * 2
                            + (long) (rnd.nextFloat() * 55);
                    p.dur = scan ? WATCH_DUR : OPEN_DUR;

                    // dispersal vector: away from the centre of the screen
                    float cx = p.x + cell / 2f, cy = p.y + cell / 2f;
                    float vx = cx - centerX, vy = cy - centerY;
                    float len = Math.max(1f, (float) Math.hypot(vx, vy));
                    float d = 40 + rnd.nextFloat() * 56;
                    p.odx = vx / len * d;
                    p.ody = vy / len * d;
                    p.odelay = 60 + (long) (Math.max(0, 900 - p.delay) * 0.38
                            + rnd.nextFloat() * 40);

                    out.add(p);
                }
            }
            colCursor += cols + GAP_CELL;
        }
    }

    private void buildNoise() {
        noise.clear();
        for (int i = 0; i < 36; i++) {
            // x, y, kind(0=dash 1=plus), twinkle phase
            noise.add(new float[]{
                    rnd.nextFloat() * 0.96f + 0.02f,
                    rnd.nextFloat() * 0.96f + 0.02f,
                    rnd.nextFloat() < 0.42f ? 1f : 0f,
                    rnd.nextFloat() * 6.28f
            });
        }
    }

    private void buildTicks() {
        tickDelays.clear();
        tickDelays.addAll(delaysFor(open, LINE_OPEN, false));
        tickDelays.addAll(delaysFor(watch, LINE_WATCH, true));
    }

    private List<Long> delaysFor(List<Pixel> px, String line, boolean scan) {
        List<Long> out = new ArrayList<>();
        int total = measureCols(line);
        int colCursor = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') { colCursor += SPACE_CELL; continue; }
            String[] g = glyph(ch);
            if (g == null) continue;
            long d = scan ? WATCH_BASE + (long) colCursor * WATCH_GAP
                    : OPEN_BASE + (long) (Math.min(1f, colCursor / (float) total) * OPEN_SPAN);
            out.add(d);
            colCursor += g[0].length() + GAP_CELL;
        }
        return out;
    }

    // ---- clock ----------------------------------------------------------------------

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (anim != null) return;
        if (!ValueAnimator.areAnimatorsEnabled()) {
            elapsed = TOTAL_MS;          // reduced motion: land directly on the settled frame
            invalidate();
            post(this::finishNow);
            return;
        }
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(TOTAL_MS);
        anim.setInterpolator(null);      // linear: every curve is applied per element
        anim.addUpdateListener(a -> {
            elapsed = (long) (a.getAnimatedFraction() * TOTAL_MS);
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { finishNow(); }
        });
        anim.start();
    }

    private void finishNow() {
        if (finished) return;
        finished = true;
        if (onDone != null) onDone.run();
    }

    @Override protected void onDetachedFromWindow() {
        if (anim != null) { anim.cancel(); anim = null; }
        super.onDetachedFromWindow();
    }


    /** Width of the OPEN CHAT row in px — the box the header flight must match. */
    float markWidth() { return openW; }

    /** Height of one text row in px. */
    float markHeight() { return ROWS * cell; }

    /** Top of the OPEN CHAT row inside this view, in px. */
    float markTop() { return openTop; }


    /**
     * Stops the boot clock and freezes the settled frame.
     *
     * Without this the running ValueAnimator keeps calling invalidate() and, more importantly,
     * keeps the view in its animating state — an external ViewPropertyAnimator on the same view
     * would fight it. The header flight calls this first.
     */
    void stopClock() {
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
        // Freeze on the last frame BEFORE the outro. TOTAL_MS would put it past OUTRO_AT,
        // where every pixel has already dispersed to zero alpha — the flight would then be
        // moving an invisible mark.
        elapsed = OUTRO_AT - 1;
        invalidate();
    }


    private float flyX, flyY, flyScale = 1f, flyAlpha = 1f;

    /**
     * Applies the header flight inside the draw pass.
     *
     * A ViewPropertyAnimator on translationX/Y is undone the moment the parent relayouts,
     * which happens here as the chat page finishes building. Scaling and offsetting the
     * canvas instead survives relayout because nothing in the layout system owns it.
     */
    void setFlight(float dx, float dy, float scale, float alpha, float pivotX, float pivotY) {
        this.flyX = dx;
        this.flyY = dy;
        this.flyScale = scale;
        this.flyAlpha = alpha;
        this.flyPivotX = pivotX;
        this.flyPivotY = pivotY;
        invalidate();
    }

    private float flyPivotX, flyPivotY;

    // ---- draw -----------------------------------------------------------------------

    @Override protected void onDraw(Canvas canvas) {
        final int save = canvas.save();
        if (flyScale != 1f || flyX != 0f || flyY != 0f) {
            canvas.scale(flyScale, flyScale, flyPivotX, flyPivotY);
            canvas.translate(flyX, flyY);
        }

        final long t = elapsed;
        final boolean outro = t >= OUTRO_AT;

        if (titleMode) {
            // Settled pixels at rest: no spawn offsets, no dispersal, no HUD.
            for (Pixel p : open) {
                px.setColor(FG);
                px.setAlpha(255);
                float x = p.x, y = p.y;
                canvas.drawRect(x, y, x + cell, y + cell, px);
            }
            canvas.restoreToCount(save);
            return;
        }

        drawNoise(canvas, t);
        drawWord(canvas, open, t, outro);
        drawWord(canvas, watch, t, outro);

        if (!outro) {
            drawBeam(canvas, t);
            drawLines(canvas, t);
            drawCorners(canvas, t);
            drawCursor(canvas, t);
        }
        drawHud(canvas, t, outro);

        // blackout: the reference's final beat. Skipped while flying, because the flight is
        // already carrying the mark to the header — painting the screen black first would hide
        // the very thing that is supposed to be seen moving.
        boolean flying = flyScale != 1f || flyX != 0f || flyY != 0f;
        if (flying) {
            // Only the row that lands in the header travels. Leaving the HUD, the viewfinder
            // and the WATCH line in place made the whole screen shrink instead of one line.
            for (Pixel p : open) {
                px.setColor(FG);
                px.setAlpha(255);
                canvas.drawRect(p.x, p.y, p.x + cell, p.y + cell, px);
            }
            canvas.restoreToCount(save);
            return;
        }
        if (!titleMode && t >= OFF_AT - 200) {
            float k = Math.min(1f, (t - (OFF_AT - 200)) / 480f);
            ink.setStyle(Paint.Style.FILL);
            ink.setColor(Color.argb((int) (255 * k), 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), getHeight(), ink);
        }
        canvas.restoreToCount(save);
    }

    private void drawWord(Canvas canvas, List<Pixel> list, long t, boolean outro) {
        for (Pixel p : list) {
            if (!outro && t < p.delay) {
                // pre-spawn ghost, as in the reference
                px.setColor(FG);
                px.setAlpha(20);
                cell(canvas, p);
                continue;
            }
            float ox, oy, a;
            if (outro) {
                float k = clamp((t - OUTRO_AT - p.odelay) / (float) DISP_DUR);
                float e = easeOut(k);
                a = k < 0.28f ? 1f - (k / 0.28f) * 0.3f : 0.7f * (1f - (k - 0.28f) / 0.72f);
                ox = p.odx * e;
                oy = p.ody * e;
            } else {
                float k = clamp((t - p.delay) / (float) p.dur);
                float move = easeOut(Math.min(1f, k / (p.scan ? 0.55f : 0.45f)));
                if (p.scan) {
                    a = k < 0.55f ? k / 0.55f
                            : k < 0.68f ? 1f - ((k - 0.55f) / 0.13f) * 0.55f
                            : k < 0.82f ? 0.45f + ((k - 0.68f) / 0.14f) * 0.55f
                            : 1f;
                    ox = 0;
                    oy = p.dy * (1 - move);
                } else {
                    a = k < 0.45f ? k / 0.45f
                            : k < 0.58f ? 1f - ((k - 0.45f) / 0.13f) * 0.5f
                            : k < 0.72f ? 0.5f + ((k - 0.58f) / 0.14f) * 0.5f
                            : 1f;
                    ox = p.dx * (1 - move);
                    oy = p.dy * (1 - move);
                }
            }
            if (a <= 0.004f) continue;
            px.setColor(FG);
            px.setAlpha((int) (255 * clamp(a)));
            if (p.scan) {
                // scanline split: 60% bar plus a 25% bar lower down
                float x = p.x + ox, y = p.y + oy;
                canvas.drawRect(x, y, x + cell, y + cell * 0.6f, px);
                canvas.drawRect(x, y + cell * 0.75f, x + cell, y + cell, px);
            } else {
                float x = p.x + ox, y = p.y + oy;
                canvas.drawRect(x, y, x + cell, y + cell, px);
            }
        }
    }

    private void cell(Canvas c, Pixel p) {
        c.drawRect(p.x, p.y, p.x + cell, p.y + cell, px);
    }

    private void drawBeam(Canvas canvas, long t) {
        long start = 120, dur = 620;
        if (t < start) return;
        float k = clamp((t - start) / (float) dur);
        float x = centerX - openW / 2f + openW * k;
        float top = openTop - cell * 2, bottom = openTop + ROWS * cell + cell;
        // the beam fades in and out at the ends rather than appearing abruptly
        float a = k < 0.08f ? k / 0.08f : (k > 0.88f ? (1f - k) / 0.12f : 1f);
        ink.setColor(FG);
        ink.setAlpha((int) (255 * clamp(a) * 0.85f));
        ink.setStrokeWidth(Math.max(1f, cell * 0.16f));
        canvas.drawLine(x, top, x, bottom, ink);
    }

    private void drawLines(Canvas canvas, long t) {
        float left = centerX - openW / 2f;

        // baseline under OPEN CHAT, growing left to right
        long bStart = 160, bDur = 520;
        if (t >= bStart) {
            float k = clamp((t - bStart) / (float) bDur);
            float y = openTop + ROWS * cell + cell * 1.5f;
            ink.setColor(LINE_A);
            ink.setStrokeWidth(Math.max(1f, cell * 0.16f));
            canvas.drawLine(left, y, left + openW * k, y, ink);
        }

        // rule under WATCH, growing right to left
        long wStart = 1180, wDur = 420;
        if (t >= wStart) {
            float k = easeOut(clamp((t - wStart) / (float) wDur));
            float wl = centerX - watchW / 2f;
            float y = watchTop + ROWS * cell + cell * 1.25f;
            ink.setColor(LINE_A);
            ink.setStrokeWidth(Math.max(1f, cell * 0.16f));
            canvas.drawLine(wl + watchW * (1 - k), y, wl + watchW, y, ink);
        }

        // vertical drop line beneath the stack
        long vStart = 100, vDur = 480;
        if (t >= vStart) {
            float k = easeOut(clamp((t - vStart) / (float) vDur));
            float x = centerX;
            float y0 = watchTop + ROWS * cell + cell * 5f;
            ink.setColor(LINE_A);
            ink.setStrokeWidth(Math.max(1f, cell * 0.16f));
            canvas.drawLine(x, y0, x, y0 + getHeight() * 0.22f * k, ink);
        }
    }

    private void drawCorners(Canvas canvas, long t) {
        long start = 1020, dur = 360;
        if (t < start) return;
        float a = clamp((t - start) / (float) dur) * 0.85f;

        float l = centerX - openW / 2f - cell * 2;
        float r = centerX + openW / 2f + cell * 2;
        float top = openTop - cell * 3;
        float bottom = watchTop + ROWS * cell + cell * 3;
        float s = cell * 3.5f;

        ink.setColor(FG);
        ink.setAlpha((int) (255 * a));
        ink.setStrokeWidth(Math.max(1f, cell * 0.16f));
        ink.setStyle(Paint.Style.STROKE);

        canvas.drawLine(l, top, l + s, top, ink);
        canvas.drawLine(l, top, l, top + s, ink);

        canvas.drawLine(r - s, top, r, top, ink);
        canvas.drawLine(r, top, r, top + s, ink);

        canvas.drawLine(l, bottom, l + s, bottom, ink);
        canvas.drawLine(l, bottom - s, l, bottom, ink);

        canvas.drawLine(r - s, bottom, r, bottom, ink);
        canvas.drawLine(r, bottom - s, r, bottom, ink);

        ink.setStyle(Paint.Style.FILL);
    }

    private void drawCursor(Canvas canvas, long t) {
        long start = 1480;
        if (t < start) return;
        // 1.3s cycle, on for the first 52%
        long phase = (t - start) % 1300;
        float a = phase < 676 ? 1f : 0f;
        if (a <= 0) return;
        float x = centerX + openW / 2f + cell * 1.5f;
        float y = openTop + ROWS * cell - cell * 7;
        px.setColor(FG);
        px.setAlpha(255);
        canvas.drawRect(x, y, x + cell, y + cell * 7, px);
    }

    private void drawNoise(Canvas canvas, long t) {
        float w = getWidth(), h = getHeight();
        px.setColor(FG);
        for (float[] n : noise) {
            // gentle twinkle: 0.05 -> 0.24, never bright enough to compete with the mark
            float tw = (float) (0.145f + 0.095f * Math.sin(t / 1400.0 + n[3]));
            px.setAlpha((int) (255 * tw));
            float x = n[0] * w, y = n[1] * h;
            if (n[2] > 0.5f) {
                float s = cell * 1.2f;
                canvas.drawRect(x, y + s / 2f, x + s, y + s / 2f + 1f, px);
                canvas.drawRect(x + s / 2f, y, x + s / 2f + 1f, y + s, px);
            } else {
                canvas.drawRect(x, y, x + cell * 1.4f, y + 1f, px);
            }
        }
    }

    private void drawHud(Canvas canvas, long t, boolean outro) {
        float w = getWidth(), h = getHeight();
        float pad = Math.max(cell * 2f, 12f);

        text.setTextSize(Math.max(9f, cell * 1.7f));
        float fade = outro ? Math.max(0f, 1f - (t - OUTRO_AT) / 360f) : 1f;

        // top bar
        ink.setColor(FG);
        ink.setAlpha((int) (255 * fade));
        ink.setStrokeWidth(Math.max(1f, cell * 0.16f));
        long dStart = 60, dDur = 560;
        if (t >= dStart) {
            float k = easeOut(clamp((t - dStart) / (float) dDur));
            canvas.drawLine(pad, pad + cell * 2f, pad + (w - pad * 2) * k, pad + cell * 2f, ink);
        }

        text.setColor(MUTED);
        text.setAlpha((int) (255 * fade));
        canvas.drawText("OPEN CHAT · WATCH", pad, pad + cell * 1.4f, text);

        String sig = t >= OUTRO_AT ? "SIG.OFF" : (t >= 1480 ? "SIG.OK" : "SIG.—");
        text.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(sig, w - pad, pad + cell * 1.4f, text);
        text.setTextAlign(Paint.Align.LEFT);

        // bottom: tick row that fills as each character lands
        float by = h - pad;
        long bStart = 240, bDur = 560;
        if (t >= bStart) {
            float k = easeOut(clamp((t - bStart) / (float) bDur));
            canvas.drawLine(w - pad - (w - pad * 2) * k, by - cell * 2f, w - pad, by - cell * 2f, ink);
        }

        int n = tickDelays.size();
        if (n > 0) {
            float tw = Math.max(4f, cell * 1.6f);
            float gap = Math.max(2f, cell * 0.7f);
            float totalW = Math.min(w - pad * 2, n * (tw + gap));
            float x = pad;
            for (int i = 0; i < n && x < pad + totalW; i++) {
                float on = clamp((t - tickDelays.get(i) - 160) / 220f);
                ink.setColor(0x29F2F2F0);
                ink.setAlpha((int) (255 * fade * 0.16f));
                canvas.drawRect(x, by, x + tw, by + 1f, ink);
                if (on > 0) {
                    ink.setColor(FG);
                    ink.setAlpha((int) (255 * fade));
                    canvas.drawRect(x, by, x + tw * on, by + 1f, ink);
                }
                x += tw + gap;
            }
        }

        text.setColor(MUTED);
        text.setAlpha((int) (255 * fade * 0.9f));
        text.setTextSize(Math.max(8f, cell * 1.5f));
        canvas.drawText("BOOT", pad, by - cell * 3.2f, text);
    }

    // ---- curves ---------------------------------------------------------------------

    private static float clamp(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float easeOut(float t) { float u = 1f - clamp(t); return 1f - u * u * u; }
}
