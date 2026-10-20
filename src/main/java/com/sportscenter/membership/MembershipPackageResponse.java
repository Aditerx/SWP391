package com.sportscenter.membership;

import java.math.BigDecimal;

public record MembershipPackageResponse(
        Integer id,
        String name,
        Integer durationDays,
        BigDecimal price,
        String benefits,
        String status,
        String packageType,
        Integer totalCredit) {

    public MembershipPackageResponse(Integer id, String name, Integer durationDays,
                                     BigDecimal price, String benefits, String status) {
        this(id, name, durationDays, price, benefits, status, "Normal", 12);
    }

    public static MembershipPackageResponse from(MembershipPackage membershipPackage) {
        if (membershipPackage == null) return null;
        return new MembershipPackageResponse(
                membershipPackage.getId(),
                membershipPackage.getName(),
                membershipPackage.getDurationDays(),
                membershipPackage.getPrice(),
                membershipPackage.getBenefits(),
                membershipPackage.getStatus(),
                membershipPackage.getPackageType(),
                membershipPackage.getTotalCredit());
    }
}
