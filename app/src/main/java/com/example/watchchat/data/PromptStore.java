package com.example.watchchat.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The system prompt sent ahead of every conversation.
 *
 * Kept separate from ModelSettings because it is per-install, not per-model: the user
 * writes one persona and every provider sees it. Empty by default — no invisible preamble
 * the user did not ask for.
 */
public final class PromptStore {

    private static final String PREFS = "prompt_store";
    private static final String KEY_SYSTEM = "system_prompt";

    private PromptStore() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String systemPrompt(Context c) {
        return prefs(c).getString(KEY_SYSTEM, "");
    }

    public static void setSystemPrompt(Context c, String value) {
        prefs(c).edit().putString(KEY_SYSTEM, value == null ? "" : value.trim()).apply();
    }
}
