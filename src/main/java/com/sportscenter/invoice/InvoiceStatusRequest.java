package com.sportscenter.invoice;

import jakarta.validation.constraints.NotBlank;

public record InvoiceStatusRequest(
        @NotBlank(message = "status is required")
        String status
) {
}
