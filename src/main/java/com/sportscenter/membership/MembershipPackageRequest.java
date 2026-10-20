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
        String status,
        String packageType,
        Integer totalCredit) {

    public MembershipPackageRequest(String name, Integer durationDays, BigDecimal price, String benefits, String status) {
        this(name, durationDays, price, benefits, status, "Normal", 12);
    }
}
