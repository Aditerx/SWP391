package com.sportscenter.report;

public record DashboardResponse(
        long totalUsers,
        long totalMembers,
        long totalClasses,
        long totalPackages) {
}
