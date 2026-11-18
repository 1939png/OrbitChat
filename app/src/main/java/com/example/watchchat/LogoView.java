package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Static wordmark for the chat page: same two faces as the intro, same hierarchy. */
final class LogoView extends View {
    private final Paint paint = new Paint();
    private final Typeface faceOne, faceTwo;
    private final int accent;

    LogoView(Context c, int accentColor, int unused) {
        super(c);
        accent = accentColor;
        faceOne = PixelText.typeface(c, R.font.press_start);
        faceTwo = PixelText.typeface(c, R.font.silkscreen);
        setContentDescription("OPEN CHAT WATCH");
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cell = fit("OPEN CHAT", faceOne, w * 0.88f, h * 0.38f);
        float cellTwo = Math.max(5f, Math.round(cell * 0.86f));
        PixelText.drawLine(canvas, "OPEN CHAT", w / 2f, h * 0.30f, cell, Color.WHITE, faceOne, paint);
        PixelText.drawLine(canvas, "WATCH", w / 2f, h * 0.70f, cellTwo, accent, faceTwo, paint);
    }

    private float fit(String text, Typeface face, float maxWidth, float maxHeight) {
        float size = Math.max(4f, Math.round(maxHeight));
        while (size > 4f) {
            float lineW = 0f;
            for (int i = 0; i < text.length(); i++) lineW += PixelText.advance(text.charAt(i), size, face);
            if (lineW <= maxWidth) break;
            size -= 1f;
        }
        return size;
    }
}
