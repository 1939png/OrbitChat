package com.example.watchchat.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Immutable per-send options. An empty reasoningValue leaves provider defaults unchanged. */
public final class RequestOptions {
    public static final String SEARCH_PREFERENCES = "chat_search_service";
    public static final String KEY_ENDPOINT = "endpoint", KEY_TOKEN = "token";
    public final boolean searchEnabled;
    public final String searchEndpoint, searchToken, reasoningValue;
    /** Output ceiling for this request, from the per-model settings. */
    public final int maxOutputTokens;

    public RequestOptions(boolean searchEnabled, String searchEndpoint, String searchToken, String reasoningValue) {
        this(searchEnabled, searchEndpoint, searchToken, reasoningValue, 4096);
    }
    public RequestOptions(boolean searchEnabled, String searchEndpoint, String searchToken, String reasoningValue, int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens <= 0 ? 4096 : maxOutputTokens;
        this.searchEnabled = searchEnabled;
        this.searchEndpoint = clean(searchEndpoint);
        this.searchToken = clean(searchToken);
        this.reasoningValue = clean(reasoningValue);
    }
    public static RequestOptions defaults() { return new RequestOptions(false, "", "", ""); }
    public static RequestOptions fromPreferences(Context context, boolean searchEnabled, String reasoningValue) {
        SharedPreferences prefs = context.getSharedPreferences(SEARCH_PREFERENCES, Context.MODE_PRIVATE);
        return new RequestOptions(searchEnabled, prefs.getString(KEY_ENDPOINT, ""), prefs.getString(KEY_TOKEN, ""), reasoningValue);
    }
    private static String clean(String text) { return text == null ? "" : text.trim(); }
}
