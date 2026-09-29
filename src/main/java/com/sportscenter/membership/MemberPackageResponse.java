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
        String status
) {
    public static MemberPackageResponse from(MemberPackage mp) {
        return new MemberPackageResponse(
                mp.getSubscriptionId(),
                mp.getMember() != null ? mp.getMember().getId() : null,
                mp.getMember() != null ? mp.getMember().getFullName() : null,
                mp.getMembershipPackage() != null ? mp.getMembershipPackage().getId() : null,
                mp.getMembershipPackage() != null ? mp.getMembershipPackage().getName() : null,
                mp.getMembershipPackage() != null ? mp.getMembershipPackage().getPrice() : BigDecimal.ZERO,
                mp.getStartDate(),
                mp.getEndDate(),
                mp.getStatus() != null ? mp.getStatus() : "Active"
        );
    }
}
