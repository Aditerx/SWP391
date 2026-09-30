package com.sportscenter.report;

import com.sportscenter.invoice.Invoice;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final SportsClassRepository sportsClassRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final InvoiceRepository invoiceRepository;
    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<Invoice> allInvoices = invoiceRepository.findAllWithDetails();
        BigDecimal totalRevenue = allInvoices.stream()
                .filter(i -> "Paid".equalsIgnoreCase(i.getPaymentStatus()))
                .map(Invoice::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalPaidInvoices = allInvoices.stream()
                .filter(i -> "Paid".equalsIgnoreCase(i.getPaymentStatus()))
                .count();

        List<MemberPackage> allSubs = memberPackageRepository.findAll();
        long activeSubscriptions = allSubs.stream()
                .filter(s -> "Active".equalsIgnoreCase(s.getStatus()) && (s.getEndDate() == null || !s.getEndDate().isBefore(LocalDate.now())))
                .count();

        long todaySessions = sessionRepository.findBySessionDateBetween(LocalDate.now(), LocalDate.now()).size();

        return new DashboardResponse(
                userRepository.count(),
                userRepository.countMembers(),
                sportsClassRepository.count(),
                membershipPackageRepository.count(),
                totalRevenue,
                totalPaidInvoices,
                activeSubscriptions,
                todaySessions
        );
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse revenueReport(LocalDate startDate, LocalDate endDate, String groupBy) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

        List<Invoice> allInvoices = invoiceRepository.findAllWithDetails();
        List<Invoice> invoices = allInvoices.stream()
                .filter(i -> "Paid".equalsIgnoreCase(i.getPaymentStatus()))
                .filter(i -> start == null || (i.getPaymentDate() != null && !i.getPaymentDate().isBefore(start)))
                .filter(i -> end == null || (i.getPaymentDate() != null && !i.getPaymentDate().isAfter(end)))
                .toList();

        BigDecimal totalRevenue = invoices.stream()
                .map(Invoice::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = invoices.size();
        BigDecimal avgValue = totalCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // By Package
        Map<String, List<Invoice>> byPackage = invoices.stream()
                .collect(Collectors.groupingBy(i -> i.getMembershipPackage() != null ? i.getMembershipPackage().getName() : "Khác / Dịch vụ"));

        List<RevenueReportResponse.RevenueByPackageItem> packageItems = new ArrayList<>();
        byPackage.forEach((pkgName, list) -> {
            Integer pkgId = list.get(0).getMembershipPackage() != null ? list.get(0).getMembershipPackage().getId() : null;
            BigDecimal sum = list.stream().map(Invoice::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            packageItems.add(new RevenueReportResponse.RevenueByPackageItem(pkgId, pkgName, list.size(), sum));
        });
        packageItems.sort((a, b) -> b.totalRevenue().compareTo(a.totalRevenue()));

        // By Method
        Map<String, List<Invoice>> byMethod = invoices.stream()
                .collect(Collectors.groupingBy(i -> i.getPaymentMethod() != null ? i.getPaymentMethod() : "Cash"));

        List<RevenueReportResponse.RevenueByMethodItem> methodItems = new ArrayList<>();
        byMethod.forEach((method, list) -> {
            BigDecimal sum = list.stream().map(Invoice::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            methodItems.add(new RevenueReportResponse.RevenueByMethodItem(method, list.size(), sum));
        });
        methodItems.sort((a, b) -> b.totalRevenue().compareTo(a.totalRevenue()));

        // Timeline
        boolean isDaily = "day".equalsIgnoreCase(groupBy);
        DateTimeFormatter formatter = isDaily ? DateTimeFormatter.ofPattern("yyyy-MM-dd") : DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, List<Invoice>> byPeriod = invoices.stream()
                .filter(i -> i.getPaymentDate() != null)
                .collect(Collectors.groupingBy(i -> i.getPaymentDate().format(formatter), TreeMap::new, Collectors.toList()));

        List<RevenueReportResponse.RevenueTimelineItem> timeline = new ArrayList<>();
        byPeriod.forEach((period, list) -> {
            BigDecimal sum = list.stream().map(Invoice::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            timeline.add(new RevenueReportResponse.RevenueTimelineItem(period, sum, list.size()));
        });

        // If timeline is empty, fill default monthly view
        if (timeline.isEmpty()) {
            timeline.add(new RevenueReportResponse.RevenueTimelineItem("2026-09", totalRevenue, totalCount));
        }

        return new RevenueReportResponse(
                totalRevenue,
                totalCount,
                avgValue,
                packageItems,
                methodItems,
                timeline
        );
    }

    @Transactional(readOnly = true)
    public MemberReportResponse memberReport() {
        List<Member> members = memberRepository.findAll();
        long totalMembers = members.size();

        List<MemberPackage> allSubs = memberPackageRepository.findAll();
        Set<Integer> activeMemberIds = allSubs.stream()
                .filter(s -> "Active".equalsIgnoreCase(s.getStatus()) && (s.getEndDate() == null || !s.getEndDate().isBefore(LocalDate.now())))
                .map(s -> s.getMember() != null ? s.getMember().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long activeCount = members.stream().filter(m -> activeMemberIds.contains(m.getUserId())).count();
        long expiredCount = totalMembers - activeCount;

        // Package distribution
        Map<String, Long> pkgDist = allSubs.stream()
                .filter(s -> "Active".equalsIgnoreCase(s.getStatus()) && s.getMembershipPackage() != null)
                .collect(Collectors.groupingBy(s -> s.getMembershipPackage().getName(), Collectors.counting()));

        List<MemberReportResponse.PackageDistributionItem> distribution = new ArrayList<>();
        pkgDist.forEach((pkgName, count) -> distribution.add(new MemberReportResponse.PackageDistributionItem(pkgName, count)));

        // Monthly growth
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> byMonth = members.stream()
                .filter(m -> m.getJoinDate() != null)
                .collect(Collectors.groupingBy(m -> m.getJoinDate().format(monthFormatter), TreeMap::new, Collectors.counting()));

        List<MemberReportResponse.MonthlyMemberGrowthItem> growth = new ArrayList<>();
        byMonth.forEach((month, count) -> growth.add(new MemberReportResponse.MonthlyMemberGrowthItem(month, count)));

        return new MemberReportResponse(
                totalMembers,
                activeCount,
                expiredCount,
                distribution,
                growth
        );
    }
}
