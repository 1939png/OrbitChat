package com.example.watchchat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;

/** 24dp vector artwork within a 44dp input-row touch target. */
final class GlobeButton extends View implements Checkable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean checked;
    GlobeButton(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setMinimumWidth(dp(44));
        setMinimumHeight(dp(44));
        setBackgroundResource(android.R.drawable.list_selector_background);
        setChecked(false);
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public boolean isChecked() { return checked; }
    @Override public void toggle() { setChecked(!checked); }
    @Override public void setChecked(boolean value) {
        checked = value;
        setContentDescription(value ? "联网搜索已开启，点击关闭" : "联网搜索已关闭，点击开启");
        invalidate();
    }
    @Override public boolean performClick() { toggle(); return super.performClick(); }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checked);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x=getWidth()/2f, y=getHeight()/2f, r=dp(11);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.3f));
        paint.setColor(checked ? 0xFF89CE68 : 0xFF969B92);
        canvas.drawCircle(x,y,r,paint);
        canvas.drawOval(x-r*.43f,y-r,x+r*.43f,y+r,paint);
        canvas.drawLine(x-r,y,x+r,y,paint);
        float half=(float)Math.sqrt(r*r-r*r*.25f);
        canvas.drawLine(x-half,y-r*.5f,x+half,y-r*.5f,paint);
        canvas.drawLine(x-half,y+r*.5f,x+half,y+r*.5f,paint);
    }
}
