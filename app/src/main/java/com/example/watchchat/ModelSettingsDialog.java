package com.example.watchchat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import com.example.watchchat.data.ModelSettings;

/** Per-model capability sheet: vision, context window, output ceiling, reasoning effort. */
final class ModelSettingsDialog extends Dialog {
    interface OnSaved { void onSaved(ModelSettings settings); }

    private final String providerId, model;
    private final OnSaved onSaved;
    private final Typeface face;
    private ModelSettings draft;

    ModelSettingsDialog(Activity a, String providerId, String model, OnSaved saved) {
        super(a);
        this.providerId = providerId;
        this.model = model;
        this.onSaved = saved;
        this.face = FontStore.typeface(a);
        this.draft = ModelSettings.get(a, providerId, model);
        setOwnerActivity(a);
    }

    @Override protected void onCreate(android.os.Bundle state) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Context c = getContext();
        int dp = (int) (c.getResources().getDisplayMetrics().density + .5f);

        LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(14 * dp, 12 * dp, 14 * dp, 12 * dp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xff101010);
        bg.setCornerRadius(20 * dp);
        bg.setStroke(dp, 0x33FFFFFF);
        panel.setBackground(bg);

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(c);
        title.setText(model);
        title.setTextColor(AppearancePreferences.ink(getContext()));
        title.setTextSize(14);
        title.setTypeface(face);
        title.setMaxLines(1);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = new TextView(c);
        close.setText("×");
        close.setTextColor(AppearancePreferences.inkMuted(getContext()));
        close.setTextSize(20);
        close.setTypeface(face);
        close.setGravity(Gravity.CENTER);
        head.addView(close, new LinearLayout.LayoutParams(34 * dp, 34 * dp));
        panel.addView(head, new LinearLayout.LayoutParams(-1, -2));
        close.setOnClickListener(v -> dismiss());

        ScrollView scroll = new ScrollView(c);
        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // ---- vision ----
        LinearLayout visionRow = new LinearLayout(c);
        visionRow.setOrientation(LinearLayout.HORIZONTAL);
        visionRow.setGravity(Gravity.CENTER_VERTICAL);
        visionRow.setPadding(0, 8 * dp, 0, 2 * dp);
        TextView visionLabel = caption(c, "视觉能力", dp);
        visionRow.addView(visionLabel, new LinearLayout.LayoutParams(0, -2, 1));
        Switch visionSwitch = new Switch(c);
        visionSwitch.setChecked(draft.vision);
        visionSwitch.setButtonTintList(android.content.res.ColorStateList.valueOf(
                AppearancePreferences.accent(c)));
        visionRow.addView(visionSwitch, new LinearLayout.LayoutParams(-2, -2));
        body.addView(visionRow, new LinearLayout.LayoutParams(-1, -2));
        visionSwitch.setOnCheckedChangeListener((CompoundButton b, boolean on) ->
                draft = new ModelSettings(on, draft.contextIndex, draft.outputIndex, draft.reasoningIndex));

        // ---- context window ----
        body.addView(caption(c, "上下文长度", dp), new LinearLayout.LayoutParams(-1, -2));
        OptionSlider ctx = new OptionSlider(c);
        ctx.setLabels(ModelSettings.CONTEXT_LABELS);
        ctx.setIndex(draft.contextIndex, false);
        body.addView(ctx, new LinearLayout.LayoutParams(-1, -2));
        ctx.setOnPick(i -> draft = new ModelSettings(draft.vision, i, draft.outputIndex, draft.reasoningIndex));

        // ---- max output ----
        body.addView(caption(c, "最大输出", dp), new LinearLayout.LayoutParams(-1, -2));
        OptionSlider out = new OptionSlider(c);
        out.setLabels(ModelSettings.OUTPUT_LABELS);
        out.setIndex(draft.outputIndex, false);
        body.addView(out, new LinearLayout.LayoutParams(-1, -2));
        out.setOnPick(i -> draft = new ModelSettings(draft.vision, draft.contextIndex, i, draft.reasoningIndex));

