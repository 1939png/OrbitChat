package com.example.watchchat;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.watchchat.data.ChatConfigStore;
import com.example.watchchat.data.ProviderStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings rendered as an embedded page (not an Activity), so it can live in the
 * same horizontal pager as chat and history and be revealed by camera translation.
 */
public final class SettingsPage extends LinearLayout {
    public interface Host {
        void onCloseSettings();
        void onPickWallpaper();
        void onRequestTree(String treeUri);
        /** Called after the day/night mode flips, so sibling pages can repaint too. */
        void onThemeChanged();
    }
    /** Resolved per build so a day/night change repaints on the next page construction. */
    /** Hairline / fill tint that works on both grounds: white on dark, black on light. */
    /** A button fill that reads on both grounds without a border doing the work. */
    private int buttonFill() {
        return AppearancePreferences.lightMode(activity) ? 0xffe9e9e5 : Color.argb(40, 255, 255, 255);
    }

    private int tint(int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return AppearancePreferences.lightMode(activity)
                ? Color.argb(a, 0, 0, 0) : Color.argb(a, 255, 255, 255);
    }

    private int bg() { return AppearancePreferences.background(activity); }
    private int fg() { return AppearancePreferences.ink(activity); }
    private int panelColor() { return AppearancePreferences.surface(activity); }
    private int mutedInk() { return AppearancePreferences.inkMuted(activity); }

    private static final int BG=Color.BLACK, PANEL=Color.rgb(16,16,16), FG=Color.WHITE, ACCENT=Color.rgb(216,255,103), MUTED=Color.rgb(150,150,150);
    private final Activity activity;
    private final Host host;
    private ChatConfigStore store; private SharedPreferences ui;
    private LinearLayout providerList, pickedBox;
    private EditText editName, editUrl, editKey;
    private ProtocolSwitch editProtocol;
    private String editingId;
    private List<String> pickedModels = new ArrayList<>();

