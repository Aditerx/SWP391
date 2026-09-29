package com.sportscenter.invoice;

import jakarta.validation.constraints.NotBlank;

public record InvoicePaymentRequest(
        @NotBlank(message = "paymentMethod is required")
        String paymentMethod,

        String gatewayTransactionRef,

        Integer receptionistId,

        String notes
) {
}
