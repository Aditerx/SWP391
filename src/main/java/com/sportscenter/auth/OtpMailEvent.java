package com.sportscenter.auth;

public record OtpMailEvent(String email, String otp, String purpose) {}
