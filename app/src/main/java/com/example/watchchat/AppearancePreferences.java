package com.example.watchchat;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

/** Stores appearance-only preferences and the private wallpaper file. */
final class AppearancePreferences {
    static final String PREFS = "appearance";
    static final String KEY_BLUR = "dynamic_blur";
    static final String KEY_BLUR_LEVEL = "blur_level";
    static final String KEY_THEME = "theme";
    /** false = dark (default), true = light. */
    static final String KEY_LIGHT = "light_mode";
    static final String DEFAULT_THEME = "墨绿";
    static final int DEFAULT_BLUR_LEVEL = 1;
    private AppearancePreferences() {}
    static SharedPreferences preferences(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static boolean blurEnabled(Context context) { return preferences(context).getBoolean(KEY_BLUR, true); }
    static int blurLevel(Context context) { return Math.max(0, Math.min(4, preferences(context).getInt(KEY_BLUR_LEVEL, DEFAULT_BLUR_LEVEL))); }
    static boolean lightMode(Context context) { return preferences(context).getBoolean(KEY_LIGHT, false); }

    static void setLightMode(Context context, boolean light) {
        preferences(context).edit().putBoolean(KEY_LIGHT, light).apply();
    }

    /** Background for both modes; the whole app reads this instead of a literal black. */
    static int background(Context context) { return lightMode(context) ? 0xfff4f4f2 : 0xff000000; }

    /** Primary text colour for both modes. */
    static int ink(Context context) { return lightMode(context) ? 0xff000000 : 0xfff2f2f2; }

    /** Secondary text for both modes. */
    static int inkMuted(Context context) { return lightMode(context) ? 0xff000000 : 0xff9a9a9a; }

    /** Panel fill for both modes. */
    static int surface(Context context) { return lightMode(context) ? 0xffe8e8e4 : 0xff20201e; }

    static String theme(Context context) { return preferences(context).getString(KEY_THEME, DEFAULT_THEME); }
    static int accent(Context c) { String t=theme(c); return "深蓝".equals(t)?0xff69b7ff:"紫灰".equals(t)?0xffc69cff:0xffd8ff67; }
    static int accentStrong(Context c) { String t=theme(c); return "深蓝".equals(t)?0xffb8ddff:"紫灰".equals(t)?0xffe3d1ff:0xffedffb1; }
    static int panel(Context c) {
        if (lightMode(c)) return 0xffffffff;   // light mode: white cards on off-white ground
        String t=theme(c); return "深蓝".equals(t)?0xff111f31:"紫灰".equals(t)?0xff272230:0xff20291f;
    }
    static int mutedAccent(Context c) { String t=theme(c); return "深蓝".equals(t)?0xff9cc9ee:"紫灰".equals(t)?0xffc8b9dc:0xffa8c99a; }
    static File wallpaperFile(Context context) { return new File(new File(context.getFilesDir(), "appearance"), "wallpaper"); }
    static void reset(Context context) {
        preferences(context).edit().clear().apply();
        File file = wallpaperFile(context);
        if (file.exists()) file.delete();
        File parent = file.getParentFile();
        if (parent != null && parent.exists()) parent.delete();
    }
}
