package com.sportscenter.membership;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record MemberPackageRequest(
        @NotNull Integer packageId,
        LocalDate startDate,
        Integer durationDays,
        Boolean isRenewal,
        String paymentMethod,
        String notes
) {
    public MemberPackageRequest(Integer packageId, LocalDate startDate, Integer durationDays) {
        this(packageId, startDate, durationDays, false, null, null);
    }
}
