package com.sportscenter.chat;
public class ChatException extends RuntimeException {
    private final int status;
    private final String code;
    private final Integer retryAfterSeconds;
    public ChatException(int status, String code, String message) { this(status,code,message,null); }
    public ChatException(int status, String code, String message, Integer retryAfterSeconds) { super(message); this.status=status; this.code=code; this.retryAfterSeconds=retryAfterSeconds; }
    public int getStatus() { return status; }
    public String getCode() { return code; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
}
