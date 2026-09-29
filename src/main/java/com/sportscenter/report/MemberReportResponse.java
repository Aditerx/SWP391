package com.sportscenter.report;

import java.util.List;

public record MemberReportResponse(
        long totalMembers,
        long activeMembers,
        long expiredMembers,
        List<PackageDistributionItem> packageDistribution,
        List<MonthlyMemberGrowthItem> monthlyGrowth
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
