package com.sportscenter.membership;

import java.math.BigDecimal;

public record PublicMembershipPackageResponse(Integer id, String name, BigDecimal price,
                                              Integer durationDays, String benefits) {
    public static PublicMembershipPackageResponse from(MembershipPackage item) {
        return new PublicMembershipPackageResponse(item.getId(), item.getName(), item.getPrice(),
                item.getDurationDays(), item.getBenefits());
    }
}
