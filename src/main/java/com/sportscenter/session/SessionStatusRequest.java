package com.sportscenter.session;

import jakarta.validation.constraints.NotBlank;

public record SessionStatusRequest(
        @NotBlank(message = "status is required")
        String status
) {}
