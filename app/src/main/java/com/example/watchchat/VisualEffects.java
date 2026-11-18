package com.example.watchchat;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import java.io.File;
import android.graphics.*;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.animation.PathInterpolator;

import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

final class VisualEffects {
    /**
     * Typeface for the whole app. Sans-serif-medium reads far cleaner than the default
     * Regular at watch sizes; a little negative tracking keeps the CJK/latin mix tight.
     */
    private static Typeface TYPE;
    /**
     * UI face: Inter, shipped as a font resource. It has far better stroke contrast than the
     * platform default at watch sizes, and a real medium weight rather than a synthetic bold.
     */
    static Typeface type(Context c){
        return FontStore.typeface(c);
    }

    /**
     * Applies the chosen typeface to every TextView already in a view tree.
     *
     * Setting the typeface at creation time only covers the views we build by hand; anything
     * created by a child view, a dialog or a third-party widget would keep the platform face.
     * Walking the finished tree is the only way to make the choice truly global.
     */
    static void applyFontToTree(View root, Context c) {
        if (root == null) return;
        Typeface face = FontStore.typeface(c);
        boolean pixel = FontStore.isPixel(c);
        applyFontRecursive(root, face, pixel);
    }

    private static void applyFontRecursive(View view, Typeface face, boolean pixel) {
        if (view instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) view;
            tv.setTypeface(face);
            if (pixel) {
                // Only the pixel faces want hard, unsmoothed edges.
                tv.setLetterSpacing(0f);
                tv.getPaint().setSubpixelText(false);
                tv.getPaint().setAntiAlias(false);
            } else {
                // The UI face gets full subpixel coverage back; leaving it off is what made
                // ordinary text look like it had an outline.
                tv.getPaint().setSubpixelText(true);
                tv.getPaint().setAntiAlias(true);
                tv.getPaint().setLinearText(false);
            }
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyFontRecursive(group.getChildAt(i), face, pixel);
            }
        }
    }
    static void styleText(android.widget.TextView v,float tracking){
        android.content.Context c=v.getContext();
        v.setTypeface(type(c));
        // Pixel faces must not be smoothed, or the blocks turn to mush.
        if(FontStore.isPixel(c)) v.setPaintFlags(v.getPaintFlags()|android.graphics.Paint.SUBPIXEL_TEXT_FLAG);
        v.setLetterSpacing(FontStore.isPixel(c)?0f:tracking);
    }


    // Curves from review-animations STANDARDS.md (easing.dev values).
    /** Strong ease-out for UI entering/appearing. */
    static final PathInterpolator EASE_OUT = new PathInterpolator(0.23f, 1f, 0.32f, 1f);
    /** Strong ease-in-out for on-screen movement. */
    static final PathInterpolator EASE_IN_OUT = new PathInterpolator(0.77f, 0f, 0.175f, 1f);
    /** iOS-like drawer curve. */
    static final PathInterpolator EASE_DRAWER = new PathInterpolator(0.32f, 0.72f, 0f, 1f);

    /** Movement is dropped, opacity kept, when the OS asks for reduced motion. */
    static boolean reducedMotion(View v){
        return !android.animation.ValueAnimator.areAnimatorsEnabled();
    }

    /** Perfectly circular key: both composer buttons share this so they cannot drift apart. */
    /**
     * Flat 2D icon key: solid fill, hairline ring, no gloss, no elevation, no 3D.
     * Press feedback is a small scale-down only, which is all a flat control needs.
     */
    static void roundKey(View view,boolean filled){
        float density=view.getResources().getDisplayMetrics().density;
        // Neutral greys only: the composer is chrome, not a call to action.
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(filled?0xff3A3A3A:Color.TRANSPARENT);
        shape.setStroke(Math.max(1,Math.round(density)),
                filled?0xff3A3A3A:Color.argb(70,255,255,255));
        android.graphics.drawable.GradientDrawable mask=new android.graphics.drawable.GradientDrawable();
        mask.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        view.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x26ffffff),shape,mask));
        view.setElevation(0f);
        android.animation.StateListAnimator st=new android.animation.StateListAnimator();
        android.animation.AnimatorSet down=new android.animation.AnimatorSet();
        down.playTogether(android.animation.ObjectAnimator.ofFloat(view,"scaleX",.92f),
                          android.animation.ObjectAnimator.ofFloat(view,"scaleY",.92f));
        down.setDuration(110);
        st.addState(new int[]{android.R.attr.state_pressed},down);
        android.animation.AnimatorSet up=new android.animation.AnimatorSet();
        up.playTogether(android.animation.ObjectAnimator.ofFloat(view,"scaleX",1f),
                        android.animation.ObjectAnimator.ofFloat(view,"scaleY",1f));
        up.setDuration(160);up.setInterpolator(EASE_OUT);
        st.addState(new int[]{},up);
        view.setStateListAnimator(st);
    }

    /** Flat panel surface: solid colour plus a hairline ring. No gloss, no shadow. */
    static void raisedSurface(View view,boolean button){
        float density=view.getResources().getDisplayMetrics().density;
        int accent=AppearancePreferences.accent(view.getContext());
        android.graphics.drawable.GradientDrawable surface=new android.graphics.drawable.GradientDrawable();
        surface.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        boolean light = AppearancePreferences.lightMode(view.getContext());
        // Light mode inverts the relationship: a white raised surface on a warm grey ground,
        // separated by a hairline border rather than by a lighter fill.
        if (light) {
            // A filled key stays dark in light mode: the primary action must read as the
            // heaviest object on the page, and a white key on a white page reads as nothing.
            surface.setColor(button ? 0xff2b2d31 : 0xffffffff);
            surface.setCornerRadius(button?26*density:24*density);
            surface.setStroke(Math.max(1,Math.round(density)),
                    button ? 0xff2b2d31 : 0xffd6d6d2);
        } else {
            surface.setColor(button?0xff3A3A3A:0xff1C1C1C);
            surface.setCornerRadius(button?26*density:24*density);
            surface.setStroke(Math.max(1,Math.round(density)),
                    button?0xff3A3A3A:Color.argb(48,255,255,255));
        }
        view.setBackground(surface);
        view.setElevation(0f);
    }

    static void applyAppearance(Activity activity, View root) {
        // Pure black is the house background. A user-authored wallpaper still wins when set.
        java.io.File wallpaper = AppearancePreferences.wallpaperFile(activity);
        if (wallpaper.isFile()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(wallpaper.getAbsolutePath());
                if (bmp != null) { root.setBackground(new BitmapDrawable(activity.getResources(), bmp)); return; }
            } catch (Throwable ignored) { }
        }
        // Day/night drives the page background. A user wallpaper still wins above this.
        root.setBackgroundColor(AppearancePreferences.background(activity));
        // Never install a 0-radius blur. It does not mean "no blur" — it still routes the whole
        // subtree through an offscreen composite, and the resampling softens every glyph edge.
        // That was the faint halo around text. Only a real radius earns the cost.
        if (Build.VERSION.SDK_INT >= 31) root.setRenderEffect(null);
    }
    static void install(Activity activity,View content){
        FrameLayout frame=new FrameLayout(activity);
        frame.setBackgroundColor(Color.BLACK);
        File wallpaper = AppearancePreferences.wallpaperFile(activity);
        if (wallpaper.isFile()) {
            try {
                android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(wallpaper.getAbsolutePath(), o);
                int sample = 1;
                while ((o.outWidth / sample) > 640 || (o.outHeight / sample) > 640) sample *= 2;
                android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
                o2.inSampleSize = sample;
                Bitmap bitmap = BitmapFactory.decodeFile(wallpaper.getAbsolutePath(), o2);
                if (bitmap != null) { BitmapDrawable drawable = new BitmapDrawable(activity.getResources(), bitmap); drawable.setAlpha(90); frame.setBackground(drawable); }
            } catch (Throwable t) { }
        }
        View glow=new View(activity);
        android.graphics.drawable.GradientDrawable gradient=new android.graphics.drawable.GradientDrawable();
        gradient.setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT);
        gradient.setColors(new int[]{0x554c7441,0x18344927,0x00050505});
        gradient.setGradientCenter(.18f,.12f);
        gradient.setGradientRadius(280*activity.getResources().getDisplayMetrics().density);
        glow.setBackground(gradient);
        if(Build.VERSION.SDK_INT>=31 && AppearancePreferences.blurEnabled(activity)){ int radius = AppearancePreferences.blurLevel(activity) * 8; if(radius > 0) glow.setRenderEffect(RenderEffect.createBlurEffect(radius,radius,Shader.TileMode.CLAMP)); }
        frame.addView(glow,new FrameLayout.LayoutParams(-1,-1));
        content.setBackgroundColor(Color.TRANSPARENT);
        frame.addView(content,new FrameLayout.LayoutParams(-1,-1));
        activity.setContentView(frame);
        enter(content);
    }
    /** Directional page transition: slide+fade. dx>0 enters from right, dx<0 from left. */
    static void pageTransition(View view,float dx){
        if(!ValueAnimator.areAnimatorsEnabled())return;
        view.setAlpha(0f);view.setTranslationX(dx);
        view.animate().alpha(1f).translationX(0).setDuration(420)
            .setInterpolator(new PathInterpolator(.22f,1f,.36f,1f)).start();
    }
    static void enter(View view){
        if(!ValueAnimator.areAnimatorsEnabled())return;
        view.setAlpha(0f);view.setTranslationY(12*view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0).setDuration(320)
            .setInterpolator(new PathInterpolator(.22f,1f,.36f,1f)).start();
    }
    /** Complex line-art loading view: rotating orbit + breathing ring + comet tail. Pure Canvas, GPU-friendly. */
    static final class LoadingRing extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds=new RectF();
        private ValueAnimator animator;
        private float phase;
        LoadingRing(Context context){super(context);setContentDescription("正在加载");}
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();start();}
        private void start(){if(animator!=null||!isShown()||!ValueAnimator.areAnimatorsEnabled())return;
            animator=ValueAnimator.ofFloat(0,1);animator.setDuration(1600);animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}
        private void stop(){if(animator!=null){animator.cancel();animator=null;}}
        @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)start();else stop();}
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
            float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)*.62f;
            paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(cx,cy,r*1.5f,new int[]{0x554eaa46,0x00225320},null,Shader.TileMode.CLAMP));
            canvas.drawCircle(cx,cy,r*1.5f,paint);paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);paint.setStrokeCap(Paint.Cap.ROUND);
            bounds.set(cx-r,cy-r,cx+r,cy+r);paint.setColor(0x334cc45c);canvas.drawOval(bounds,paint);
            paint.setColor(AppearancePreferences.accent(this.getContext()));canvas.drawArc(bounds,phase*360-90,80+(float)Math.sin(phase*Math.PI*2)*40,false,paint);
        }
    }
    /** Complex line-art loading: orbits + comet + breathing core, sized for any view. */
    static final class LoadingSlider extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect=new RectF();
        private ValueAnimator animator;
        private float phase;
        LoadingSlider(Context context){super(context);setContentDescription("正在加载，请稍候");}
        private void start(){if(animator!=null||!isShown()||!ValueAnimator.areAnimatorsEnabled())return;
            animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(2200);
            animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}
        private void stop(){if(animator!=null){animator.cancel();animator=null;}}
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();start();}
        @Override protected void onWindowVisibilityChanged(int v){super.onWindowVisibilityChanged(v);if(v==VISIBLE)start();else stop();}
        @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
            float cx=getWidth()/2f,cy=getHeight()/2f;
            float base=Math.min(cx,cy)*.42f;
            int accent=AppearancePreferences.accent(this.getContext());
            float dpi=getResources().getDisplayMetrics().density;
            // layer 1: two counter-rotating elliptical orbits
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
            for(int layer=0;layer<2;layer++){
                float lr=base*(layer==0?1f:.68f);
                float rot=phase*360*(layer==0?1f:-1.4f)+(layer==0?0f:120f);
                paint.setStrokeWidth((layer==0?2f:1.5f)*dpi);
                paint.setColor(layer==0?0x334cc45c:0x2a4c7441);
                android.graphics.Matrix m=new android.graphics.Matrix();
                m.postRotate(rot,cx,cy);m.postScale(1f,layer==0?.38f:.55f,cx,cy);
                canvas.save();canvas.concat(m);
                canvas.drawCircle(cx,cy,lr,paint);
                canvas.restore();
            }
            // layer 2: comet tail arc
            paint.setStrokeWidth(3.5f*dpi);
            paint.setColor(accent);
            rect.set(cx-base,cy-base*.38f,cx+base,cy+base*.38f);
            canvas.drawArc(rect,phase*360-140,110,false,paint);
            // comet head dot with glow
            float ang=(float)Math.toRadians(phase*360-140+110);
            float hx=cx+base*(float)Math.cos(ang);
            float hy=cy+base*.38f*(float)Math.sin(ang);
            paint.setStyle(Paint.Style.FILL);
            int glowR=(int)(9*dpi);
            int accentFaded=(accent&0x00FFFFFF);
            paint.setShader(new RadialGradient(hx,hy,glowR,new int[]{accent,accentFaded},null,Shader.TileMode.CLAMP));
            canvas.drawCircle(hx,hy,glowR,paint);
            paint.setShader(null);
            // layer 3: breathing core circle
            float breathe=base*.28f*(1f+.14f*(float)Math.sin(phase*Math.PI*4));
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f*dpi);
            paint.setColor(accent);
            canvas.drawCircle(cx,cy,breathe,paint);
            // layer 4: center dot
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx,cy,3*dpi,paint);
        }
    }
}
