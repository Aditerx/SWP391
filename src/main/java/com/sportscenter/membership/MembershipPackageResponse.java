package com.sportscenter.membership;

import java.math.BigDecimal;

public record MembershipPackageResponse(
        Integer id,
        String name,
        Integer durationDays,
        BigDecimal price,
        String benefits,
        String status) {

    static MembershipPackageResponse from(MembershipPackage membershipPackage) {
        return new MembershipPackageResponse(
                membershipPackage.getId(),
                membershipPackage.getName(),
                membershipPackage.getDurationDays(),
                membershipPackage.getPrice(),
                membershipPackage.getBenefits(),
                membershipPackage.getStatus());
    }
}
