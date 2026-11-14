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
        Integer advancedRegistrationId,Integer memberId,String memberName,Integer subscriptionId,String advancedState) {

    public SportsClassResponse(Integer id, String name, Integer subjectId, Integer coachId,
                               Integer maxCapacity, BigDecimal tuitionFee, LocalDate startDate,
                               LocalDate endDate, String status, Integer enrolledCount) {
        this(id, name, subjectId, null, coachId, null, maxCapacity, tuitionFee, startDate, endDate, status, enrolledCount, "Normal", 12, null, null, null, 0, 0, false, false,null,null,null,null,null);
    }

    public static SportsClassResponse from(SportsClass sportsClass) {
        return from(sportsClass, null);
    }

    public static SportsClassResponse from(SportsClass sportsClass, Integer enrolledCount) {
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
                sportsClass.getPostponementCount(), sportsClass.getLifecycleVersion(), sportsClass.isPricingDraft(),sportsClass.isChangePending(),null,null,null,null,null);
    }

    public static SportsClassResponse advanced(SportsClass c,com.sportscenter.membership.AdvancedRegistration r,java.time.Clock clock) {
        var sub=r.getSubscription();
        String state=sub==null ? (r.getAcceptedAt()!=null&&!java.time.LocalDateTime.now(clock).isBefore(r.getAcceptedAt().plusHours(24))
                ? "ConfirmationExpired":"AwaitingConfirmation") : "Pending".equals(sub.getStatus())?"AwaitingPayment":sub.getStatus();
        String status="Active".equals(state)?"Ongoing":"Cancelled".equals(state)?"Cancelled":"Closed";
        return new SportsClassResponse(c.getId(),c.getName(),c.getSubject().getId(),c.getSubject().getName(),r.getCoach().getId(),r.getCoach().getFullName(),
                1,sub!=null&&sub.getPurchaseAmount()!=null?sub.getPurchaseAmount():c.getTuitionFee(),c.getStartDate(),c.getEndDate(),status,1,"Advanced",12,
                null,null,null,0,c.getLifecycleVersion(),false,false,r.getId(),r.getMember().getId(),r.getMember().getFullName(),sub==null?null:sub.getSubscriptionId(),state);
    }
}
