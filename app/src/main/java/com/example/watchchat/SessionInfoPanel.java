package com.example.watchchat;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import com.example.watchchat.data.ModelSettings;
import com.example.watchchat.data.SessionRepository;
import com.example.watchchat.data.PromptStore;
import com.example.watchchat.data.TokenEstimator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Per-conversation report. Seven numbers, no prose.
 *
 * The previous version explained its own methodology in four paragraphs under every figure.
 * That reads as hedging, not as information: the user opened this to see numbers, and any
 * caveat belongs in the panel only when the number is genuinely missing.
 *
 * The context bar is a stacked proportion bar rather than a single fill: system prompt,
 * tool definitions and conversation each get their own segment in their own colour, so the
 * composition is legible at a glance without a legend being strictly necessary.
 */
public class SessionInfoPanel extends Dialog {

    interface OnRename { void onRename(SessionRepository.Session session); }

    // Segment colours. Grey = instructions, violet = tools, blue = messages. These are
    // deliberately outside the accent palette: they must stay distinguishable from the
    // user's chosen accent and from each other.
    private static final int C_SYSTEM = 0xFF8A8A8E;
    private static final int C_MSG    = 0xFF4A9EFF;
    private static final int C_FREE   = 0x00000000;

    private final SessionRepository.Session session;
    /** The real Activity. getContext() on a Dialog returns a ContextThemeWrapper,
     *  which cannot be cast — that cast crashed the export button. */
    private final android.app.Activity host;
    /** The dialog's own content root: hints must live here, not on the activity. */
    private android.view.ViewGroup sheetRoot;
    private final int size, foreground, secondary, accent;
    private OnRename onRename;

    public SessionInfoPanel(android.app.Activity activity, SessionRepository.Session session,
                            int size, int foreground, int secondary, int accent) {
        super(activity);
        this.session = session;
        this.host = activity;
        this.size = size;
        this.foreground = foreground;
        this.secondary = secondary;
        this.accent = accent;
        build(activity);
    }

    void setOnRename(OnRename l) { onRename = l; }

    private int dp(float v) {
        return (int) (v * getContext().getResources().getDisplayMetrics().density + .5f);
    }

    private boolean light() { return AppearancePreferences.lightMode(getContext()); }

    private void build(Context ctx) {
        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(light() ? 0xFFF4F4F2 : 0xFF08090A);

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(16));

        // ---- header ----
        TextView back = text("‹  返回", size, accent);
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setMinHeight(dp(44));
        back.setOnClickListener(v -> dismiss());
        content.addView(back);

        TextView title = text(session.title == null ? "对话信息" : session.title, size + 3, foreground);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(0, dp(2), 0, dp(10));
        content.addView(title);

        // ---- context occupancy bar ----
        content.addView(sectionLabel("上下文占用"));
        int contextLimit = Math.max(1, ModelSettings.get(ctx, pidOf(), modelOf()).contextTokens());

        // The prompt segment is everything the app prepends to the conversation: the text the
        // user wrote in settings, plus whatever the knowledge base retrieved for the last turn.
        // There is no separate "tool" segment because nothing is sent as a tool call — folding
        // it in here is what makes the bar match what the request actually contains.
        int promptTokens = TokenEstimator.estimate(PromptStore.systemPrompt(ctx));
        int msgTokens = session.contextTokens == null ? 0 : session.contextTokens;

