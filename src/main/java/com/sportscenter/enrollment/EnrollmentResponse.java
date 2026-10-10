package com.sportscenter.enrollment;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Integer id,
        Integer memberId,
        String memberName,
        String memberCode,
        String memberEmail,
        String memberPhone,
        Integer classId,
        String className,
        String subjectName,
        String coachName,
        LocalDateTime enrolledAt,
        String status,
        Integer invoiceId,
        String invoiceCode,
        String paymentStatus,
        String paymentUrl
) {
    public static EnrollmentResponse from(ClassEnrollment enrollment) {
        if (enrollment == null) return null;
        var member = enrollment.getMember();
        var sportsClass = enrollment.getSportsClass();
        return new EnrollmentResponse(
                enrollment.getId(),
                member != null ? member.getId() : null,
                member != null ? member.getFullName() : null,
                member != null ? "MB-" + (1000 + member.getId()) : null,
                member != null ? member.getEmail() : null,
                member != null ? member.getPhone() : null,
                sportsClass != null ? sportsClass.getId() : null,
                sportsClass != null ? sportsClass.getName() : null,
                sportsClass != null && sportsClass.getSubject() != null ? sportsClass.getSubject().getName() : null,
                sportsClass != null && sportsClass.getCoach() != null ? sportsClass.getCoach().getFullName() : null,
                enrollment.getEnrolledAt(),
                enrollment.getStatus(), null, null, null, null
        );
    }

    public static EnrollmentResponse from(ClassEnrollment enrollment, com.sportscenter.invoice.InvoiceOrderResponse order) {
        if (enrollment == null) return null;
        EnrollmentResponse base = from(enrollment);
        return new EnrollmentResponse(base.id(), base.memberId(), base.memberName(), base.memberCode(), base.memberEmail(),
                base.memberPhone(), base.classId(), base.className(), base.subjectName(), base.coachName(), base.enrolledAt(),
                base.status(), order.invoiceId(), order.invoiceCode(), order.status(), order.paymentUrl());
    }
}
