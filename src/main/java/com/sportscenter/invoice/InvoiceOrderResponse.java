package com.sportscenter.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceOrderResponse(
        Integer invoiceId,
        String invoiceCode,
        BigDecimal amount,
        String paymentMethod,
        String status,
        LocalDateTime expiresAt,
        String paymentUrl
) {
    public static InvoiceOrderResponse from(Invoice invoice, String paymentUrl) {
        return new InvoiceOrderResponse(invoice.getInvoiceId(), invoice.getInvoiceCode(), invoice.getAmount(),
                invoice.getPaymentMethod(), invoice.getPaymentStatus(), invoice.getExpiresAt(), paymentUrl);
    }
}
