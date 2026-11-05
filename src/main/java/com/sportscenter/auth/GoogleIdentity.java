package com.sportscenter.auth;

public record GoogleIdentity(String sub, String email, boolean emailVerified, String name, String pictureUrl) {}
