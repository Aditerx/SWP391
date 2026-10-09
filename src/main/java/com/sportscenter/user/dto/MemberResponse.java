package com.sportscenter.user.dto;

import com.sportscenter.membership.MemberPackage;
import com.sportscenter.user.Member;
import com.sportscenter.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MemberResponse(
        Integer id,
        String code,
        String name,
        String email,
        String phone,
        LocalDate joinDate,
        Integer currentPackageId,
        String currentPackageName,
        String membershipStatus,
        Integer primaryCoachId,
        String primaryCoachName,
        BigDecimal totalSpent,
        String goal,
        String healthNote,
        String status,
        Boolean emailSent,
        String warning
) {
    public static MemberResponse from(User user, Member member, MemberPackage currentPackage, BigDecimal totalSpent) {
        String code = "MB-" + (1000 + user.getId());
        LocalDate joinDate = member != null && member.getJoinDate() != null ? member.getJoinDate() : (user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : LocalDate.now());

        Integer pkgId = currentPackage != null && currentPackage.getMembershipPackage() != null
                ? currentPackage.getMembershipPackage().getId() : null;
        String pkgName = currentPackage != null && currentPackage.getMembershipPackage() != null
                ? currentPackage.getMembershipPackage().getName() : null;
        String mStatus = currentPackage != null && currentPackage.getStatus() != null
                ? currentPackage.getStatus().toLowerCase() : "active";

        return new MemberResponse(
                user.getId(),
                code,
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                joinDate,
                pkgId,
                pkgName,
                mStatus,
                null,
                null,
                totalSpent != null ? totalSpent : BigDecimal.ZERO,
                member != null ? member.getGoal() : null,
                member != null ? member.getHealthNote() : null,
                user.getStatus() != null ? user.getStatus().toLowerCase() : "active",
                null,
                null
        );
    }

    public MemberResponse withEmailDelivery(boolean sent, String warning) {
        return new MemberResponse(id, code, name, email, phone, joinDate, currentPackageId,
                currentPackageName, membershipStatus, primaryCoachId, primaryCoachName, totalSpent,
                goal, healthNote, status, sent, warning);
    }
}
