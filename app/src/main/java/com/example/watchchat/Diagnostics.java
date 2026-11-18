package com.example.watchchat;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.watchchat.data.ChatMessage;
import com.example.watchchat.data.ModelSettings;
import com.example.watchchat.data.ProviderStore;
import com.example.watchchat.data.SessionRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Writes a plain-text diagnostic report for one conversation.
 *
 * What goes in and what stays out: the report is meant to be sendable to someone helping with
 * a connection problem, so it carries the request shape — provider, protocol, model, the
 * reasoning suffix that was actually sent, the token accounting, and every message. The API
 * key is never included; it is replaced by its length and last four characters, which is
 * enough to tell two keys apart without exposing either.
 *
 * Destination: the public Downloads collection, so the file is reachable from a file manager
 * without the app being granted any storage permission on Android 10+.
 */
final class Diagnostics {

    private Diagnostics() { }

    /** Filename: conversation title plus a timestamp, with anything path-hostile removed. */
    private static String fileName(String title, Date when) {
        String base = title == null ? "" : title.trim();
        if (base.isEmpty()) base = "\u5bf9\u8bdd";
        // strip characters that break a filename on any platform
        base = base.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_");
        if (base.length() > 40) base = base.substring(0, 40);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(when);
        return base + "_" + stamp + ".txt";
    }

    static String buildReport(Context c, SessionRepository repo,
                              SessionRepository.Session s, List<ChatMessage> messages) {
        StringBuilder b = new StringBuilder();
        Date now = new Date();
        SimpleDateFormat full = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

        b.append("OrbitChat \u8bca\u65ad\u65e5\u5fd7\n");
        b.append("\u751f\u6210\u65f6\u95f4: ").append(full.format(now)).append('\n');
        b.append('\n');

        // ---- environment ----
        b.append("== \u8bbe\u5907 ==\n");
        b.append("\u8bbe\u5907: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        b.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        b.append("App: ").append(version(c)).append('\n');
        b.append("\u4e3b\u9898: ").append(AppearancePreferences.lightMode(c) ? "\u4eae\u8272" : "\u6df1\u8272").append('\n');
        b.append('\n');

        // ---- provider ----
        ProviderStore.Provider p = ProviderStore.activeProvider(c);
        String model = ProviderStore.activeModel(c);
        b.append("== \u6a21\u578b ==\n");
        if (p == null) {
            b.append("\u672a\u914d\u7f6e\u4f9b\u5e94\u5546\n");
        } else {
            b.append("\u4f9b\u5e94\u5546: ").append(p.displayName).append('\n');
            b.append("\u534f\u8bae: ").append(p.protocol).append('\n');
            b.append("\u5730\u5740: ").append(p.baseUrl).append('\n');
            b.append("\u6a21\u578b: ").append(model).append('\n');
            b.append("Key: ").append(maskKey(p.apiKey)).append('\n');

            ModelSettings ms = ModelSettings.get(c, p.id, model);
            b.append("\u601d\u8003\u5f3a\u5ea6(\u53d1\u9001\u503c): ")
                    .append(ms.reasoningValue(c, p.id, model)).append('\n');
            b.append("\u4e0a\u4e0b\u6587\u4e0a\u9650: ").append(ms.contextTokens()).append('\n');
            b.append("\u6700\u5927\u8f93\u51fa: ").append(ms.outputTokens()).append('\n');
            b.append("\u542f\u7528\u7684\u6863\u4f4d: ").append(ModelSettings.getEnabledReasoning(c, p.id, model)).append('\n');
            b.append("\u81ea\u5b9a\u4e49\u540e\u7f00: ").append(ModelSettings.getCustomReasoning(c, p.id, model)).append('\n');
            String sp = com.example.watchchat.data.PromptStore.systemPrompt(c);
            b.append("\u7cfb\u7edf\u63d0\u793a\u8bcd: ").append(sp.isEmpty() ? "\u672a\u8bbe\u7f6e"
                    : com.example.watchchat.data.TokenEstimator.estimate(sp) + " tokens").append('\n');
        }
        b.append('\n');

        // ---- token accounting ----
        b.append("== \u672c\u6b21\u4f1a\u8bdd ==\n");
        b.append("\u6807\u9898: ").append(s.title).append('\n');
        b.append("\u6d88\u606f\u6570: ").append(messages == null ? 0 : messages.size()).append('\n');
        b.append("\u8bf7\u6c42\u6b21\u6570: ").append(s.requestCount == null ? "\u672a\u8bb0\u5f55" : s.requestCount).append('\n');
        b.append("\u8f93\u5165 token: ").append(s.inputTokens == null ? "\u672a\u8fd4\u56de" : s.inputTokens).append('\n');
        b.append("\u7f13\u5b58\u8f93\u5165 token: ").append(s.cacheHitTokens == null ? "\u672a\u8fd4\u56de" : s.cacheHitTokens).append('\n');
        b.append("\u8f93\u51fa token: ").append(s.outputTokens == null ? "\u672a\u8fd4\u56de" : s.outputTokens).append('\n');
        b.append("\u7f13\u5b58\u547d\u4e2d: ").append(s.cacheHit == null ? "\u672a\u8fd4\u56de"
                : (s.cacheHit == 1 ? "\u547d\u4e2d" : "\u672a\u547d\u4e2d")).append('\n');
        b.append("\u7528\u65f6: ").append(s.elapsedMs <= 0 ? "\u672a\u8bb0\u5f55" : s.elapsedMs + " ms").append('\n');
        b.append('\n');

        // ---- messages ----
        b.append("== \u6d88\u606f\u8bb0\u5f55 ==\n");
        if (messages != null) {
            for (int i = 0; i < messages.size(); i++) {
                ChatMessage m = messages.get(i);
                b.append("--- #").append(i + 1).append(' ').append(m.role).append(' ');
                b.append(new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date(m.createdAt)));
                b.append(" ---\n");
                b.append(m.content == null ? "" : m.content).append('\n');
                if (m.content != null) {
                    b.append("[").append(m.content.length()).append(" \u5b57, \u7ea6 ")
                            .append(com.example.watchchat.data.TokenEstimator.estimate(m.content))
                            .append(" tokens]\n");
                }
                b.append('\n');
            }
        }
        return b.toString();
    }

    private static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** Enough to identify a key, not enough to use it. */
    private static String maskKey(String key) {
        if (key == null || key.isEmpty()) return "\u672a\u586b\u5199";
        String tail = key.length() <= 4 ? "" : "\u2026" + key.substring(key.length() - 4);
        return "\u5df2\u8bbe\u7f6e (\u957f\u5ea6 " + key.length() + tail + ")";
    }

    /**
     * Writes to Downloads and returns the display name on success, or null.
     *
     * Android 10+ goes through MediaStore, which needs no permission. Below that the file is
     * written to the public Downloads directory directly, which does.
     */
    static String export(Context c, String title, String content) {
        String name = fileName(title, new Date());
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = c.getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return null;
                OutputStream os = c.getContentResolver().openOutputStream(uri);
                if (os == null) return null;
                os.write(content.getBytes("UTF-8"));
                os.close();
                return name;
            }
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists() && !dir.mkdirs()) return null;
            File f = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            return name;
        } catch (Exception e) {
            return null;
        }
    }
}
