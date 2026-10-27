package com.sportscenter.membership;
import java.time.LocalDateTime;
public record AdvancedRegistrationResponse(Integer id, Integer memberId, Integer subjectId, Integer primaryCoachId,
        String status, LocalDateTime createdAt, Integer acceptedBy, LocalDateTime acceptedAt, String reason, Integer subscriptionId,
        String memberName,String subjectName,String primaryCoachName,String subscriptionStatus,Integer remainingCredit,LocalDateTime activatedAt,LocalDateTime expiresAt) {
    public static AdvancedRegistrationResponse from(AdvancedRegistration r) {
        return new AdvancedRegistrationResponse(r.getId(), r.getMember().getId(), r.getSubject().getId(), r.getCoach().getId(),
                r.getStatus(), r.getCreatedAt(), r.getAcceptedBy() == null ? null : r.getAcceptedBy().getId(), r.getAcceptedAt(),
                r.getReason(), r.getSubscription() == null ? null : r.getSubscription().getSubscriptionId(),
                r.getMember().getFullName(),r.getSubject().getName(),r.getCoach().getFullName(),r.getSubscription()==null?null:r.getSubscription().getStatus(),
                r.getSubscription()==null?null:r.getSubscription().getRemainingCredit(),r.getSubscription()==null?null:r.getSubscription().getActivatedAt(),r.getSubscription()==null?null:r.getSubscription().getExpiresAt());
    }
}
