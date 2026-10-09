package com.sportscenter.auth;

import java.util.Set;

public record LoginResponse(
        Integer userId,
        String email,
        String fullName,
        String role,
        Set<String> authorities,
        boolean isFirstLogin) {
}
