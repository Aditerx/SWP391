package com.sportscenter.report;

import java.math.BigDecimal;
import java.util.List;

public record RevenueReportResponse(
        BigDecimal totalRevenue,
        long totalTransactions,
        BigDecimal averageTransactionValue,
        List<RevenueByPackageItem> revenueByPackage,
        List<RevenueByMethodItem> revenueByMethod,
        List<RevenueTimelineItem> timeline
) {
    public record RevenueByPackageItem(
            Integer packageId,
            String packageName,
            long count,
            BigDecimal totalRevenue
    ) {}

    public record RevenueByMethodItem(
            String method,
            long count,
            BigDecimal totalRevenue
    ) {}

    public record RevenueTimelineItem(
            String period,
            BigDecimal revenue,
            long count
    ) {}
}
