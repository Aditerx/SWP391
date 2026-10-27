package com.sportscenter.auth;

public record OtpMessageResponse(String message, String code) {
    public OtpMessageResponse(String message) { this(message, null); }
}
