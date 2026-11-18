package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.TextView;

/** Arrow / stop glyph drawn as a path so it never depends on the device font. */
final class SendButton extends TextView {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);

    SendButton(Context context) {
        super(context);
        setGravity(android.view.Gravity.CENTER);
    }

    @Override protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float x = getWidth() / 2f, y = getHeight() / 2f;
        // The key is dark in light mode (so it still reads as the primary action), which means
        // the icon has to flip too or it disappears into the fill.
        ink.setColor(AppearancePreferences.lightMode(getContext()) ? 0xFFF4F4F2 : 0xFFFFFFFF);
        ink.setStrokeWidth(2.4f * d);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        if ("■".contentEquals(getText())) {
            ink.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(x - 6 * d, y - 6 * d, x + 6 * d, y + 6 * d, 2 * d, 2 * d, ink);
        } else {
            ink.setStyle(Paint.Style.STROKE);
            Path icon = new Path();
            icon.moveTo(x - 9 * d, y - 7 * d);
            icon.lineTo(x + 10 * d, y);
            icon.lineTo(x - 9 * d, y + 7 * d);
            icon.lineTo(x - 5 * d, y);
            icon.close();
            canvas.drawPath(icon, ink);
            canvas.drawLine(x - 5 * d, y, x + 9 * d, y, ink);
        }
    }
}
