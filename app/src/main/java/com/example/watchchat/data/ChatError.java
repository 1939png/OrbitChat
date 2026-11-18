package com.example.watchchat.data;

public final class ChatError extends Exception {
    public final int httpStatus; public final boolean retryable;
    public ChatError(String message, int status, boolean retryable, Throwable cause) { super(message, cause); this.httpStatus=status; this.retryable=retryable; }
    public ChatError(String message, int status, boolean retryable) { this(message,status,retryable,null); }
}
