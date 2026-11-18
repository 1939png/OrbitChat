package com.example.watchchat.plugin.template;

import java.util.Set;

/** Stable plugin contract example; the current app does not yet expose this host API. */
public interface Plugin {
    String id();
    String displayName();
    Set<String> capabilities();
    default void onLoad(PluginContext context) {}
    PluginResult execute(PluginContext context, String input);
    default void onUnload() {}
}
