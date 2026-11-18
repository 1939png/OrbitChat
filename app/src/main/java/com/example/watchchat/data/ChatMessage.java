package com.example.watchchat.data;

/** Immutable chat turn. */
public final class ChatMessage {
    public enum Role { SYSTEM, USER, ASSISTANT }
    public final String id;
    public final Role role;
    public final String content;
    public final long createdAt;
    public ChatMessage(String id, Role role, String content, long createdAt) {
        if (role == null || content == null) throw new IllegalArgumentException("role/content required");
        this.id = id; this.role = role; this.content = content; this.createdAt = createdAt;
    }
    public static ChatMessage of(Role role, String content) {
        return new ChatMessage(java.util.UUID.randomUUID().toString(), role, content, System.currentTimeMillis());
    }
}
