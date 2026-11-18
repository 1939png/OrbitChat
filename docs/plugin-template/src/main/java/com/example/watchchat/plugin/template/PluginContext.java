package com.example.watchchat.plugin.template;

/** Narrow host capabilities exposed to a plugin. Keep secrets and UI internals out. */
public interface PluginContext {
    void postUserVisibleMessage(String text);
}