    public SettingsPage(Activity activity, Host host) {
        super(activity);
        this.activity = activity;
        this.host = host;
        setOrientation(VERTICAL);
        setBackgroundColor(bg());
        store = new ChatConfigStore(activity);
        ui = activity.getSharedPreferences("chat_settings_ui", Activity.MODE_PRIVATE);
        try { seed(); } catch (Throwable ignored) { }
        build();
    }

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private LinearLayout row(){LinearLayout x=new LinearLayout(activity);x.setOrientation(HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
    private LinearLayout column(){LinearLayout x=new LinearLayout(activity);x.setOrientation(VERTICAL);return x;}
    private TextView label(String value,int size){
        TextView t=new TextView(activity);t.setText(value);t.setTextSize(Math.max(size,18));t.setTextColor(fg());
        t.setGravity(Gravity.CENTER_VERTICAL);
        VisualEffects.styleText(t,0.01f);
        return t;
    }
    private TextView label(String value,int size,int color){TextView t=label(value,size);t.setTextColor(color);return t;}
    private TextView tiny(String t){TextView v=label(t,12);v.setTextColor(MUTED);v.setPadding(dp(4),dp(8),0,dp(3));return v;}
    private TextView actionBtn(String t,int bg,int fg){TextView v=label(t,15);v.setGravity(Gravity.CENTER);v.setTextColor(fg);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable d=new GradientDrawable();d.setColor(bg);d.setCornerRadius(dp(12));v.setBackground(d);return v;}

    private final java.util.List<View> tracked = new java.util.ArrayList<>();
    /** Whether the system-prompt tool is on. Kept separate from the text itself:
     *  an empty prompt and a disabled tool are different states. */
    private boolean promptEnabled;
    private SizeCapsule capsuleRef;

    /** Controls that own a horizontal drag; the pager must not steal it. */
    public java.util.List<View> horizontalControls() { return tracked; }

    /** Rebuilds the whole page body; call after appearance reset. */
    public void rebuild(){removeAllViews();build();}

    private void seed(){
        com.example.watchchat.data.ChatConfig c=store.load();
        SharedPreferences.Editor e=ui.edit();
        e.apply();
    }

    private void build(){
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(14),dp(10),dp(14),dp(10));
        // Back key: soft frosted circle with a drawn chevron, not a text glyph.
        BackKey backBtn=new BackKey(activity,dp(46),tint(30),fg());
        backBtn.setContentDescription("返回聊天");
        header.addView(backBtn,new LinearLayout.LayoutParams(dp(46),dp(46)));
        TextView title=label("设置",24,fg());title.setTypeface(VisualEffects.type(activity));title.setGravity(Gravity.CENTER);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(46),1));
        header.addView(new View(activity),new LinearLayout.LayoutParams(dp(46),dp(46)));
        addView(header);
        pressFeedback(backBtn);
        backBtn.setOnClickListener(v->host.onCloseSettings());
        View divider=new View(activity);divider.setBackgroundColor(Color.argb(60,255,255,255));
        addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));

        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(false);scroll.setBackgroundColor(bg());scroll.setClipToPadding(true);
        LinearLayout form=column();form.setBackgroundColor(bg());form.setPadding(0,dp(4),0,dp(12));
        // ---- system prompt: its own collapsible card, above the providers ----
        promptEnabled = !com.example.watchchat.data.PromptStore.systemPrompt(activity).isEmpty();
        // ---- tools: one card, each tool a row with a toggle and a detail chevron ----
        LinearLayout toolsCard = section(form, "\u5de5\u5177", "\u5f00\u5173\u6a21\u578b\u53ef\u4ee5\u4f7f\u7528\u7684\u80fd\u529b", "tools");


        // System prompt lives in the tools list now: it is a capability the model may use,
        // so the editor belongs behind its chevron instead of sprawling over the page.
        toolsCard.addView(toolRow(
                "\u7cfb\u7edf\u63d0\u793a\u8bcd",
                () -> promptEnabled,
                on -> {
                    promptEnabled = on;
                    if (!on) com.example.watchchat.data.PromptStore.setSystemPrompt(activity, "");
                },
                () -> showPromptDetail()
        ), new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout profiles=section(form,"模型供应商","添加接口、获取模型、点选使用");
        LinearLayout appearance=section(form,"个性化","appearance");
        // ---- day / night: same switch geometry as the protocol selector ----
        appearance.addView(tiny("外观模式"));
        final ThemeSwitch themeSwitch = new ThemeSwitch(activity);
        tracked.add(themeSwitch);   // the pager must not steal its horizontal drag
        themeSwitch.setLight(AppearancePreferences.lightMode(activity), false);
        appearance.addView(themeSwitch, new LinearLayout.LayoutParams(-1, dp(46)));
        final TextView modeValue = label("", 13, MUTED);
        modeValue.setGravity(Gravity.CENTER);
        modeValue.setPadding(0, dp(4), 0, dp(8));
        appearance.addView(modeValue, new LinearLayout.LayoutParams(-1, -2));
        final Runnable syncMode = () -> modeValue.setText(
                themeSwitch.isLight() ? "亮色模式" : "深色模式");
        syncMode.run();
        themeSwitch.setOnChange(light -> {
            AppearancePreferences.setLightMode(activity, light);
            // A colour constant cannot be patched in place: every card, field and label in
            // this page was built with the old mode baked in. Rebuild the page so the whole
            // surface flips at once instead of half-updating.
            rebuild();
            // Colours are baked at construction time, so every page is rebuilt rather
            // than patched. That is what makes the switch instant instead of a restart.
            if (host != null) host.onThemeChanged();
        });

        // ---- font size: ten-step charged capsule ----
        appearance.addView(tiny("字号（重启生效）"));
        sizeCapsule = new SizeCapsule(activity);
        tracked.add(sizeCapsule);   // the pager must not steal its horizontal drag
        appearance.addView(sizeCapsule, new LinearLayout.LayoutParams(-1, dp(58)));
        SizeCapsule capsule = sizeCapsule;
        TextView sizeValue = label("", 15, MUTED);
        sizeValue.setGravity(Gravity.CENTER);
        sizeValue.setPadding(0, dp(4), 0, dp(6));
        appearance.addView(sizeValue, new LinearLayout.LayoutParams(-1, -2));
        final TextView valueView = sizeValue;
        Runnable sync = () -> {
            int lv = capsule.level();
            valueView.setText("第 " + (lv + 1) + " / " + FontStore.SIZE_LEVELS + " 档 · "
                    + Math.round(11f + lv * 1.65f) + "sp");
        };
        sync.run();
        capsule.setOnLevelChange(l -> sync.run());

        TextView restartNote = label("字体与字号在下次启动时生效", 13, MUTED);
        restartNote.setGravity(Gravity.CENTER);
        restartNote.setPadding(0, dp(6), 0, dp(2));
        appearance.addView(restartNote, new LinearLayout.LayoutParams(-1, -2));

        ProviderStore.migrateLegacy(activity);
        providerList=new LinearLayout(activity);providerList.setOrientation(VERTICAL);profiles.addView(providerList);
        renderProviderCards();


        LinearLayout editor=column();
        // The label IS the hint: showing both a static label and a hint repeated every
        // field name twice on a black page. Hints are dimmed so they read as placeholders.
        editName=prefixField("显示名称", InputType.TYPE_CLASS_TEXT);
        editUrl=prefixField("API 地址", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        editKey=prefixField("API Key", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editProtocol=new ProtocolSwitch(activity);
        addFieldBox(editor,editName);
        addFieldBox(editor,editProtocol);
        addFieldBox(editor,editUrl);
        addFieldBox(editor,editKey);
        LinearLayout btnRow=new LinearLayout(activity);btnRow.setOrientation(HORIZONTAL);
        TextView fetchBtn=actionBtn("获取可用模型",buttonFill(),fg());
        btnRow.addView(fetchBtn,new LinearLayout.LayoutParams(0,dp(46),1));
        TextView saveBtn=actionBtn("保存供应商",ACCENT,Color.rgb(20,26,16));
        LinearLayout.LayoutParams sbp=new LinearLayout.LayoutParams(0,dp(46),1);sbp.leftMargin=dp(8);btnRow.addView(saveBtn,sbp);
        editor.addView(btnRow,new LinearLayout.LayoutParams(-1,-2));
        pickedModels=new ArrayList<>();pickedBox=column();pickedBox.setPadding(dp(4),dp(4),dp(4),0);editor.addView(pickedBox);
        profiles.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        pressFeedback(fetchBtn);
        fetchBtn.setOnClickListener(v->fetchModels());
        pressFeedback(saveBtn);
        saveBtn.setOnClickListener(v->saveProvider());

        TextView footer=new TextView(activity);
        footer.setText("HIM开发 · 免费开源");
        footer.setTextSize(11);
        footer.setTextColor(Color.argb(110,205,208,198));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,dp(18),0,dp(26));
        form.addView(footer,new LinearLayout.LayoutParams(-1,-2));

        scroll.addView(form);
        addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        VisualEffects.applyFontToTree(this,activity);
    }

    /** A single-line field whose hint is its only label. */
    private EditText prefixField(String hint, int inputType) {
        EditText e = new EditText(activity);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(fg());
        // Hint contrast flips with the ground: a dim grey is legible on black, invisible on white.
        e.setHintTextColor(AppearancePreferences.lightMode(activity)
                ? Color.argb(140, 20, 22, 26) : Color.argb(90, 205, 208, 198));
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        return e;
    }

    /** Wraps a control in a visible field box so it reads as an input on a black page. */
    private void addFieldBox(LinearLayout host, View control) {
        GradientDrawable box = new GradientDrawable();
        boolean lightBox = AppearancePreferences.lightMode(activity);
        box.setColor(lightBox ? 0xffffffff : 0xff141414);
        box.setCornerRadius(dp(10));
        box.setStroke(dp(1), lightBox ? 0xffd2d2ce : Color.argb(60, 255, 255, 255));
        control.setBackground(box);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.topMargin = dp(8);
        host.addView(control, lp);
    }

    private LinearLayout section(LinearLayout form,String title,String id){return section(form,title,null,id);}
    private LinearLayout section(LinearLayout form,String title,String subtitle,String id){
        LinearLayout card=column();card.setPadding(dp(16),dp(6),dp(16),dp(10));
        GradientDrawable background=new GradientDrawable();
        background.setColor(panelColor());background.setCornerRadius(dp(18));
        boolean lightCard = AppearancePreferences.lightMode(activity);
        // On a white card a white hairline is invisible: light mode uses a grey border.
        background.setStroke(dp(1), lightCard ? 0xffdcdcd8 : Color.argb(70,255,255,255));
        card.setBackground(background);
        LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);
        margin.bottomMargin=dp(12);margin.leftMargin=dp(12);margin.rightMargin=dp(12);margin.topMargin=dp(4);
        form.addView(card,margin);
        LinearLayout toggleRow=new LinearLayout(activity);toggleRow.setOrientation(HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);toggleRow.setPadding(dp(4),dp(10),dp(4),dp(10));
        TextView toggle=label(title,18);toggle.setTypeface(VisualEffects.type(activity));toggle.setGravity(Gravity.CENTER_VERTICAL);
        TextView arrow=label(expandMark(id),16,ACCENT);arrow.setPadding(dp(10),0,0,0);
        LinearLayout headText=column();headText.addView(toggle);
        if(subtitle!=null){TextView sub=label(subtitle,12);sub.setTextColor(MUTED);sub.setPadding(0,dp(1),0,0);headText.addView(sub);}
        toggleRow.addView(headText,new LinearLayout.LayoutParams(0,-2,1));toggleRow.addView(arrow);
        card.addView(toggleRow,new LinearLayout.LayoutParams(-1,-2));
        View line=new View(activity);
        line.setBackgroundColor(AppearancePreferences.lightMode(activity)
                ? 0xffe2e2de : Color.argb(40,255,255,255));
        line.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));card.addView(line);
        LinearLayout body=column();card.addView(body,new LinearLayout.LayoutParams(-1,-2));
        boolean expanded=ui.getBoolean("expanded_"+id,false);
        body.setVisibility(expanded?View.VISIBLE:View.GONE);arrow.setText(expandMark(id));
        toggleRow.setClickable(true);
        toggleRow.setFocusable(true);

        // Whole card reacts: the border brightens and the card presses in, not just the text.
        final GradientDrawable cardBg = background;
        toggleRow.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    // No scale: a full-width row scaled down resamples its text and
                    // shimmers. A tint shift gives the same "heard you" without it.
                    cardBg.setColor(Color.argb(18, 255, 255, 255));
                    cardBg.setStroke(dp(1), Color.argb(150, 255, 255, 255));
                    arrow.animate().cancel();
                    arrow.animate().alpha(.6f).setDuration(100)
                        .setInterpolator(VisualEffects.EASE_OUT).start();
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().cancel();
                    cardBg.setColor(panelColor());
                    cardBg.setStroke(dp(1), Color.argb(70, 255, 255, 255));
                    arrow.animate().cancel();
                    arrow.animate().alpha(1f).setDuration(140)
                        .setInterpolator(VisualEffects.EASE_OUT).start();
                    if (ev.getActionMasked() == android.view.MotionEvent.ACTION_UP) v.performClick();
                    return true;
            }
            return false;
        });

        pressFeedback(toggleRow);
        toggleRow.setOnClickListener(v -> {
            boolean open = body.getVisibility() != View.VISIBLE;
            animateBody(body, open, arrow, id, title);
            toggle.setContentDescription(title + (open ? "，已展开" : "，已折叠"));
        });

        return body;
    }

    /**
     * Height animation for a collapsible body. Cancels any in-flight animation first, so a
     * second tap mid-flight retargets from the current height instead of snapping.
     */
    /** The OS asks for gentler motion; honour it rather than ignoring it. */
    private boolean reducedMotion() {
        return !android.animation.ValueAnimator.areAnimatorsEnabled();
    }

    /**
     * Expand / collapse without animating height.
     *
     * Animating height relayouts the whole card every frame (3 dropped frames measured on
     * device). Instead the body keeps its wrap_content height and is clipped by the card:
     * only clipBounds changes, which is a draw-time operation, and the rows slide on
     * translationY. The clip still has to be written per frame, but nothing re-measures.
     */
    private void animateBody(android.view.ViewGroup body, boolean open, TextView arrow, String id, String title) {
        body.animate().cancel();
        final int full = measureBody(body);
        final int live = body.getHeight() > 0 ? body.getHeight() : (open ? 0 : full);

        // The body is clipped to its own bounds; animating "revealed" height means
        // shrinking the clip rect from the bottom.
        body.setVisibility(View.VISIBLE);
        // Pin the height so wrap_content does not collapse while we clip; it is restored
        // to WRAP_CONTENT once the animation settles.
        body.getLayoutParams().height = full;
        body.requestLayout();
        body.setClipBounds(new android.graphics.Rect(0, 0, body.getWidth(), live));

        int target = open ? full : 0;
        ValueAnimator va = ValueAnimator.ofInt(live, target);
        int distance = Math.abs(target - live);
        va.setDuration(Math.max(140, Math.min(280, 140 + distance / 3)));
        va.setInterpolator(reducedMotion() ? new android.view.animation.LinearInterpolator()
                                           : VisualEffects.EASE_IN_OUT);
        va.addUpdateListener(a -> {
            int h = (int) a.getAnimatedValue();
            body.setClipBounds(new android.graphics.Rect(0, 0, body.getWidth(), h));
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                body.setClipBounds(null);
                body.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                // No requestLayout() here: the clip is already cleared and the height only
                // needs to be re-read on the next pass. Forcing a synchronous measure on the
                // settle frame is what made the collapse drop one frame at the end.
                if (!open) body.setVisibility(View.GONE);
            }
        });
        va.start();

        arrow.animate().cancel();
        arrow.animate().rotation(open ? 90f : 0f)
            .setDuration(220).setInterpolator(VisualEffects.EASE_OUT).start();

        // Rows rise in with a short stagger so the block reads as content arriving.
        if (!reducedMotion()) {
            for (int i = 0; i < body.getChildCount(); i++) {
                View row = body.getChildAt(i);
                if (open) {
                    row.setAlpha(0f);
                    row.setTranslationY(dp(10));
                    row.animate().alpha(1f).translationY(0f)
                            .setStartDelay(30 + i * 40L)
                            .setDuration(200)
                            .setInterpolator(VisualEffects.EASE_OUT).start();
                } else {
                    row.animate().cancel();
                    row.setAlpha(1f);
                    row.setTranslationY(0f);
                }
            }
        }
        body.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        ui.edit().putBoolean("expanded_" + id, open).apply();
    }

    private int measureBody(android.view.ViewGroup body) {
        int w = body.getWidth();
        if (w <= 0) w = getWidth() - dp(60);
        body.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1, w), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return body.getMeasuredHeight();
    }
    private String expandMark(String id){return "\u25b8";}
    /** Ripple + hairline that makes a whole header row read as one pressable control. */
    private android.graphics.drawable.Drawable pressableRow(){
        GradientDrawable fill=new GradientDrawable();
        fill.setColor(Color.TRANSPARENT);
        fill.setCornerRadius(dp(12));
        GradientDrawable mask=new GradientDrawable();
        mask.setColor(Color.WHITE);mask.setCornerRadius(dp(12));
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22ffffff),fill,mask);
    }

    private SizeCapsule sizeCapsule;

    /** One row per face: label in its own typeface, check mark on the active one. */
    private boolean isInside(View view,View ancestor){for(ViewGroup parent=null;view!=null&&(parent=(ViewGroup)view.getParent())!=null;view=parent)if(parent==ancestor)return true;return false;}

    private void addField(LinearLayout form,String title,View control){
        TextView t=label(title,12);t.setTextColor(MUTED);t.setPadding(dp(4),dp(10),0,dp(4));
        GradientDrawable field=new GradientDrawable();
        field.setColor(tint(30));field.setCornerRadius(dp(12));
        control.setBackground(field);control.setPadding(dp(12),dp(4),dp(12),dp(4));
        control.setMinimumHeight(dp(56));
        LinearLayout wrap=column();wrap.addView(t);wrap.addView(control,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);
        form.addView(wrap,p);
    }

    private ArrayAdapter<String> adapter(String[] values){
        return new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_item,values){
            private TextView makeRow(View convert,int pos,boolean dropdown){
                TextView t=convert instanceof TextView?(TextView)convert:new TextView(activity);
                t.setText(values[pos]);t.setTextColor(fg());t.setTextSize(19);
                t.setGravity(Gravity.CENTER_VERTICAL);t.setSingleLine(false);
                t.setMinHeight(dp(dropdown?60:64));t.setPadding(dp(16),dp(12),dp(16),dp(12));
                t.setBackgroundColor(dropdown?panelColor():bg());return t;
            }
            @Override public View getView(int pos,View cv,ViewGroup parent){return makeRow(cv,pos,false);}
            @Override public View getDropDownView(int pos,View cv,ViewGroup parent){return makeRow(cv,pos,true);}
        };
    }


    /**
     * One tool row: name, a tick/cross toggle, and a chevron into its detail sheet.
     *
     * The toggle is a plain text glyph rather than a Switch widget because the platform
     * switch drags in its own track colours and thumb, which do not survive the day/night
     * switch cleanly. A two-state glyph is also instantly readable at this size.
     */
    /**
     * One tool row: name on the left, a sliding gate on the right, a chevron into the detail.
     *
     * Three deliberate choices:
     *  - The gate is a drawn control, not a ✓/✗ glyph pair. The two glyphs have different
     *    widths, so the row's baseline jitters every time the state flips, and a glyph cannot
     *    show the transition at all.
     *  - The whole row is pressable for the gate, because 38dp is a small target on a 500px
     *    screen. The chevron keeps its own hit area and a 8dp gap so the two never fight.
     *  - No toast on toggle. The gate itself is the feedback; a second message elsewhere on
     *    the screen just costs the user a glance for nothing.
     */
    private LinearLayout toolRow(String name, java.util.function.Supplier<Boolean> get,
                                 java.util.function.Consumer<Boolean> set, Runnable detail) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = label(name, 15, fg());
        label.setSingleLine(true);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));

        final ToolToggle gate = new ToolToggle(activity);
        gate.setOn(get.get(), false);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(dp(46), dp(44));
        row.addView(gate, glp);

        gate.setOnChange(on -> set.accept(on));

        // The label doubles as a second hit area for the gate: a name is a much wider target
        // than a 38dp pill, and tapping a tool's name to switch it on is what people expect.
        pressFeedback(label);
        label.setOnClickListener(v -> {
            boolean next = !gate.isOn();
            gate.setOn(next, true);
            set.accept(next);
        });

        TextView more = label("›", 20, AppearancePreferences.inkMuted(activity));
        more.setGravity(Gravity.CENTER);
        more.setContentDescription(name + " 设置");
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(dp(34), dp(44));
        mlp.leftMargin = dp(2);
        row.addView(more, mlp);
        pressFeedback(more);
        more.setOnClickListener(v -> detail.run());

        return row;
    }


    /** Last saved prompt, kept so toggling the tool off and on does not lose the text. */
    private static String lastPrompt = "";
    private String savedPrompt() {
        String cur = com.example.watchchat.data.PromptStore.systemPrompt(activity);
        return cur.isEmpty() ? lastPrompt : cur;
    }

    /** Editor for the system prompt, opened from its chevron in the tools card. */
    private void showPromptDetail() {
        final android.app.Dialog dialog = new android.app.Dialog(activity,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout overlay = column();
        overlay.setBackgroundColor(AppearancePreferences.lightMode(activity) ? 0x66FFFFFF : 0xAA000000);

        LinearLayout sheet = column();
        overlay.setOnClickListener(v -> dismissWithExit(dialog, sheet));
        sheet.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppearancePreferences.lightMode(activity) ? 0xffffffff : 0xff141414);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), AppearancePreferences.lightMode(activity) ? 0xffdcdcd8 : 0x33FFFFFF);
        sheet.setBackground(bg);
        sheet.setOnClickListener(v -> { });

        TextView title = label("\u7cfb\u7edf\u63d0\u793a\u8bcd", 16, fg());
        title.setTypeface(VisualEffects.type(activity));
        sheet.addView(title, new LinearLayout.LayoutParams(-1, dp(28)));

        final EditText field = new EditText(activity);
        field.setText(com.example.watchchat.data.PromptStore.systemPrompt(activity));
        field.setHint("\u4f8b\uff1a\u4f60\u662f\u4e00\u4e2a\u7b80\u6d01\u7684\u52a9\u624b");
        field.setTextColor(fg());
        field.setHintTextColor(AppearancePreferences.lightMode(activity)
                ? Color.argb(140, 0, 0, 0) : Color.argb(90, 205, 208, 198));
        field.setTextSize(14);
        field.setGravity(Gravity.TOP | Gravity.START);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        field.setMinLines(4);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        GradientDrawable pbox = new GradientDrawable();
        boolean pLight = AppearancePreferences.lightMode(activity);
        pbox.setColor(pLight ? 0xffffffff : 0xff141414);
        pbox.setCornerRadius(dp(10));
        pbox.setStroke(dp(1), pLight ? 0xffe4e4e0 : Color.argb(60, 255, 255, 255));
        field.setBackground(pbox);
        sheet.addView(field, new LinearLayout.LayoutParams(-1, dp(150)));

        final TextView note = label("", 12, fg());
        note.setPadding(0, dp(6), 0, 0);
        sheet.addView(note, new LinearLayout.LayoutParams(-1, -2));
        final Runnable sync = () -> {
            int t = com.example.watchchat.data.TokenEstimator.estimate(field.getText().toString());
            note.setText(t == 0 ? "\u672a\u8bbe\u7f6e" : "\u7ea6 " + t + " tokens");
        };
        sync.run();
        field.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void onTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) { sync.run(); }
        });

        TextView save = actionBtn("\u4fdd\u5b58", buttonFill(), fg());
        pressFeedback(save);
        save.setOnClickListener(v -> {
            String text = field.getText().toString().trim();
            com.example.watchchat.data.PromptStore.setSystemPrompt(activity, text);
            lastPrompt = text;
            Hints.show(activity, text.isEmpty() ? "\u5df2\u6e05\u7a7a\u63d0\u793a\u8bcd" : "\u63d0\u793a\u8bcd\u5df2\u4fdd\u5b58");
            dialog.dismiss();
            rebuild();
        });
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(-1, dp(42));
        slp2.topMargin = dp(10);
        sheet.addView(save, slp2);

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(-1, -2);
        flp.gravity = Gravity.CENTER;
        flp.setMargins(dp(18), dp(18), dp(18), dp(18));
        overlay.addView(sheet, flp);
        dialog.setContentView(overlay);
        VisualEffects.applyFontToTree(overlay, activity);
        dialog.show();
        animateSheetIn(overlay, sheet);
    }



    /**
     * Entrance for a detail sheet.
     *
     * Gate: a detail sheet is opened occasionally, so it earns a standard animation. Purpose:
     * prevent a jarring change — the sheet used to appear between frames.
     *
     * Only transform and opacity animate, so nothing re-lays-out while it plays.
     * The sheet grows from scale .94 to 1 rather than from 0: nothing in the real world
     * appears from nothing, and a 0 start reads as a flicker on a small screen.
     * The scrim fades on the same 220ms clock so the two cannot desync.
     *
     * EASE_OUT, not EASE_IN: an entrance that starts slow delays the exact moment the user
     * is already looking at.
     */
    private void animateSheetIn(android.view.View scrim, android.view.View sheet) {
        boolean reduced = !android.animation.ValueAnimator.areAnimatorsEnabled();
        if (reduced) return;   // the content is already correct; motion is optional

        scrim.setAlpha(0f);
        sheet.setAlpha(0f);
        sheet.setScaleX(0.94f);
        sheet.setScaleY(0.94f);

        scrim.animate().alpha(1f)
                .setDuration(220)
                .setInterpolator(VisualEffects.EASE_OUT)
                .start();

        sheet.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(220)
                .setInterpolator(VisualEffects.EASE_OUT)
                .start();
    }

    /** Mirrors the entrance: exit faster than entry, and toward the same place. */
    private void dismissWithExit(final android.app.Dialog dialog,
                                 android.view.View sheet) {
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) {
            dialog.dismiss();
            return;
        }
        sheet.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f)
                .setDuration(140)          // exits are faster than entrances
                .setInterpolator(VisualEffects.EASE_OUT)
                .withEndAction(dialog::dismiss)
                .start();
    }


    /**
     * Sets a text size that shrinks until the string fits the given width.
     *
     * Why not a fixed size + ellipsis: two long model ids differ only in their tail
     * (…-fl vs …-pro), so truncation makes them identical and the user cannot tell which
     * row they are adding. Shrinking the type keeps the whole string readable, which is the
     * only property that matters in a chooser.
     *
     * Floor is 9sp: below that the glyphs stop resolving on a 240dpi screen. If even the
     * floor does not fit, the text is left to clip rather than becoming unreadably small.
     */
    private void fitTextSize(TextView view, String text, float maxSp, float minSp, int widthPx) {
        if (widthPx <= 0) return;
        float sp = maxSp;
        view.setTextSize(sp);
        while (sp > minSp) {
            float w = view.getPaint().measureText(text);
            if (w <= widthPx) return;
            sp -= 0.5f;
            view.setTextSize(sp);
        }
    }


    /**
     * Press feedback for any tappable row or button.
     *
     * Why scale and not a ripple: the app has a custom visual language on both surfaces, and a
     * Material ripple on one of them reads as a different app. A 3% scale is the same gesture
     * everywhere and carries the label and icon with it, which is what makes it feel physical.
     *
     * Why 0.97 and not 0.9: at 0.9 a text row visibly jumps. 0.97 is enough to register and
     * not enough to distract, which is the whole budget for something seen dozens of times a
     * day.
     *
     * on-down 100ms, on-release 140ms: entering feedback must feel instant, returning must
     * feel unhurried but not slow. Both are inside the 100-160ms press-feedback band.
     */
    private void pressFeedback(android.view.View v) {
        if (v == null) return;
        final float down = 0.97f;
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.animate().scaleX(down).scaleY(down)
                            .setDuration(100)
                            .setInterpolator(VisualEffects.EASE_OUT).start();
                    // Returning false keeps the click listener working: consuming here would
                    // swallow the tap and the row would stop responding.
                    return false;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().cancel();
                    view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(140)
                            .setInterpolator(VisualEffects.EASE_OUT).start();
                    return false;
            }
            return false;
        });
    }

    private void renderProviderCards(){
        if(providerList==null)return;
        providerList.removeAllViews();
        String activeKey=ProviderStore.activeKey(activity);
        for(ProviderStore.Provider p:ProviderStore.list(activity)){
            // Two lines instead of one: a long provider name used to wrap and orphan the
            // protocol/count suffix onto a second line at a random place. Name on its own
            // line (truncated, it is identifying), metadata on a quieter line below.
            LinearLayout gRow=row();gRow.setPadding(dp(2),dp(8),0,dp(2));
            LinearLayout gText=column();
            gText.setOrientation(LinearLayout.VERTICAL);
            TextView group=tiny(p.displayName);
            group.setSingleLine(true);
            group.setEllipsize(android.text.TextUtils.TruncateAt.END);
            group.setMaxLines(1);
            gText.addView(group,new LinearLayout.LayoutParams(-1,-2));
            TextView meta=label(p.protocol+" · "+p.models.size()+" 个模型",11,MUTED);
            meta.setSingleLine(true);
            meta.setEllipsize(android.text.TextUtils.TruncateAt.END);
            gText.addView(meta,new LinearLayout.LayoutParams(-1,-2));
            gRow.addView(gText,new LinearLayout.LayoutParams(0,-2,1));
            TextView editBtn=label("编辑",13,ACCENT);editBtn.setPadding(dp(8),0,dp(8),0);
            editBtn.setGravity(android.view.Gravity.CENTER);
            gRow.addView(editBtn);
            providerList.addView(gRow);
            pressFeedback(editBtn);
            editBtn.setOnClickListener(v->{
                editingId=p.id;editName.setText(p.displayName);editUrl.setText(p.baseUrl);editKey.setText(p.apiKey);
                editProtocol.setAnthropic("ANTHROPIC".equals(p.protocol),false);
                pickedModels=new ArrayList<>(p.models);

                Hints.show(activity, "已载入\u201c"+p.displayName+"\u201d，修改后点保存供应商");
            });
            for(final String model:p.models){
                boolean active=(p.id+"|"+model).equals(activeKey);
                LinearLayout card=row();card.setPadding(dp(10),dp(4),dp(10),dp(4));
                GradientDrawable bg=new GradientDrawable();
                bg.setColor(tint(30));bg.setCornerRadius(dp(12));
                bg.setStroke(dp(1),active?ACCENT:tint(40));card.setBackground(bg);
                final String rowText = model + (active ? "  ● 使用中" : "");
                TextView name=label(rowText,14,active?ACCENT:fg());
                name.setSingleLine(true);
                // Middle-truncation was wrong: sibling ids differ at the tail (-flash vs
                // -pro), so eliding the middle made two different models look identical.
                card.addView(name,new LinearLayout.LayoutParams(0,-2,1));
                name.post(() -> fitTextSize(name, rowText, 14f, 9f, name.getWidth() - dp(2)));
                TextView tune=label("\u203a",20,MUTED);
                tune.setGravity(Gravity.CENTER);
                tune.setContentDescription(model+" 设置");
                card.addView(tune,new LinearLayout.LayoutParams(dp(30),-2));
                pressFeedback(tune);
                tune.setOnClickListener(v->new ModelSettingsDialog(activity,p.id,model,st->
                        Hints.show(activity, "已保存 "+model+" 的设置")).show());
                TextView del=label("移除",12,MUTED);card.addView(del);
                LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,-2);clp.bottomMargin=dp(4);
                providerList.addView(card,clp);
                pressFeedback(card);
                card.setOnClickListener(v->{ProviderStore.setActive(activity,p.id,model);renderProviderCards();});
                pressFeedback(del);
                del.setOnClickListener(v->{ProviderStore.removeModel(activity,p.id,model);renderProviderCards();});
            }
        }
        if(providerList.getChildCount()==0)providerList.addView(tiny("还没有模型，在下方添加供应商并获取"));
    }

    private void renderPicked(){
        if(pickedBox==null)return;
        pickedBox.removeAllViews();
        pickedBox.addView(tiny("已加入模型（"+pickedModels.size()+"个）"));
        for(String model:pickedModels){
            LinearLayout r=row();r.setPadding(dp(8),dp(2),dp(8),dp(2));
            TextView m=label(model,13,fg());r.addView(m,new LinearLayout.LayoutParams(0,-2,1));
            TextView x=label("\u00d7",15,MUTED);r.addView(x);
            pickedBox.addView(r);
            pressFeedback(x);
            x.setOnClickListener(v->{pickedModels.remove(model);renderPicked();});
        }
    }

    private void fetchModels(){
        String u=editUrl.getText().toString().trim(),k=editKey.getText().toString().trim();
        if(u.isEmpty()){Hints.show(activity, "请先填写 API 地址");return;}
        Hints.show(activity, "正在获取模型\u2026");
        com.example.watchchat.data.ModelListClient.fetchAsync(u,k,editProtocol.isAnthropic()?"ANTHROPIC":"OPENAI",(models,error)->{
            if(error!=null){Hints.show(activity, "获取失败："+error);return;}
            if(models.isEmpty()){Hints.show(activity, "接口未返回模型");return;}
            showMultiSelectDialog(models);
        });
    }

    private void showMultiSelectDialog(List<String> models){
        android.app.Dialog dialog=new android.app.Dialog(activity);
        LinearLayout panel=column();panel.setPadding(dp(18),dp(16),dp(18),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xff15171a);bg.setCornerRadius(dp(34));panel.setBackground(bg);
        TextView head=label("选择要加入的模型",20,fg());head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        head.setGravity(Gravity.CENTER);head.setPadding(0,0,0,dp(10));panel.addView(head);
        ScrollView scroll=new ScrollView(activity);LinearLayout listBody=column();scroll.addView(listBody);
        Set<String> checked=new HashSet<>();
        if(editingId!=null){
            ProviderStore.Provider existing=ProviderStore.find(activity,editingId);
            if(existing!=null)for(String m:existing.models)if(models.contains(m))checked.add(m);
        }
        for(String model:models){
            LinearLayout r=row();r.setPadding(dp(20),dp(15),dp(20),dp(15));
            GradientDrawable rb=new GradientDrawable();
            rb.setColor(Color.argb(38,255,255,255));rb.setCornerRadius(dp(28));
            rb.setStroke(Math.max(2,dp(1.5f)),Color.argb(45,255,255,255));r.setBackground(rb);
            final TextView name=label(model,17,fg());
            name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            name.setSingleLine(true);
            // The ＋ takes ~34dp; everything else in the row is available to the id.
            final String idText = model;
            r.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            name.post(() -> fitTextSize(name, idText, 17f, 9f,
                    name.getWidth() - dp(2)));
            TextView mark=label("\uff0b",20,ACCENT);r.addView(mark);
            LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.bottomMargin=dp(10);
            listBody.addView(r,rlp);
            if(checked.contains(model)){mark.setText("\u2713");rb.setColor(Color.argb(70,216,255,103));rb.setStroke(Math.max(2,dp(1.5f)),0xffd8ff67);}
            pressFeedback(r);
            r.setOnClickListener(v->{
                if(checked.contains(model)){checked.remove(model);mark.setText("\uff0b");rb.setColor(Color.argb(38,255,255,255));rb.setStroke(Math.max(2,dp(1.5f)),Color.argb(45,255,255,255));}
                else{checked.add(model);mark.setText("\u2713");rb.setColor(Color.argb(70,216,255,103));rb.setStroke(Math.max(2,dp(1.5f)),0xffd8ff67);}
            });
        }
        TextView confirm=actionBtn("加入已选",ACCENT,Color.rgb(20,26,16));
        confirm.setTypeface(Typeface.DEFAULT,Typeface.BOLD);confirm.setGravity(Gravity.CENTER);
        pressFeedback(confirm);
        confirm.setOnClickListener(v->{mergePicked(models,checked);dialog.dismiss();
            Hints.show(activity, "已选 "+pickedModels.size()+" 个模型，点保存供应商生效");});
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        panel.addView(confirm,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView saveNow=actionBtn("保存供应商",buttonFill(),fg());
        saveNow.setTypeface(Typeface.DEFAULT,Typeface.BOLD);saveNow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams snlp=new LinearLayout.LayoutParams(-1,dp(48));snlp.topMargin=dp(8);
        panel.addView(saveNow,snlp);
        pressFeedback(saveNow);
        saveNow.setOnClickListener(v->{mergePicked(models,checked);dialog.dismiss();saveProvider();});
        dialog.setContentView(panel);
        android.view.Window w=dialog.getWindow();
        if(w!=null){
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics m=activity.getResources().getDisplayMetrics();
            // Leave a margin top and bottom: a fixed 480dp overflowed small screens.
            int maxH=(int)(m.heightPixels*0.82f);
            int maxW=(int)(m.widthPixels*0.88f);
            w.setLayout(Math.min(dp(340),maxW),Math.min(dp(480),maxH));
            w.setDimAmount(.55f);
        }
        dialog.show();
    }

    /** Merge semantics: models the API did not return this round are kept, not dropped. */
    private void mergePicked(List<String> returned,Set<String> checked){
        Set<String> seen=new HashSet<>(returned);
        List<String> kept=new ArrayList<>();
        for(String m:pickedModels)if(!seen.contains(m))kept.add(m);
        for(String m:checked)if(!kept.contains(m))kept.add(m);
        pickedModels.clear();pickedModels.addAll(kept);renderPicked();
    }

    private void saveProvider(){
        String name=editName.getText().toString().trim(),u=editUrl.getText().toString().trim(),k=editKey.getText().toString().trim();
        if(name.isEmpty()||u.isEmpty()){Hints.show(activity, "名称与地址不能为空");return;}
        if(pickedModels.isEmpty()){Hints.show(activity, "请先获取并选择至少一个模型");return;}
        String id=editingId!=null?editingId:"prov_"+System.currentTimeMillis();
        String protocol=editProtocol.isAnthropic()?"ANTHROPIC":"OPENAI";
        List<String> reasoning=new ArrayList<>();
        ProviderStore.upsert(activity,new ProviderStore.Provider(id,name,u,protocol,k,new ArrayList<>(pickedModels),reasoning));
        // Saving must not hijack the model the user is currently chatting with.
        String currentActive=ProviderStore.activeKey(activity);
        boolean noActive=ProviderStore.activeProvider(activity)==null;
        boolean activeWasOurs=id.equals(currentActive);
        if(noActive){ProviderStore.setActive(activity,id,pickedModels.get(0));}
        else if(activeWasOurs&&!pickedModels.contains(ProviderStore.activeModel(activity))){
            ProviderStore.setActive(activity,id,pickedModels.get(0));
        }
        editingId=null;pickedModels=new ArrayList<>();
        renderPicked();renderProviderCards();
        Hints.show(activity, "已保存，可在聊天页 \u25bc 切换模型");
    }
}
