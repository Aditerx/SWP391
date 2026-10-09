package com.sportscenter.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record MemberRequest(
        @NotBlank String name,
        @NotBlank String email,
        String phone,
        String password,
        Integer packageId,
        String goal,
        String healthNote,
        String address,
        String gender,
        LocalDate dateOfBirth,
        String status,
        Integer centerId
) {}
