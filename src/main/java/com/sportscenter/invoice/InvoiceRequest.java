package com.sportscenter.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InvoiceRequest(
        @NotNull(message = "memberId is required")
        Integer memberId,

        Integer packageId,

        Integer receptionistId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        String paymentMethod,

        String paymentStatus,

        String gatewayTransactionRef,

        String notes
) {
}
