package com.sportscenter.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UserRequest(
        @NotBlank String fullName,
        @NotBlank String email,
        String phone,
        String address,
        String gender,
        LocalDate dateOfBirth,
        String status,
        Integer roleId,
        String roleName,
        String password
) {}
