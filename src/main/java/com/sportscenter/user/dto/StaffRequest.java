package com.sportscenter.user.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffRequest(
        @NotBlank String name,
        @NotBlank String email,
        String phone,
        @NotBlank String role,
        String specialization,
        String password,
        String status
) {}
