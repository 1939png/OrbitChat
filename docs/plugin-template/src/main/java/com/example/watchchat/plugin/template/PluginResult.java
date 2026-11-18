package com.example.watchchat.plugin.template;

public final class PluginResult {
    private final boolean success;
    private final String message;
    private PluginResult(boolean success, String message) { this.success = success; this.message = message; }
    public static PluginResult ok() { return new PluginResult(true, ""); }
    public static PluginResult error(String message) { return new PluginResult(false, message == null ? "plugin error" : message); }
    public boolean isSuccess() { return success; }
    public String message() { return message; }
}
