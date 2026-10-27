package com.sportscenter.membership;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberPackageResponse(
        Integer subscriptionId,
        Integer memberId,
        String memberName,
        Integer packageId,
        String packageName,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String packageType,
        Integer remainingCredit,
        Integer validityDays,
        Integer subjectId,
        Integer primaryCoachId,
        java.time.LocalDateTime activatedAt,
        java.time.LocalDateTime expiresAt
) {
    public MemberPackageResponse(
            Integer subscriptionId,
            Integer memberId,
            String memberName,
            Integer packageId,
            String packageName,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            String status
    ) {
        this(subscriptionId, memberId, memberName, packageId, packageName, price, startDate, endDate, status, "Normal", 0, 30,null,null,null,null);
    }

    public static MemberPackageResponse from(MemberPackage mp) {
        if (mp == null) return null;
        String pkgType = mp.getSubject() != null ? "Advanced" : (mp.getMembershipPackage() != null && mp.getMembershipPackage().getPackageType() != null)
                ? mp.getMembershipPackage().getPackageType() : "Normal";
        return new MemberPackageResponse(
                mp.getSubscriptionId(),
                mp.getMember() != null ? mp.getMember().getId() : null,
                mp.getMember() != null ? mp.getMember().getFullName() : null,
                mp.getMembershipPackage() != null ? mp.getMembershipPackage().getId() : null,
                mp.getSubject() != null ? mp.getSubject().getName() + " Advanced" : mp.getMembershipPackage() != null ? mp.getMembershipPackage().getName() : null,
                mp.getPurchaseAmount()!=null?mp.getPurchaseAmount():mp.getMembershipPackage() != null ? mp.getMembershipPackage().getPrice() : null,
                mp.getStartDate(),
                mp.getEndDate(),
                mp.getStatus() != null ? mp.getStatus() : "Active",
                pkgType,
                mp.getRemainingCredit() != null ? mp.getRemainingCredit() : 0,
                mp.getValidityDays() != null ? mp.getValidityDays() : 45,
                mp.getSubject() == null ? null : mp.getSubject().getId(),
                mp.getPrimaryCoach() == null ? null : mp.getPrimaryCoach().getId(),
                mp.getActivatedAt(), mp.getExpiresAt()
        );
    }
}
