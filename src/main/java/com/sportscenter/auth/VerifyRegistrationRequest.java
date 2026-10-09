package com.sportscenter.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyRegistrationRequest(@NotBlank @Email String email, @NotBlank String otp) {}
