package com.example.watchchat.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Per-model capability overrides. Stored separately from the provider so adding or renaming
 * a model never loses its settings, and a deleted model leaves nothing behind silently.
 */
public final class ModelSettings {
    private static final String PREFS = "model_settings";

    /** Reasoning suffixes offered in the picker; index 0 means "do not think". */
    public static final String[] REASONING_LABELS = {"none", "low", "high", "ultra", "max"};
    public static final String[] REASONING_VALUES = {"none", "low", "high", "ultra", "max"};
    /** How many user-defined suffixes a model may carry. They append to the built-ins. */
    public static final int CUSTOM_SLOTS = 3;

    /** Context window choices, in tokens. */
    public static final int[] CONTEXT_VALUES = {100_000, 256_000, 1_000_000};
    public static final String[] CONTEXT_LABELS = {"100K", "256K", "1M"};

    /** Max output tokens. Four common ceilings across current providers. */
    public static final int[] OUTPUT_VALUES = {4_096, 8_192, 32_768, 65_536};
    public static final String[] OUTPUT_LABELS = {"4K", "8K", "32K", "64K"};

    public final boolean vision;
    public final int contextIndex, outputIndex, reasoningIndex;

    public ModelSettings(boolean vision, int contextIndex, int outputIndex, int reasoningIndex) {
        this.vision = vision;
        this.contextIndex = clamp(contextIndex, CONTEXT_VALUES.length);
        this.outputIndex = clamp(outputIndex, OUTPUT_VALUES.length);
        this.reasoningIndex = clamp(reasoningIndex, REASONING_VALUES.length);
    }

    public static ModelSettings defaults() { return new ModelSettings(true, 0, 1, 0); }

    private static int clamp(int v, int n) { return v < 0 ? 0 : (v >= n ? n - 1 : v); }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String providerId, String model) {
        return providerId + "|" + model;
    }

    public static ModelSettings get(Context c, String providerId, String model) {
        SharedPreferences p = prefs(c);
        String k = key(providerId, model);
        return new ModelSettings(
                p.getBoolean(k + "|vision", true),
                p.getInt(k + "|ctx", 0),
                p.getInt(k + "|out", 1),
                p.getInt(k + "|reason", 0));
    }

    public static void put(Context c, String providerId, String model, ModelSettings s) {
        SharedPreferences p = prefs(c);
        String k = key(providerId, model);
        p.edit()
                .putBoolean(k + "|vision", s.vision)
                .putInt(k + "|ctx", s.contextIndex)
                .putInt(k + "|out", s.outputIndex)
                .putInt(k + "|reason", s.reasoningIndex)
                .apply();
    }

    /**
     * Which reasoning levels the user ticked for this model. Stored as a bitmask so a model
     * can expose a subset (e.g. only low/high) on the chat page slider.
     */
    public static java.util.Set<Integer> getEnabledReasoning(Context c, String providerId, String model) {
        int mask = prefs(c).getInt(providerId + "|" + model + "|reasonMask", -1);
        java.util.Set<Integer> out = new java.util.LinkedHashSet<>();
        if (mask == -1) { out.add(0); return out; }   // default: only "do not think"
        // The mask covers custom suffixes too, so the bound is built-ins + at most
        // CUSTOM_SLOTS. Looping to REASONING_VALUES.length was why a custom level could
        // never appear on the slider no matter how often it was ticked.
        int slots = REASONING_VALUES.length + getCustomReasoning(c, providerId, model).size();
        for (int i = 0; i < slots; i++) {
            if ((mask & (1 << i)) != 0) out.add(i);
        }
        if (out.isEmpty()) out.add(0);
        return out;
    }

    public static void putEnabledReasoning(Context c, String providerId, String model,
                                           java.util.Set<Integer> levels) {
        int mask = 0;
        for (int i : levels) mask |= (1 << i);
        if (mask == 0) mask = 1;   // always keep at least one
        prefs(c).edit().putInt(providerId + "|" + model + "|reasonMask", mask).apply();
    }

    /** User-defined suffixes for this model, e.g. ["minimal", "extreme"]. */
    public static java.util.List<String> getCustomReasoning(Context c, String providerId, String model) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String raw = prefs(c).getString(providerId + "|" + model + "|custom", "");
        if (raw == null || raw.isEmpty()) return out;
        for (String part : raw.split("\\n")) {
            String t = part.trim();
            if (!t.isEmpty() && out.size() < CUSTOM_SLOTS) out.add(t);
        }
        return out;
    }

    public static void putCustomReasoning(Context c, String providerId, String model,
                                          java.util.List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size() && i < CUSTOM_SLOTS; i++) {
            String t = values.get(i) == null ? "" : values.get(i).trim();
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(t);
        }
        prefs(c).edit().putString(providerId + "|" + model + "|custom", sb.toString()).apply();
    }

    /** Every suffix offered for a model: built-ins then user-defined, deduplicated. */
    public static java.util.List<String> allSuffixes(Context c, String providerId, String model) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (String v : REASONING_VALUES) if (!v.isEmpty()) out.add(v);
        out.addAll(getCustomReasoning(c, providerId, model));
        return new java.util.ArrayList<>(out);
    }

    /**
     * The suffix for a given ticked level, by label index into REASONING_VALUES.
     * Custom suffixes live past the built-ins.
     */
    public static String labelAt(Context c, String providerId, String model, int labelIndex) {
        java.util.List<String> custom = getCustomReasoning(c, providerId, model);
        int builtIn = REASONING_VALUES.length;
        if (labelIndex < builtIn) return REASONING_VALUES[labelIndex];
        int k = labelIndex - builtIn;
        return k < custom.size() ? custom.get(k) : REASONING_VALUES[0];
    }

    /** Label shown for a suffix index, custom ones included. */
    public static String labelTextAt(Context c, String providerId, String model, int labelIndex) {
        java.util.List<String> custom = getCustomReasoning(c, providerId, model);
        int builtIn = REASONING_LABELS.length;
        if (labelIndex < builtIn) return REASONING_LABELS[labelIndex];
        int k = labelIndex - builtIn;
        return k < custom.size() ? custom.get(k) : REASONING_LABELS[0];
    }

    /** Full label list for the chat slider: built-ins plus this model's custom suffixes. */
    public static java.util.List<String> allLabels(Context c, String providerId, String model) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String l : REASONING_LABELS) out.add(l);
        out.addAll(getCustomReasoning(c, providerId, model));
        return out;
    }

    /** Persists just the chosen reasoning level, leaving the other fields untouched. */
    public static void putReasoningIndex(Context c, String providerId, String model, int index) {
        ModelSettings cur = get(c, providerId, model);
        put(c, providerId, model, new ModelSettings(cur.vision, cur.contextIndex, cur.outputIndex, index));
    }

    /**
     * The reasoning suffix to append to the request, or "" for none.
     *
     * This overload is the one that works with custom suffixes: the no-arg version can only
     * reach the built-in array, because it has no Context to read the user strings from. A
     * model with a custom suffix would therefore send the wrong value — or the built-in one
     * at that index — which is why the feature looked broken.
     */
    public String reasoningValue(Context c, String providerId, String model) {
        return labelAt(c, providerId, model, reasoningIndex);
    }

    /** Built-ins only. Kept for callers that have no Context; prefer the overload above. */
    public String reasoningValue() {
        return reasoningIndex >= 0 && reasoningIndex < REASONING_VALUES.length
                ? REASONING_VALUES[reasoningIndex] : "";
    }

    public int contextTokens() { return CONTEXT_VALUES[contextIndex]; }

    public int outputTokens() { return OUTPUT_VALUES[outputIndex]; }
}