        // ---- reasoning: a checklist, not a slider. Each ticked level becomes a stop on the
        //      chat page's reasoning slider, so the user picks which values they want to
        //      switch between while chatting rather than committing to one up front.
        body.addView(caption(c, "思考强度（勾选后可在聊天页切换）", dp), new LinearLayout.LayoutParams(-1, -2));
        java.util.Set<Integer> enabled = new java.util.HashSet<>(
                com.example.watchchat.data.ModelSettings.getEnabledReasoning(
                        c, providerId, model));
        for (int i = 0; i < ModelSettings.REASONING_LABELS.length; i++) {
            final int index = i;
            LinearLayout rowView = new LinearLayout(c);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            rowView.setGravity(Gravity.CENTER_VERTICAL);
            rowView.setPadding(2 * dp, 6 * dp, 2 * dp, 6 * dp);

            TextView name = new TextView(c);
            name.setText(ModelSettings.REASONING_LABELS[i]);
            name.setTextSize(14);
            name.setTypeface(face);
            name.setTextColor(AppearancePreferences.ink(getContext()));
            rowView.addView(name, new LinearLayout.LayoutParams(0, -2, 1));

            final TextView box = new TextView(c);
            box.setGravity(Gravity.CENTER);
            box.setTextSize(15);
            box.setTypeface(face);
            rowView.addView(box, new LinearLayout.LayoutParams(30 * dp, 30 * dp));

            final boolean[] on = {enabled.contains(i)};
            paintCheck(box, on[0]);
            rowView.setOnClickListener(v -> {
                // never allow zero choices: the chat slider would have nothing to show
                if (on[0] && enabled.size() <= 1) return;
                on[0] = !on[0];
                if (on[0]) enabled.add(index); else enabled.remove(index);
                paintCheck(box, on[0]);
            });
            body.addView(rowView, new LinearLayout.LayoutParams(-1, -2));
        }

        // ---- user-defined suffixes ----
        // Same shape as the built-in levels above: a tick box on the right, and here an
        // editable field in place of a fixed label. The previous version was two bare
        // EditTexts with no box at all, which read as an unrelated text form — and because
        // nothing added their indices to the enabled set, a custom suffix could never reach
        // the chat slider no matter what was typed.
        body.addView(caption(c, "自定义后缀", dp), new LinearLayout.LayoutParams(-1, -2));
        java.util.List<String> customs = new java.util.ArrayList<>(
                com.example.watchchat.data.ModelSettings.getCustomReasoning(c, providerId, model));
        java.util.List<android.widget.EditText> customFields = new java.util.ArrayList<>();

        for (int slot = 0; slot < com.example.watchchat.data.ModelSettings.CUSTOM_SLOTS; slot++) {
            final int customIndex = ModelSettings.REASONING_LABELS.length + slot;

            LinearLayout rowView = new LinearLayout(c);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            rowView.setGravity(Gravity.CENTER_VERTICAL);
            rowView.setPadding(2 * dp, 5 * dp, 2 * dp, 5 * dp);

            android.widget.EditText field = new android.widget.EditText(c);
            field.setHint("后缀 " + (slot + 1) + "（如 minimal）");
            field.setTextSize(13);
            field.setSingleLine(true);
            field.setTypeface(face);
            field.setTextColor(AppearancePreferences.ink(getContext()));
            field.setHintTextColor(AppearancePreferences.lightMode(getContext())
                    ? 0x66000000 : 0x5AFFFFFF);
            field.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            if (slot < customs.size()) field.setText(customs.get(slot));

            android.graphics.drawable.GradientDrawable bgField = new android.graphics.drawable.GradientDrawable();
            bgField.setColor(AppearancePreferences.lightMode(getContext()) ? 0xffffffff : 0xff141414);
            bgField.setCornerRadius(8 * dp);
            bgField.setStroke(dp, AppearancePreferences.lightMode(getContext())
                    ? 0xffdcdcd8 : 0x33FFFFFF);
            field.setBackground(bgField);
            field.setPadding(10 * dp, 7 * dp, 10 * dp, 7 * dp);
            rowView.addView(field, new LinearLayout.LayoutParams(0, 40 * dp, 1f));

            final TextView box = new TextView(c);
            box.setGravity(Gravity.CENTER);
            box.setTextSize(15);
            box.setTypeface(face);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(34 * dp, 34 * dp);
            blp.leftMargin = 8 * dp;
            rowView.addView(box, blp);

            final boolean[] on = { enabled.contains(customIndex)
                    && slot < customs.size() && !customs.get(slot).isEmpty() };
            paintCheck(box, on[0]);

            final android.widget.EditText bindField = field;
            final TextView bindBox = box;
            rowView.setOnClickListener(v -> {
                String typed = bindField.getText().toString().trim();
                if (typed.isEmpty()) {
                    // Nothing to send yet: ask for the text instead of ticking an empty row.
                    bindField.requestFocus();
                    return;
                }
                if (on[0] && enabled.size() <= 1) return;
                on[0] = !on[0];
                if (on[0]) enabled.add(customIndex); else enabled.remove(customIndex);
                paintCheck(bindBox, on[0]);
            });

            body.addView(rowView, new LinearLayout.LayoutParams(-1, -2));
            customFields.add(field);
        }
        final java.util.Set<Integer> finalEnabled = enabled;

