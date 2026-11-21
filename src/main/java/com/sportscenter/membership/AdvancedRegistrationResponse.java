package com.sportscenter.membership;
import java.time.LocalDateTime;
public record AdvancedRegistrationResponse(Integer id, Integer memberId, Integer subjectId, Integer primaryCoachId,
        String status, LocalDateTime createdAt, Integer acceptedBy, LocalDateTime acceptedAt, String reason, Integer subscriptionId,
        String memberName,String subjectName,String primaryCoachName,String subscriptionStatus,Integer remainingCredit,LocalDateTime activatedAt,LocalDateTime expiresAt,LocalDateTime confirmationDeadline,Integer classId,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true) Integer invoiceId,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true) String invoiceStatus,
        Boolean purchaseConfirmationAllowed,String purchaseConfirmationBlockReason,java.time.OffsetDateTime serverTime) {
    public static AdvancedRegistrationResponse from(AdvancedRegistration r) {
        return from(r,null,false,"REGISTRATION_OWNER_REQUIRED",null);
    }
    public static AdvancedRegistrationResponse from(AdvancedRegistration r,com.sportscenter.invoice.Invoice invoice,boolean allowed,String reason,java.time.Clock clock) {
        return new AdvancedRegistrationResponse(r.getId(), r.getMember().getId(), r.getSubject().getId(), r.getCoach().getId(),
                r.getStatus(), r.getCreatedAt(), r.getAcceptedBy() == null ? null : r.getAcceptedBy().getId(), r.getAcceptedAt(),
                r.getReason(), r.getSubscription() == null ? null : r.getSubscription().getSubscriptionId(),
                r.getMember().getFullName(),r.getSubject().getName(),r.getCoach().getFullName(),r.getSubscription()==null?null:r.getSubscription().getStatus(),
                r.getSubscription()==null?null:r.getSubscription().getRemainingCredit(),r.getSubscription()==null?null:r.getSubscription().getActivatedAt(),r.getSubscription()==null?null:r.getSubscription().getExpiresAt(),r.getAcceptedAt()==null?null:r.getAcceptedAt().plusHours(24),
                r.getSportsClass()==null?null:r.getSportsClass().getId(),
                invoice==null?null:invoice.getInvoiceId(),invoice==null?null:invoice.getPaymentStatus(),allowed,reason,
                clock==null?null:com.sportscenter.training.SessionAttendancePolicy.now(clock).atZone(com.sportscenter.training.SessionAttendancePolicy.ZONE).toOffsetDateTime());
    }
}
