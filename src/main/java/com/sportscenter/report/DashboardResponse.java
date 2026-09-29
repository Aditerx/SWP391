package com.sportscenter.report;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalUsers,
        long totalMembers,
        long totalClasses,
        long totalPackages,
        BigDecimal totalRevenue,
        long totalInvoices,
        long activeSubscriptions,
        long todaySessions
) {
    public DashboardResponse(long totalUsers, long totalMembers, long totalClasses, long totalPackages) {
        this(totalUsers, totalMembers, totalClasses, totalPackages, BigDecimal.ZERO, 0L, 0L, 0L);
    }
}
