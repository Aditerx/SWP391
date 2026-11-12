package com.sportscenter.chat.ai.tool;

/** Authenticated context supplied by the application, never by the model. */
public record ToolContext(Integer memberUserId, String memberQuestion) {
    public ToolContext(Integer memberUserId) { this(memberUserId, null); }
}
