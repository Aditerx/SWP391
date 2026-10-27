package com.sportscenter.report;

import java.util.List;

public record MemberReportResponse(
        long totalMembers,
        long activeMembers,
        long expiredMembers,
        List<PackageDistributionItem> packageDistribution,
        List<MonthlyMemberGrowthItem> monthlyGrowth,
        long neverEntitledMembers,
        long previouslyEntitledMembersWithoutRights,
        long expiredRegistrations,
        long completedRegistrations,
        long reconciliationRegistrations,
        long reconciliationMembers
) {
    public record PackageDistributionItem(
            String packageName,
            long memberCount
    ) {}

    public record MonthlyMemberGrowthItem(
            String month,
            long count
    ) {}
}
