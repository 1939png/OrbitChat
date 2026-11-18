package com.example.watchchat.data;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;

/** Capability resolution: official allowlist first, then per-provider custom reasoning suffixes. */
public final class ModelCapabilities {
    private final String[] labels, values;
    private final boolean anthropic;
    private boolean deepseekFlash;
    private final List<String> customSuffixes;
    private ModelCapabilities(boolean anthropic, String[] labels, String[] values) { this(anthropic, labels, values, null); }
    private ModelCapabilities(boolean anthropic, String[] labels, String[] values, List<String> custom) {
        this.anthropic = anthropic; this.labels = labels; this.values = values; this.customSuffixes = custom == null ? new ArrayList<>() : custom;
    }
    /** Custom suffix entry format: "label:value" pairs, e.g. "关:off,低:low,高:high". */
    public static ModelCapabilities forConfig(ChatConfig config) { return forConfig(config, null); }
    public static ModelCapabilities forConfig(ChatConfig config, List<String> customOptions) {
        ModelCapabilities official = officialFor(config);
        if (official != null) return official;
        if (customOptions != null && !customOptions.isEmpty()) return parseCustom(customOptions, config != null && config.provider == ChatConfig.Provider.ANTHROPIC);
        return new ModelCapabilities(config != null && config.provider == ChatConfig.Provider.ANTHROPIC, new String[0], new String[0]);
    }
    private static ModelCapabilities officialFor(ChatConfig config) {
        if (config == null) return null;
        if (config.provider == ChatConfig.Provider.OPENAI && "deepseek-flash".equals(config.model)) {
            ModelCapabilities c = new ModelCapabilities(false, new String[]{"NONE", "LOW", "HIGH", "MAX"}, new String[]{"none", "low", "high", "max"});
            c.deepseekFlash = true; return c;
        }
        try {
            URI uri = new URI(config.baseUrl.trim());
            boolean official = "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443);
            if (official && config.provider == ChatConfig.Provider.OPENAI && "api.openai.com".equalsIgnoreCase(uri.getHost())
                    && Arrays.asList("o1", "o1-2024-12-17", "o3-mini", "o3-mini-2025-01-31", "o3", "o3-2025-04-16", "o4-mini", "o4-mini-2025-04-16").contains(config.model))
                return new ModelCapabilities(false, new String[]{"低", "中", "高"}, new String[]{"low", "medium", "high"});
            if (official && config.provider == ChatConfig.Provider.ANTHROPIC && "api.anthropic.com".equalsIgnoreCase(uri.getHost())
                    && Arrays.asList("claude-3-7-sonnet-20250219", "claude-sonnet-4-20250514", "claude-opus-4-20250514").contains(config.model))
                return new ModelCapabilities(true, new String[]{"预算预设 1024", "预算预设 2048", "预算预设 4096"}, new String[]{"1024", "2048", "4096"});
        } catch (Exception ignored) { }
        return null;
    }
    /** Custom entries are raw suffix values appended to the payload as reasoning_effort. */
    private static ModelCapabilities parseCustom(List<String> options, boolean anthropic) {
        List<String> labels = new ArrayList<>(), values = new ArrayList<>();
        for (String entry : options) {
            if (entry == null) continue;
            String e = entry.trim(); if (e.isEmpty()) continue;
            int sep = e.indexOf(':');
            String label = sep > 0 ? e.substring(0, sep).trim() : e;
            String value = sep > 0 ? e.substring(sep + 1).trim() : e;
            if (label.isEmpty() || value.isEmpty()) continue;
            labels.add(label); values.add(value);
        }
        return new ModelCapabilities(anthropic, labels.toArray(new String[0]), values.toArray(new String[0]));
    }
    public String[] labels() { return labels.clone(); }
    public String[] values() { return values.clone(); }
    public boolean supports(String value) { return Arrays.asList(values).contains(value); }
    public void apply(JSONObject payload, String value) throws org.json.JSONException {
        // Do not gate on supports(): user-defined suffixes are not in the built-in list and
        // were being silently dropped, so the picker appeared to do nothing.
        if (value == null || value.isEmpty()) return;
        if (anthropic) {
            try { int budget = Integer.parseInt(value);
                payload.put("thinking", new JSONObject().put("type", "enabled").put("budget_tokens", budget));
                payload.put("max_tokens", budget + 4096); return;
            } catch (NumberFormatException ignored) { /* custom suffix falls through */ }
        }
        // "none" is a real value: DeepSeek's compatible dialects switch thinking off through a
        // separate object, everything else accepts the literal in reasoning_effort.
        boolean off = "none".equals(value);
        if (deepseekFlash || off) {
            payload.put("thinking", new JSONObject().put("type", off ? "disabled" : "enabled"));
        }
        // Send the suffix verbatim, including user-defined ones.
        payload.put("reasoning_effort", value);
    }
}
