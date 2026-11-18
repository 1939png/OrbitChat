package com.example.watchchat.data;

public final class ChatConfig {
    public enum Provider { OPENAI, ANTHROPIC }
    public final Provider provider; public final String baseUrl, apiKey, model;
    public final int connectTimeoutMs, readTimeoutMs, maxRetries;
    public ChatConfig(Provider provider, String baseUrl, String apiKey, String model, int connectTimeoutMs, int readTimeoutMs, int maxRetries) {
        if (provider == null || blank(baseUrl) || blank(model)) throw new IllegalArgumentException("provider, baseUrl and model required");
        if (connectTimeoutMs < 1 || readTimeoutMs < 1 || maxRetries < 0) throw new IllegalArgumentException("invalid timeout/retry setting");
        this.provider=provider; this.baseUrl=baseUrl.replaceAll("/+$", ""); this.apiKey=apiKey == null ? "" : apiKey; this.model=model; this.connectTimeoutMs=connectTimeoutMs; this.readTimeoutMs=readTimeoutMs; this.maxRetries=maxRetries;
    }
    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }
    public static ChatConfig defaults(Provider p, String key, String model) { return new ChatConfig(p, p==Provider.OPENAI?"https://api.openai.com/v1":"https://api.anthropic.com/v1", key, model, 15000, 60000, 1); }
}