        ContextBar bar = new ContextBar(ctx);
        bar.setData(promptTokens, msgTokens, contextLimit);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(26));
        blp.topMargin = dp(2);
        content.addView(bar, blp);

        // ---- the seven figures ----
        int used = promptTokens + msgTokens;
        addRow(content, "模型", modelOf());
        addRow(content, "用时", elapsed());
        addRow(content, "Token 用量", tokens());
        addRow(content, "缓存命中率", cacheRate());
        addRow(content, "未缓存输入", count(session.inputTokens, true));
        addRow(content, "缓存输入", count(session.cacheHitTokens, true));
        addRow(content, "输出 Token", count(session.outputTokens, true));
        addRow(content, "上下文占用", pct(used, contextLimit));

        // ---- export: a support tool, not a metric, so it sits below the numbers ----
        TextView export = text("导出日志", size - 1, secondary);
        export.setGravity(Gravity.CENTER);
        export.setMinHeight(dp(40));
        GradientDrawable ebg = new GradientDrawable();
        ebg.setColor(light() ? 0xffffffff : 0xFF1A1A1C);
        ebg.setCornerRadius(dp(12));
        ebg.setStroke(dp(1), light() ? 0xFFDCDCD8 : 0xFF2E2E30);
        export.setBackground(ebg);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, dp(40));
        elp.topMargin = dp(6);
        content.addView(export, elp);
        export.setOnClickListener(v -> exportLogs(export));

        // ---- legend, two swatches, one line ----
        content.addView(legend(ctx, promptTokens, msgTokens));
        // The absolute total belongs with the bar, not in a cramped row: this line has the
        // full width to itself and reads as the bar's caption.
        TextView total = text(compact(used) + " / " + compact(contextLimit) + " tokens",
                size - 2, secondary);
        total.setGravity(Gravity.END);
        total.setPadding(0, dp(4), 0, 0);
        content.addView(total);

        TextView close = text("返回历史", size, accent);
        close.setGravity(Gravity.CENTER);
        close.setMinHeight(dp(48));
        close.setPadding(0, dp(10), 0, 0);
        close.setOnClickListener(v -> dismiss());
        content.addView(close);

        scroll.addView(content);

        // A FrameLayout wraps the ScrollView so a hint can float above it: a ScrollView accepts
        // exactly one direct child, so attaching the hint to it throws.
        android.widget.FrameLayout window = new android.widget.FrameLayout(ctx);
        window.addView(scroll, new android.widget.FrameLayout.LayoutParams(-1, -1));
        window.setBackgroundColor(light() ? 0xFFF4F4F2 : 0xFF08090A);
        this.sheetRoot = window;
        setContentView(window);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.55f;
            w.setAttributes(lp);
        }
    }

    // ---- rows ---------------------------------------------------------------------------

    private void addRow(LinearLayout host, String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        TextView k = text(label, size - 1, secondary);
        k.setSingleLine(true);
        row.addView(k, new LinearLayout.LayoutParams(0, -2, 0.85f));

        TextView v = text(value, size, foreground);
        v.setGravity(Gravity.END);
        v.setSingleLine(true);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 1.15f));

        host.addView(row);
        host.addView(hairline());
    }

    private LinearLayout legend(Context ctx, int prompt, int msg) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(0, dp(8), 0, 0);
        // “提示词” covers the system prompt and the retrieved knowledge material, because both
        // are prepended by the app and both count against the window.
        box.addView(swatch(ctx, C_SYSTEM, "提示词 " + compact(prompt)));
        box.addView(swatch(ctx, C_MSG, "消息 " + compact(msg)));
        return box;
    }

    private LinearLayout swatch(Context ctx, int color, String label) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);

        android.view.View dot = new android.view.View(ctx);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        dot.setBackground(d);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dlp.rightMargin = dp(4);
        cell.addView(dot, dlp);

        TextView t = text(label, size - 3, secondary);
        t.setSingleLine(true);
        cell.addView(t);

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, -2, 1f);
        clp.rightMargin = dp(6);
        cell.setLayoutParams(clp);
        return cell;
    }

    private TextView sectionLabel(String s) {
        TextView t = text(s, size - 2, secondary);
        t.setPadding(0, dp(6), 0, 0);
        return t;
    }

    private android.view.View hairline() {
        android.view.View v = new android.view.View(getContext());
        v.setBackgroundColor(light() ? 0xFFE2E2DE : 0xFF23252A);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
        return v;
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(getContext());
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(FontStore.typeface(getContext()));
        return v;
    }

    // ---- values -------------------------------------------------------------------------

    /** Session rows do not carry the model id; read the active one, which is what recorded it. */
    private String pidOf() {
        com.example.watchchat.data.ProviderStore.Provider p =
                com.example.watchchat.data.ProviderStore.activeProvider(getContext());
        return p == null ? "" : p.id;
    }

    private String modelOf() {
        return com.example.watchchat.data.ProviderStore.activeModel(getContext());
    }

    /** 1048576 reads as 1.05M. A 500px watch cannot show seven digits beside a label,
     *  and the exact unit does not change any decision the user is making. */

    /**
     * Writes the report and tells the user where it landed.
     *
     * The filename goes into the confirmation because "saved to Downloads" is not actionable
     * on its own — the user needs the exact name to find it among whatever else is there.
     */
    private void exportLogs(TextView button) {
        android.app.Activity act = host;
        button.setEnabled(false);
        button.setText("导出中…");

        final String[] name = new String[1];
        final String[] error = new String[1];
        new Thread(() -> {
            try {
                SessionRepository repo = new SessionRepository(act);
                java.util.List<com.example.watchchat.data.ChatMessage> msgs = repo.getMessages(session.id);
                String body = Diagnostics.buildReport(act, repo, session, msgs);
                name[0] = Diagnostics.export(act, session.title, body);
            } catch (Throwable t) {
                error[0] = t.getMessage();
            }
            act.runOnUiThread(() -> {
                button.setEnabled(true);
                button.setText("导出日志");
                // Attached to this dialog's root, not the activity's: a hint on the activity
                // paints underneath the dialog window and is never visible.
                if (name[0] != null) {
                    Hints.showIn(act, sheetRoot, "已导出到下载：" + name[0], 2600, 24);
                } else {
                    Hints.showIn(act, sheetRoot, "导出失败"
                            + (error[0] == null ? "" : "：" + error[0]), 2600, 24);
                }
            });
        }).start();
    }

    private static String compact(long n) {
        if (n < 1000) return String.valueOf(n);
        if (n < 1_000_000) return String.format(Locale.CHINA, "%.1fK", n / 1000.0);
        return String.format(Locale.CHINA, "%.2fM", n / 1_000_000.0);
    }

    private static String compact(Integer n) { return n == null ? null : compact(n.longValue()); }

    private String missing() { return session.latestRecorded ? "未返回" : "未记录"; }

    private String count(Integer v, boolean withUnit) {
        if (v == null) return missing();
        return withUnit ? compact(v) + " tokens" : compact(v);
    }

    private String tokens() {
        Integer in = session.inputTokens, out = session.outputTokens;
        if (in == null && out == null) return missing();
        long a = in == null ? 0 : in, b = out == null ? 0 : out;
        return compact(a + b) + " tokens";
    }

    /** cached / total input as a percentage. The counts live on their own rows, so this
     *  row carries the rate alone — it is the number the user is scanning for. */
    private String cacheRate() {
        Integer in = session.inputTokens;
        Integer cached = session.cacheHitTokens;

        // "The server did not report cached tokens" is not "the cache missed". Treating a
        // missing field as 0 printed 0.0% on every provider that simply omits the key, which
        // reads as a dead cache rather than as absent data.
        if (cached == null) return missing();
        if (in == null || in <= 0) return missing();

        // A server that reports more cached tokens than input tokens is inconsistent; the
        // clamp keeps the rate at 100% instead of inventing a number above it.
        int c = Math.min(cached, in);
        return String.format(Locale.CHINA, "%.1f%%", c * 100.0 / in);
    }

    private String elapsed() {
        long ms = session.elapsedMs;
        if (ms <= 0) return missing();
        if (ms < 1000) return ms + " ms";
        return String.format(Locale.CHINA, "%.1f s", ms / 1000.0);
    }

    private static String pct(int used, int limit) {
        return String.format(Locale.CHINA, "%.1f%%", used * 100.0 / Math.max(1, limit));
    }

    // ---- stacked bar --------------------------------------------------------------------

    /** Proportion bar: three segments, rounded ends, no animation. */
    private static final class ContextBar extends android.view.View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private final android.graphics.Path pill = new android.graphics.Path();
        private int prompt, msg, limit = 1;

        ContextBar(Context c) { super(c); }

        void setData(int promptTokens, int messages, int contextLimit) {
            prompt = Math.max(0, promptTokens);
            msg = Math.max(0, messages);
            limit = Math.max(1, contextLimit);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            float barH = Math.min(h, dp(12));
            box.set(0, (h - barH) / 2f, w, (h + barH) / 2f);
            float r = barH / 2f;

            // The track is the shape. Every segment is painted as a plain rectangle and the
            // canvas is clipped to the track, so a 4dp segment inherits the track's rounding
            // instead of getting a 2dp corner radius of its own — which is what made narrow
            // fills look like a stray vertical pill sitting at the edge of the bar.
            int trackColor = AppearancePreferences.lightMode(getContext())
                    ? 0xFFE2E2DE : 0xFF1E1E20;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(trackColor);
            canvas.drawRoundRect(box, r, r, paint);

            int used = prompt + msg;
            if (used <= 0) return;

            final int save = canvas.save();
            // clipRect would leave the fill square at both ends. A rounded clip is what makes
            // a segment inherit the track's curve instead of drawing its own corner radius.
            pill.reset();
            pill.addRoundRect(box, r, r, android.graphics.Path.Direction.CW);
            canvas.clipPath(pill);
            float inner = box.left, innerRight = box.right;

            float total = Math.max(1, limit);
            // Reserve the minimum gaps first so proportions stay honest for the big segments.
            float minW = dp(4f);
            int smallCount = 0;
            if (prompt > 0 && w * prompt / total < minW) smallCount++;
            if (msg > 0 && w * msg / total < minW) smallCount++;
            float budget = Math.max(0f, innerRight - inner - smallCount * minW);

            float cursor = inner;
            cursor = segment(canvas, cursor, budget, total, prompt, C_SYSTEM, minW);
            segment(canvas, cursor, budget, total, msg, C_MSG, minW);

            canvas.restoreToCount(save);

            // A hairline keeps the track legible against a light page, where a pale fill would
            // otherwise dissolve into the background.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, dp(1f)));
            paint.setColor(AppearancePreferences.lightMode(getContext())
                    ? 0x14000000 : 0x1AFFFFFF);
            canvas.drawRoundRect(box, r, r, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        /**
         * One segment as a plain rectangle. Width is the true proportion, floored at minW so a
         * tiny category stays visible; the floor is taken out of the shared budget before the
         * proportions are computed, so the bar never overflows.
         */
        private float segment(Canvas c, float from, float budget, float total,
                              int amount, int color, float minW) {
            if (amount <= 0) return from;
            float raw = budget * (amount / total);
            float len = Math.max(raw, minW);
            paint.setColor(color);
            RectF seg = new RectF(from, box.top, from + len, box.bottom);
            c.drawRect(seg, paint);
            return from + len;
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }
    }
}
