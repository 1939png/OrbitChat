package com.example.watchchat;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/** Pixel type helpers: whole-pixel sizing and anti-alias-free direct glyph drawing. */
final class PixelText {
    private PixelText() { }

    static Typeface typeface(android.content.Context c, int fontRes) {
        try { return c.getResources().getFont(fontRes); }
        catch (Throwable t) { return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); }
    }

    static float advance(char ch, float pixelSize, Typeface face) {
        Paint p = new Paint();
        p.setTypeface(face);
        p.setTextSize(pixelSize);
        p.setLetterSpacing(0f);
        return p.measureText(String.valueOf(ch));
    }

    /**
     * Draws one glyph centred on {@code centerY}, anti-aliasing off and the baseline snapped
     * to a whole pixel — that is what keeps a bitmap face from turning to mush.
     */
    static void drawGlyph(Canvas canvas, char ch, float x, float centerY,
                          float pixelSize, int color, Typeface face, Paint paint) {
        paint.reset();
        paint.setTypeface(face);
        paint.setTextSize(pixelSize);
        paint.setColor(color);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
        paint.setStyle(Paint.Style.FILL);
        paint.setLetterSpacing(0f);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float glyphCenter = (fm.ascent + fm.descent) / 2f;
        canvas.drawText(String.valueOf(ch), Math.round(x), Math.round(centerY - glyphCenter), paint);
    }

    static float drawLine(Canvas canvas, String text, float centerX, float centerY,
                          float pixelSize, int color, Typeface face, Paint paint) {
        float total = 0f;
        for (int i = 0; i < text.length(); i++) total += advance(text.charAt(i), pixelSize, face);
        float x = centerX - total / 2f;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            drawGlyph(canvas, ch, x, centerY, pixelSize, color, face, paint);
            x += advance(ch, pixelSize, face);
        }
        return total;
    }

    static float spring(float t, float dampingRatio, float angularFrequency) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float wd = angularFrequency * (float) Math.sqrt(Math.max(0f, 1f - dampingRatio * dampingRatio));
        if (wd < 0.0001f) return 1f;
        float e = (float) Math.exp(-dampingRatio * angularFrequency * t);
        return 1f - e * ((float) Math.cos(wd * t)
                + (dampingRatio * angularFrequency / wd) * (float) Math.sin(wd * t));
    }

    static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
