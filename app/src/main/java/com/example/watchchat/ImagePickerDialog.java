package com.example.watchchat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-app photo picker: scans MediaStore for the newest images and shows them as a grid of
 * thumbnails. Replaces the system documents UI so the flow stays inside the app's own look.
 */
final class ImagePickerDialog extends Dialog {
    interface Callback { void onPicked(Uri uri, String name); }

    private static final int COLUMNS = 3;
    private final Callback callback;
    private final Typeface face;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private LinearLayout grid;
    private TextView status;

    ImagePickerDialog(Activity a, Callback cb) {
        super(a);
        callback = cb;
        face = VisualEffects.type(a);
        setOwnerActivity(a);
    }

    @Override protected void onCreate(android.os.Bundle state) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Context c = getContext();
        int dp = (int) (c.getResources().getDisplayMetrics().density + .5f);

        LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(12 * dp, 12 * dp, 12 * dp, 12 * dp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xff101010);
        bg.setCornerRadius(20 * dp);
        bg.setStroke(dp, 0x33FFFFFF);
        panel.setBackground(bg);

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(c);
        title.setText("选择照片");
        title.setTextColor(AppearancePreferences.ink(getContext()));
        title.setTextSize(16);
        title.setTypeface(face);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = new TextView(c);
        close.setText("×");
        close.setTextColor(AppearancePreferences.inkMuted(getContext()));
        close.setTextSize(20);
        close.setTypeface(face);
        close.setGravity(Gravity.CENTER);
        head.addView(close, new LinearLayout.LayoutParams(36 * dp, 36 * dp));
        panel.addView(head, new LinearLayout.LayoutParams(-1, -2));
        close.setOnClickListener(v -> dismiss());

        status = new TextView(c);
        status.setText("正在扫描相册…");
        status.setTextColor(AppearancePreferences.inkMuted(getContext()));
        status.setTextSize(13);
        status.setTypeface(face);
        status.setPadding(2 * dp, 8 * dp, 2 * dp, 6 * dp);
        panel.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(c);
        grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(grid);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(panel);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(300 * dp, 400 * dp);
            w.setDimAmount(.6f);
        }
        clearOnDismiss();
        if (hasImagePermission()) {
            loadImages();
        } else {
            status.setText("需要相册权限才能显示照片");
            requestPermissionThenLoad();
        }
    }

    private void clearOnDismiss() {
        setOnDismissListener(d -> io.shutdownNow());
    }

    /**
     * Which permission is live depends on the runtime OS, not on targetSdk: on Android 13+
     * the platform only honours READ_MEDIA_IMAGES (READ_EXTERNAL_STORAGE is deprecated and
     * silently ignored), and below that it is the other way round.
     */
    static String requiredPermission() {
        return android.os.Build.VERSION.SDK_INT >= 33
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasImagePermission() {
        return getContext().checkSelfPermission(requiredPermission())
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /** Asks the host activity for permission, then re-scans. */
    private void requestPermissionThenLoad() {
        if (getOwnerActivity() instanceof android.app.Activity) {
            android.app.Activity host = getOwnerActivity();
            host.requestPermissions(new String[]{requiredPermission()}, 9001);
        }
    }

    /** Called by the host once the permission result arrives. */
    void onPermissionResult() {
        if (hasImagePermission()) loadImages();
        else status.setText("没有读取相册的权限，可在系统设置里授予");
    }

    /**
     * Scans the public picture directories directly instead of going through MediaStore.
     * MediaStore applies per-app visibility rules that depend on both targetSdk and the
     * runtime OS; walking the folders is deterministic and needs no provider cooperation.
     */
    private void loadImages() {
        io.execute(() -> {
            final List<Uri> found = new ArrayList<>();
            final List<String> names = new ArrayList<>();
            java.io.File[] roots = {
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_PICTURES),
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DCIM),
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS),
            };
            for (java.io.File root : roots) collect(root, found, names, 0, 60);
            ui.post(() -> render(found, names));
        });
    }

    /** Depth-limited walk so a deep DCIM tree cannot stall the scan. */
    private void collect(java.io.File dir, List<Uri> out, List<String> names, int depth, int limit) {
        if (dir == null || !dir.isDirectory() || depth > 3 || out.size() >= limit) return;
        java.io.File[] children = dir.listFiles();
        if (children == null) return;
        java.util.Arrays.sort(children, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (java.io.File f : children) {
            if (out.size() >= limit) return;
            if (f.isDirectory()) {
                collect(f, out, names, depth + 1, limit);
            } else if (isImage(f.getName())) {
                out.add(Uri.fromFile(f));
                names.add(f.getName());
            }
        }
    }

    private static boolean isImage(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp");
    }

    private void render(List<Uri> uris, List<String> labels) {
        if (isShowing() == false && getWindow() == null) return;
        grid.removeAllViews();
        if (uris.isEmpty()) {
            status.setText("相册里没有找到图片");
            return;
        }
        status.setText("最近 " + uris.size() + " 张");
        Context c = getContext();
        int dp = (int) (c.getResources().getDisplayMetrics().density + .5f);
        LinearLayout row = null;
        for (int i = 0; i < uris.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            final Uri uri = uris.get(i);
            final String name = labels.get(i);
            FrameLayout cell = new FrameLayout(c);
            ImageView thumb = new ImageView(c);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundColor(0xff1E1E1E);
            try { thumb.setImageURI(uri); } catch (Throwable ignored) { }
            cell.addView(thumb, new FrameLayout.LayoutParams(-1, -1));
            GradientDrawable cellBg = new GradientDrawable();
            cellBg.setColor(Color.TRANSPARENT);
            cellBg.setCornerRadius(8 * dp);
            cellBg.setStroke(dp, 0x22FFFFFF);
            cell.setForeground(null);
            cell.setBackground(cellBg);
            cell.setOnClickListener(v -> {
                dismiss();
                callback.onPicked(uri, name);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 88 * dp, 1);
            lp.setMargins(2 * dp, 2 * dp, 2 * dp, 2 * dp);
            row.addView(cell, lp);
        }
    }
}
