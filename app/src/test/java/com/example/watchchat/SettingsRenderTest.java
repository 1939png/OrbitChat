package com.example.watchchat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
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

/** Renders a static replica of the restyled settings layout to PNG for visual review. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class SettingsRenderTest {

    @Test public void settingsRestyleRendersToPng() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        int BG = Color.rgb(18,19,16), PANEL = Color.rgb(32,34,29), FG = Color.WHITE, ACCENT = Color.rgb(216,255,103), MUTED = Color.rgb(205,208,198);
        int dp = (int) activity.getResources().getDisplayMetrics().density;

        LinearLayout page = new LinearLayout(activity); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(BG);
        LinearLayout header = new LinearLayout(activity); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(20*dp,14*dp,20*dp,10*dp);
        TextView title = label(activity,"设置",26,FG); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView subtitle = label(activity,"配置预设、接口、外观与文件",14,MUTED); subtitle.setPadding(0,2*dp,0,0);
        header.addView(title); header.addView(subtitle); page.addView(header);
        View divider = new View(activity); divider.setBackgroundColor(Color.argb(60,255,255,255)); page.addView(divider,new LinearLayout.LayoutParams(-1,dp));

        ScrollView scroll = new ScrollView(activity); LinearLayout form = new LinearLayout(activity); form.setOrientation(LinearLayout.VERTICAL); scroll.addView(form);
        form.addView(card(activity,"预设管理","五组独立接口配置",PANEL,FG,MUTED,ACCENT,dp,true));
        form.addView(card(activity,"模型与接口","服务地址、密钥、模型名",PANEL,FG,MUTED,ACCENT,dp,true));
        form.addView(card(activity,"个性化","主题、壁纸、动态模糊",PANEL,FG,MUTED,ACCENT,dp,false));
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout actions = new LinearLayout(activity); actions.setOrientation(LinearLayout.HORIZONTAL); actions.setPadding(16*dp,8*dp,16*dp,12*dp);
        TextView back = label(activity,"返回聊天",16,FG); back.setGravity(Gravity.CENTER);
        GradientDrawable backBg = new GradientDrawable(); backBg.setColor(Color.argb(40,255,255,255)); backBg.setCornerRadius(14*dp); back.setBackground(backBg);
        TextView save = label(activity,"保存设置",16,Color.rgb(20,26,16)); save.setGravity(Gravity.CENTER); save.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable saveBg = new GradientDrawable(); saveBg.setColor(ACCENT); saveBg.setCornerRadius(14*dp); save.setBackground(saveBg);
        actions.addView(back,new LinearLayout.LayoutParams(0,52*dp,1)); actions.addView(save,new LinearLayout.LayoutParams(0,52*dp,1));
        page.addView(actions);

        File dir = new File("build/render-snapshots"); if(!dir.exists()) dir.mkdirs();
        int w = View.MeasureSpec.makeMeasureSpec(360*dp, View.MeasureSpec.EXACTLY);
        int h = View.MeasureSpec.makeMeasureSpec(640*dp, View.MeasureSpec.EXACTLY);
        page.measure(w,h); page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(page.getMeasuredWidth(),page.getMeasuredHeight(),Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap); page.draw(canvas);
        FileOutputStream out = new FileOutputStream(new File(dir,"settings.png"));
        bitmap.compress(Bitmap.CompressFormat.PNG,100,out); out.close();
        if(new File(dir,"settings.png").length()<2000) throw new AssertionError("settings snapshot too small");
    }

    private static LinearLayout card(Activity a,String title,String sub,int panel,int fg,int muted,int accent,int dp,boolean expanded){
        LinearLayout card = new LinearLayout(a); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(16*dp,6*dp,16*dp,10*dp);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(panel); bg.setCornerRadius(18*dp); bg.setStroke(dp,Color.argb(70,255,255,255)); card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=12*dp; lp.leftMargin=12*dp; lp.rightMargin=12*dp; lp.topMargin=4*dp;
        LinearLayout row = new LinearLayout(a); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(4*dp,10*dp,4*dp,10*dp);
        LinearLayout head = new LinearLayout(a); head.setOrientation(LinearLayout.VERTICAL);
        head.addView(label(a,title,18,fg));
        head.addView(label(a,sub,12,muted));
        row.addView(head,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(label(a,expanded?"▾":"▸",16,accent));
        card.addView(row); View line = new View(a); line.setBackgroundColor(Color.argb(40,255,255,255)); card.addView(line,new LinearLayout.LayoutParams(-1,dp));
        LinearLayout wrap = new LinearLayout(a); wrap.setOrientation(LinearLayout.VERTICAL); wrap.addView(card,lp); return wrap;
    }
    private static TextView label(Activity a,String text,int sp,int color){TextView t=new TextView(a);t.setText(text);t.setTextSize(sp);t.setTextColor(color);return t;}
}
