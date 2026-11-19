package com.sportscenter.sportclass;

import java.time.LocalDate;
import java.math.BigDecimal;

public record SportsClassResponse(
        Integer id,
        String name,
        Integer subjectId,
        String subjectName,
        Integer coachId,
        String coachName,
        Integer maxCapacity,
        BigDecimal tuitionFee,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer enrolledCount,
        String classType,
        Integer totalSessions,
        Integer minCapacity,
        java.time.LocalDateTime openingConfirmedAt,
        Integer openingConfirmedBy,
        int postponementCount,
        long lifecycleVersion,
        boolean pricingDraft,
        boolean changePending,
        Integer advancedRegistrationId,Integer memberId,String memberName,Integer subscriptionId,String advancedState,
        java.time.OffsetDateTime firstSessionAt, java.time.OffsetDateTime registrationDeadline,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true) Boolean registrationAllowed,String registrationBlockReason,
        java.time.OffsetDateTime openingConfirmationDueAt,java.time.OffsetDateTime openingConfirmationDeadline,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true) Boolean openingReasonRequired,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true) Boolean openingConfirmationAllowed,String openingConfirmationBlockReason,
        java.time.OffsetDateTime serverTime) {
    public record Actions(java.time.OffsetDateTime firstSessionAt, java.time.OffsetDateTime registrationDeadline,
            Boolean registrationAllowed,String registrationBlockReason,
            java.time.OffsetDateTime openingConfirmationDueAt,java.time.OffsetDateTime openingConfirmationDeadline,
            Boolean openingReasonRequired,Boolean openingConfirmationAllowed,String openingConfirmationBlockReason,
            java.time.OffsetDateTime serverTime) { }

    public SportsClassResponse(Integer id, String name, Integer subjectId, Integer coachId,
                               Integer maxCapacity, BigDecimal tuitionFee, LocalDate startDate,
                               LocalDate endDate, String status, Integer enrolledCount) {
        this(id, name, subjectId, null, coachId, null, maxCapacity, tuitionFee, startDate, endDate, status, enrolledCount, "Normal", 12, null, null, null, 0, 0, false, false,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null);
    }

    public static SportsClassResponse from(SportsClass sportsClass) {
        return from(sportsClass, null);
    }

    public static SportsClassResponse from(SportsClass sportsClass, Integer enrolledCount) {
        return from(sportsClass,enrolledCount,null);
    }
    public static SportsClassResponse from(SportsClass sportsClass, Integer enrolledCount, Actions actions) {
        if (sportsClass == null) return null;
        String subjectName = sportsClass.getSubject() != null ? sportsClass.getSubject().getName() : null;
        String coachName = sportsClass.getCoach() != null ? sportsClass.getCoach().getFullName() : null;
        return new SportsClassResponse(
                sportsClass.getId(),
                sportsClass.getName(),
                sportsClass.getSubject() == null ? null : sportsClass.getSubject().getId(),
                subjectName,
                sportsClass.getCoach() == null ? null : sportsClass.getCoach().getId(),
                coachName,
                sportsClass.getMaxCapacity(),
                sportsClass.getTuitionFee(),
                sportsClass.getStartDate(),
                sportsClass.getEndDate(),
                sportsClass.getStatus(),
                enrolledCount,
                sportsClass.getClassType(),
                sportsClass.getTotalSessions(), sportsClass.getMinCapacity(), sportsClass.getOpeningConfirmedAt(), sportsClass.getOpeningConfirmedBy(),
                sportsClass.getPostponementCount(), sportsClass.getLifecycleVersion(), sportsClass.isPricingDraft(),sportsClass.isChangePending(),null,null,null,null,null,
                actions==null?null:actions.firstSessionAt(),actions==null?null:actions.registrationDeadline(),
                actions==null?null:actions.registrationAllowed(),actions==null?null:actions.registrationBlockReason(),
                actions==null?null:actions.openingConfirmationDueAt(),actions==null?null:actions.openingConfirmationDeadline(),
                actions==null?null:actions.openingReasonRequired(),actions==null?null:actions.openingConfirmationAllowed(),actions==null?null:actions.openingConfirmationBlockReason(),actions==null?null:actions.serverTime());
    }

    public static SportsClassResponse advanced(SportsClass c,com.sportscenter.membership.AdvancedRegistration r,java.time.Clock clock) {
        var sub=r.getSubscription();
        String state=sub==null ? (r.getAcceptedAt()!=null&&!java.time.LocalDateTime.now(clock).isBefore(r.getAcceptedAt().plusHours(24))
                ? "ConfirmationExpired":"AwaitingConfirmation") : "Pending".equals(sub.getStatus())?"AwaitingPayment":sub.getStatus();
        String status="Active".equals(state)?"Ongoing":"Cancelled".equals(state)?"Cancelled":"Closed";
        return new SportsClassResponse(c.getId(),c.getName(),c.getSubject().getId(),c.getSubject().getName(),r.getCoach().getId(),r.getCoach().getFullName(),
                1,sub!=null&&sub.getPurchaseAmount()!=null?sub.getPurchaseAmount():c.getTuitionFee(),c.getStartDate(),c.getEndDate(),status,1,"Advanced",12,
                null,null,null,0,c.getLifecycleVersion(),false,false,r.getId(),r.getMember().getId(),r.getMember().getFullName(),sub==null?null:sub.getSubscriptionId(),state,
                null,null,null,null,null,null,null,null,null,com.sportscenter.training.SessionAttendancePolicy.now(clock).atZone(com.sportscenter.training.SessionAttendancePolicy.ZONE).toOffsetDateTime());
    }
}
