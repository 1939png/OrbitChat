package com.example.watchchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

/**
 * Font catalogue and size scale for the whole app.
 *
 * The choice is stored in preferences and applied on the next start, because changing the
 * typeface means rebuilding every page — doing that live would interrupt whatever the user
 * is in the middle of.
 */
final class FontStore {
    private static final String PREFS = "chat_features";
    private static final String KEY_SIZE = "font_size_level";

    /** Ten steps. Level 4 (index) is the default. */
    static final int SIZE_LEVELS = 10;
    static final int DEFAULT_SIZE = 4;

    private static Typeface CACHE;

    private FontStore() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int sizeLevel(Context c) {
        int i = prefs(c).getInt(KEY_SIZE, DEFAULT_SIZE);
        return Math.max(0, Math.min(SIZE_LEVELS - 1, i));
    }

    static void setSizeLevel(Context c, int level) {
        prefs(c).edit().putInt(KEY_SIZE, Math.max(0, Math.min(SIZE_LEVELS - 1, level))).apply();
    }

    /**
     * One face for the whole app. Inter is chosen because it stays legible at watch sizes and
     * has a proper CJK fallback chain; the pixel faces remain in res/font for the wordmark
     * only, where their low legibility is the point.
     */
    static Typeface typeface(Context c) {
        if (CACHE != null) return CACHE;
        try { CACHE = c.getResources().getFont(R.font.inter); }
        catch (Throwable t) { CACHE = Typeface.create("sans-serif", Typeface.NORMAL); }
        return CACHE;
    }

    /** Always false now: the UI face is a normal outline font. */
    static boolean isPixel(Context c) { return false; }

    /**
     * Body text size in sp for the current level. The scale spans roughly 11 → 26 sp in even
     * steps, so every level is visibly different without any of them becoming unusable.
     */
    static float bodySize(Context c) {
        int level = sizeLevel(c);
        return 11f + level * 1.65f;
    }

    /** Secondary text is a fixed proportion of the body size, so the scale stays harmonious. */
    static float uiSize(Context c) {
        return bodySize(c) * 0.86f;
    }
}
