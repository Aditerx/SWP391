package com.sportscenter.membership;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PackageExtensionRequest(
        @NotNull(message = "extendDays is required")
        @Min(value = 1, message = "extendDays must be at least 1")
        Integer extendDays,

        String reason
) {}

