package com.sportscenter.invoice;

/** Query status is separate from the stored invoice paymentStatus. */
public record VnPayVerificationResponse(
        String verificationStatus, String message, int retryAfterSeconds, InvoiceResponse invoice) { }
