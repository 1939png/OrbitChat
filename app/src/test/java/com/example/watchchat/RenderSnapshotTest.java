package com.example.watchchat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Renders real views via Robolectric native graphics and writes PNGs to app/build/render-snapshots/. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w240dp-h240dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class RenderSnapshotTest {

    @Test public void composerAndInfoCardRenderToPng() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();

        LinearLayout composer = new LinearLayout(activity);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setPadding(dp(activity, 12), dp(activity, 9), dp(activity, 12), dp(activity, 9));
        VisualEffects.raisedSurface(composer, false);

        TextView attach = new TextView(activity);
        attach.setText("＋");
        attach.setTextSize(24);
        attach.setGravity(android.view.Gravity.CENTER);
        VisualEffects.raisedSurface(attach, false);
        LinearLayout.LayoutParams attachParams = new LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 52));
        attachParams.leftMargin = dp(activity, 4);
        attachParams.rightMargin = dp(activity, 4);
        composer.addView(attach, attachParams);

        TextView field = new TextView(activity);
        field.setText("输入消息…");
        field.setTextSize(16);
        field.setTextColor(0xffb9bdb3);
        composer.addView(field, new LinearLayout.LayoutParams(0, -2, 1));

        SendButton send = new SendButton(activity);
        send.setText("↑");
        VisualEffects.raisedSurface(send, true);
        composer.addView(send, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)));

        File outDir = new File(activity.getApplicationInfo().dataDir, "../../../build/render-snapshots");
        outDir = new File("build/render-snapshots");
        if (!outDir.exists()) outDir.mkdirs();
        write(activity, composer, new File(outDir, "composer.png"));

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        android.graphics.drawable.GradientDrawable surface = new android.graphics.drawable.GradientDrawable();
        surface.setColor(0xff181a1d);
        surface.setCornerRadius(dp(activity, 14));
        card.setBackground(surface);
        card.addView(label(activity, "最近请求 · 输入", 14, 0xffb9bdb3));
        card.addView(label(activity, "1280 tokens", 20, 0xffd8ff67));
        card.addView(label(activity, "上下文占用按输入 token 计算，不含输出。", 12, 0xff8a8f85));
        write(activity, card, new File(outDir, "info-card.png"));

        long png1 = new File(outDir, "composer.png").length();
        long png2 = new File(outDir, "info-card.png").length();
        if (png1 < 500 || png2 < 500) throw new AssertionError("snapshot too small: " + png1 + "," + png2);
    }

    private static TextView label(Activity activity, String text, int sp, int color) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private static void write(Activity activity, View view, File target) throws Exception {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(dp(activity, 216), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        view.draw(canvas);
        FileOutputStream out = new FileOutputStream(target);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
        bitmap.recycle();
    }

    private static int dp(Activity activity, float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + .5f);
    }
}
