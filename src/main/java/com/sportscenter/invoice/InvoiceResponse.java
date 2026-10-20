package com.sportscenter.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Integer invoiceId,
        Integer memberId,
        String memberName,
        String memberEmail,
        String memberPhone,
        Integer packageId,
        String packageName,
        Integer durationDays,
        Integer receptionistId,
        String receptionistName,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime paymentDate,
        String gatewayTransactionRef,
        String invoiceCode,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        Integer classId,
        String className
) {
    public static InvoiceResponse from(Invoice invoice) {
        if (invoice == null) return null;

        Integer memberId = invoice.getMember() != null ? invoice.getMember().getId() : null;
        String memberName = invoice.getMember() != null ? invoice.getMember().getFullName() : null;
        String memberEmail = invoice.getMember() != null ? invoice.getMember().getEmail() : null;
        String memberPhone = invoice.getMember() != null ? invoice.getMember().getPhone() : null;

        Integer packageId = invoice.getMembershipPackage() != null ? invoice.getMembershipPackage().getId() : null;
        String packageName = invoice.getMembershipPackage() != null ? invoice.getMembershipPackage().getName() : null;
        Integer durationDays = invoice.getMembershipPackage() != null ? invoice.getMembershipPackage().getDurationDays() : null;

        Integer receptionistId = invoice.getReceptionist() != null ? invoice.getReceptionist().getId() : null;
        String receptionistName = invoice.getReceptionist() != null ? invoice.getReceptionist().getFullName() : null;

        return new InvoiceResponse(
                invoice.getInvoiceId(),
                memberId,
                memberName,
                memberEmail,
                memberPhone,
                packageId,
                packageName,
                durationDays,
                receptionistId,
                receptionistName,
                invoice.getAmount(),
                invoice.getPaymentMethod(),
                invoice.getPaymentStatus(),
                invoice.getPaymentDate(),
                invoice.getGatewayTransactionRef(),
                invoice.getInvoiceCode(),
                invoice.getCreatedAt(),
                invoice.getExpiresAt(),
                invoice.getSportsClass() != null ? invoice.getSportsClass().getId() : null,
                invoice.getSportsClass() != null ? invoice.getSportsClass().getName() : null
        );
    }
}
