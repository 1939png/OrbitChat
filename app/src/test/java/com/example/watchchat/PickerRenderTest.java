package com.example.watchchat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w500dp-h500dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class PickerRenderTest {
    @Test public void pillPickerRendersToPng() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        int dp = (int) activity.getResources().getDisplayMetrics().density;
        int fg = Color.WHITE, accent = 0xffd8ff67, muted = 0xff8a8f85;
        // chat behind: simulate blurred green chat page (static approximation of RenderEffect blur)
        FrameLayout stage = new FrameLayout(activity);
        stage.setBackgroundColor(0xff101b16);
        TextView fake = label(activity,"已模糊的聊天页面",18,0xff5a6b52);
        fake.setGravity(Gravity.CENTER);
        stage.addView(fake,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout shell = new LinearLayout(activity); shell.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shellBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xC226292e,0xB3101216});
        shellBg.setCornerRadius(36*dp); shellBg.setStroke(2*dp,Color.argb(90,255,255,255)); shell.setBackground(shellBg);
        shell.setPadding(7*dp,7*dp,7*dp,9*dp);
        LinearLayout headRow = new LinearLayout(activity); headRow.setOrientation(LinearLayout.HORIZONTAL); headRow.setGravity(Gravity.CENTER_VERTICAL); headRow.setPadding(2*dp,0,2*dp,2*dp);
        TextView closeKey = label(activity,"x",24,Color.WHITE); closeKey.setTypeface(Typeface.DEFAULT,Typeface.BOLD); closeKey.setGravity(Gravity.CENTER);
        GradientDrawable closeBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xF23E454F,0xD9353B45});
        closeBg.setCornerRadius(20*dp); closeBg.setStroke(2*dp,Color.argb(130,255,255,255));
        GradientDrawable closeGloss = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x59ffffff,0x00ffffff});
        closeGloss.setCornerRadius(20*dp);
        closeKey.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{closeBg,closeGloss}));
        headRow.addView(closeKey,new LinearLayout.LayoutParams(58*dp,40*dp));
        headRow.addView(new View(activity),new LinearLayout.LayoutParams(0,40*dp,1));
        shell.addView(headRow);
        LinearLayout panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable innerBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xA61d2025,0x920e1013});
        innerBg.setCornerRadius(28*dp); innerBg.setStroke(dp,Color.argb(70,255,255,255)); panel.setBackground(innerBg);
        panel.setPadding(8*dp,4*dp,8*dp,10*dp);
        ScrollView list = new ScrollView(activity); list.setFillViewport(true); list.setPadding(4*dp,0,4*dp,24*dp); list.setClipToPadding(false);
        LinearLayout body = new LinearLayout(activity); body.setOrientation(LinearLayout.VERTICAL); list.addView(body);
        TextView g1 = label(activity,"我的DeepSeek",15,muted); g1.setTypeface(Typeface.DEFAULT,Typeface.BOLD); g1.setPadding(8*dp,8*dp,0,6*dp); body.addView(g1);
        body.addView(pill(activity,"deepseek-chat",true,accent,fg,muted,dp));
        body.addView(pill(activity,"deepseek-reasoner",false,accent,fg,muted,dp));
        TextView g2 = label(activity,"公司中转",15,muted); g2.setTypeface(Typeface.DEFAULT,Typeface.BOLD); g2.setPadding(8*dp,8*dp,0,6*dp); body.addView(g2);
        body.addView(pill(activity,"gpt-4o-mini",false,accent,fg,muted,dp));
        body.addView(pill(activity,"gpt-4o",false,accent,fg,muted,dp));
        panel.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        shell.addView(panel,new LinearLayout.LayoutParams(-1,0,1));
        stage.addView(shell,new FrameLayout.LayoutParams(300*dp,340*dp,Gravity.CENTER));
        int w = View.MeasureSpec.makeMeasureSpec(500*dp, View.MeasureSpec.EXACTLY);
        int h = View.MeasureSpec.makeMeasureSpec(500*dp, View.MeasureSpec.EXACTLY);
        stage.measure(w,h); stage.layout(0,0,stage.getMeasuredWidth(),stage.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(stage.getMeasuredWidth(),stage.getMeasuredHeight(),Bitmap.Config.ARGB_8888);
        stage.draw(new Canvas(bitmap));
        File dir = new File("build/render-snapshots"); if(!dir.exists()) dir.mkdirs();
        FileOutputStream out = new FileOutputStream(new File(dir,"picker.png"));
        bitmap.compress(Bitmap.CompressFormat.PNG,100,out); out.close();
    }
    private static LinearLayout pill(Activity a,String model,boolean active,int accent,int fg,int muted,int dp){
        LinearLayout card = new LinearLayout(a); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(18*dp,13*dp,18*dp,13*dp);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            active?new int[]{0xff3a432b,0xff232a1c}:new int[]{0xff2a2d33,0xff17191d});
        bg.setCornerRadius(26*dp); bg.setStroke(Math.max(2,(int)(1.5f*dp)),active?0xffd8ff67:Color.argb(60,255,255,255));
        GradientDrawable gloss = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x55ffffff,0x0affffff,0x00ffffff});
        gloss.setCornerRadius(26*dp);
        card.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{bg,gloss}));
        TextView name = label(a,model,18,active?0xffd8ff67:fg); name.setTypeface(Typeface.DEFAULT,Typeface.BOLD); name.setGravity(Gravity.CENTER);
        card.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        if(active){TextView dot = label(a,"●",16,0xffd8ff67); dot.setPadding(10*dp,0,0,0); card.addView(dot);}
        TextView del = label(a,"x",18,muted); del.setPadding(12*dp,0,0,0); card.addView(del);
        LinearLayout.LayoutParams wrap = new LinearLayout.LayoutParams(-1,-2); wrap.bottomMargin=9*dp;
        LinearLayout outer = new LinearLayout(a); outer.setOrientation(LinearLayout.VERTICAL); outer.addView(card,wrap); return outer;
    }
    private static TextView label(Activity a,String text,int sp,int color){TextView t=new TextView(a);t.setText(text);t.setTextSize(sp);t.setTextColor(color);return t;}
}
