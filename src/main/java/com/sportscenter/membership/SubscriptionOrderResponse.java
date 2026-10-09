package com.sportscenter.membership;

import com.sportscenter.invoice.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionOrderResponse(
        Integer subscriptionId,
        Integer invoiceId,
        String invoiceCode,
        BigDecimal amount,
        String paymentMethod,
        String status,
        LocalDateTime expiresAt,
        String paymentUrl
) {
    public static SubscriptionOrderResponse from(MemberPackage subscription, Invoice invoice, String paymentUrl) {
        return new SubscriptionOrderResponse(subscription.getSubscriptionId(), invoice.getInvoiceId(),
                invoice.getInvoiceCode(), invoice.getAmount(), invoice.getPaymentMethod(),
                invoice.getPaymentStatus(), invoice.getExpiresAt(), paymentUrl);
    }
}
