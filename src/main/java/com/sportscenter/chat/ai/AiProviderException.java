package com.sportscenter.chat.ai;

public class AiProviderException extends RuntimeException {
    public AiProviderException() { super("AI provider unavailable"); }
    public AiProviderException(String category) { super("AI provider unavailable: " + safe(category)); }
    private static String safe(String value) { return value != null && value.matches("[a-zA-Z0-9_-]{1,40}") ? value : "unknown"; }
}
