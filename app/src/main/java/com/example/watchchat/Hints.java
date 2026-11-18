package com.example.watchchat;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * In-app status line, replacing android.widget.Toast.
 *
 * Why not Toast: it is a system overlay at the screen edge, it owns its own lifetime, and on
 * a 500px watch it covers the very control the user just touched. This version lives inside
 * the app's own view tree, sits just above the composer, and never steals the whole width.
 *
 * Behaviour:
 *  - One at a time. A second message replaces the first instead of queueing, so a burst of
 *    status updates cannot stack up over the UI.
 *  - Rises 8dp and fades in over 180ms, leaves 160ms after 1.9s. EASE_OUT both ways, so it
 *    never looks like it is being yanked away.
 *  - Non-interactive: it must never eat a tap meant for the UI underneath.
 */
final class Hints {

    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static TextView current;
    private static ViewGroup host;
    private static Runnable dismiss;

    private Hints() { }

    static void show(Activity activity, String message) {
        show(activity, message, 1900L);
    }

    static void show(Activity activity, String message, long holdMs) {
        if (activity == null) return;
        showIn(activity, activity.findViewById(android.R.id.content), message, holdMs, 96);
    }

    /**
     * Shows the hint inside a specific container, for surfaces that are their own window.
     *
     * A Dialog has its own window above the activity's, so a hint attached to
     * android.R.id.content is painted underneath the dialog and never seen — which is why
     * the export confirmation appeared to do nothing. Pass the dialog's own root instead.
     */
    static void showIn(Activity activity, ViewGroup container, String message,
                       long holdMs, int bottomMarginDp) {
        if (activity == null || container == null || message == null || message.isEmpty()) return;
        if (activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup root = container;

        // Drop any hint still on screen: the newest message is the true state.
        clear();

        TextView tv = new TextView(activity);
        tv.setText(message);
        tv.setTextSize(11.5f);
        tv.setTextColor(0xFFEDEDED);
        tv.setTypeface(FontStore.typeface(activity));
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(2);
        tv.setPadding(dp(activity, 14), dp(activity, 7), dp(activity, 14), dp(activity, 7));
        tv.setClickable(false);
        tv.setFocusable(false);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF21A1A1A);
        bg.setCornerRadius(dp(activity, 14));
        bg.setStroke(Math.max(1, dp(activity, 1)), 0x33FFFFFF);
        tv.setBackground(bg);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = dp(activity, bottomMarginDp);
        lp.leftMargin = dp(activity, 24);
        lp.rightMargin = dp(activity, 24);

        tv.setAlpha(0f);
        tv.setTranslationY(dp(activity, 8));
        root.addView(tv, lp);

        current = tv;
        host = root;

        tv.animate().alpha(1f).translationY(0f)
                .setDuration(180L)
                .setInterpolator(VisualEffects.EASE_OUT)
                .start();

        Runnable fade = () -> {
            if (current != tv) return;   // a newer hint already took over
            tv.animate().alpha(0f).translationY(dp(activity, 6))
                    .setDuration(160L)
                    .setInterpolator(VisualEffects.EASE_OUT)
                    .withEndAction(() -> remove(tv, root))
                    .start();
        };
        dismiss = fade;
        UI.postDelayed(fade, Math.max(400L, holdMs));
    }

    /** Removes the visible hint immediately. Safe to call when nothing is showing. */
    static void clear() {
        if (dismiss != null) {
            UI.removeCallbacks(dismiss);
            dismiss = null;
        }
        if (current != null) {
            TextView v = current;
            ViewGroup g = host;
            current = null;
            host = null;
            if (v.animate() != null) v.animate().cancel();
            remove(v, g);
        }
    }

    private static void remove(View v, ViewGroup parent) {
        if (v == null || parent == null) return;
        if (v.getParent() == parent) parent.removeView(v);
    }

    private static int dp(Activity a, float v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + .5f);
    }
}