        TextView save = new TextView(c);
        save.setText("保存");
        save.setTextSize(15);
        save.setTypeface(face);
        save.setGravity(Gravity.CENTER);
        save.setTextColor(0xff101010);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(AppearancePreferences.accent(c));
        saveBg.setCornerRadius(12 * dp);
        save.setBackground(saveBg);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, 44 * dp);
        slp.topMargin = 10 * dp;
        panel.addView(save, slp);
        save.setOnClickListener(v -> {
            ModelSettings.put(getContext(), providerId, model, draft);
            java.util.List<String> typed = new java.util.ArrayList<>();
            for (android.widget.EditText f : customFields) typed.add(f.getText().toString());

            // The saved set has to be rebuilt, not copied: a ticked custom slot whose field was
            // later cleared must drop out, or the slider would show an empty stop.
            java.util.Set<Integer> toSave = new java.util.HashSet<>(finalEnabled);
            for (int slot = 0; slot < typed.size(); slot++) {
                int idx = ModelSettings.REASONING_LABELS.length + slot;
                String t = typed.get(slot);
                boolean usable = t != null && !t.trim().isEmpty();
                if (!usable) toSave.remove(idx);
            }
            if (toSave.isEmpty()) toSave.add(0);   // the slider always needs one stop

            com.example.watchchat.data.ModelSettings.putEnabledReasoning(
                    getContext(), providerId, model, toSave);
            com.example.watchchat.data.ModelSettings.putCustomReasoning(
                    getContext(), providerId, model, typed);
            if (onSaved != null) onSaved.onSaved(draft);
            dismiss();
        });

        VisualEffects.applyFontToTree(panel, c);

        setContentView(panel);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            android.util.DisplayMetrics m = c.getResources().getDisplayMetrics();
            // Bound to the real screen: a fixed height will overflow a small watch.
            w.setLayout(Math.min(310 * dp, (int) (m.widthPixels * 0.90f)),
                        Math.min(420 * dp, (int) (m.heightPixels * 0.84f)));
            w.setDimAmount(.6f);
        }
    }

    private void paintCheck(TextView box, boolean on) {
        int unit = (int) (getContext().getResources().getDisplayMetrics().density + .5f);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(8 * unit);
        d.setColor(on ? AppearancePreferences.accent(getContext())
                : (AppearancePreferences.lightMode(getContext()) ? 0xffffffff : 0xff141414));
        d.setStroke(unit, on ? AppearancePreferences.accent(getContext()) : 0x55FFFFFF);
        box.setBackground(d);
        box.setText(on ? "\u2713" : "");
        box.setTextColor(0xff101010);
    }

    private TextView caption(Context c, String text, int dp) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(AppearancePreferences.inkMuted(getContext()));
        t.setTypeface(face);
        t.setPadding(2 * dp, 10 * dp, 2 * dp, 0);
        return t;
    }
}
