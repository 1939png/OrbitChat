package com.example.watchchat.plugin.template;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit registry example; adapt when the host adds a real plugin lifecycle. */
public final class PluginRegistry {
    private final Map<String, Plugin> plugins = new LinkedHashMap<>();
    public void register(Plugin plugin) {
        if (plugin == null || plugin.id() == null || plugin.id().trim().isEmpty()) throw new IllegalArgumentException("plugin id required");
        if (plugins.containsKey(plugin.id())) throw new IllegalArgumentException("duplicate plugin id: " + plugin.id());
        plugins.put(plugin.id(), plugin);
    }
    public Plugin find(String id) { return plugins.get(id); }
    public PluginResult execute(String id, PluginContext context, String input) {
        Plugin plugin = find(id);
        if (plugin == null) return PluginResult.error("unknown plugin: " + id);
        try { return plugin.execute(context, input); } catch (RuntimeException e) { return PluginResult.error(e.getMessage()); }
    }
}
