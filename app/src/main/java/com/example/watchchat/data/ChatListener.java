package com.example.watchchat.data;

public interface ChatListener {
    void onDelta(String text);
    default void onThinkingDelta(String text) {}
    default void onStatus(String status) {}
    /** Provider usage; null fields remain unknown and are never estimated as exact. */
    default void onUsage(ChatUsage usage) {}
    void onComplete(String assistantText);
    void onError(ChatError error);
    void onCancelled();
}
