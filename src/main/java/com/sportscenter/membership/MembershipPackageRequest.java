package com.sportscenter.membership;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MembershipPackageRequest(
        @NotBlank String name,
        @NotNull @Positive Integer durationDays,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        String benefits,
        String status) {
}
