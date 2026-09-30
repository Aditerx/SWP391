package com.sportscenter.report;

import com.sportscenter.invoice.Invoice;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SportsClassRepository sportsClassRepository;

    @Mock
    private MembershipPackageRepository membershipPackageRepository;

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Dashboard returns correct counts and revenue with empty invoices")
    void dashboard_EmptyInvoices_Success() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countMembers()).thenReturn(5L);
        when(sportsClassRepository.count()).thenReturn(3L);
        when(membershipPackageRepository.count()).thenReturn(4L);
        when(invoiceRepository.findAllWithDetails()).thenReturn(Collections.emptyList());
        when(memberPackageRepository.findAll()).thenReturn(Collections.emptyList());
        when(sessionRepository.findBySessionDateBetween(any(), any())).thenReturn(Collections.emptyList());

        DashboardResponse res = reportService.dashboard();

        assertNotNull(res);
        assertEquals(10L, res.totalUsers());
        assertEquals(5L, res.totalMembers());
        assertEquals(3L, res.totalClasses());
        assertEquals(4L, res.totalPackages());
        assertEquals(BigDecimal.ZERO, res.totalRevenue());
        assertEquals(0L, res.totalInvoices());
    }

    @Test
    @DisplayName("Revenue report calculates total revenue and aggregates by method and package")
    void revenueReport_Success() {
        User member = new User();
        member.setId(1);

        MembershipPackage pkg = new MembershipPackage();
        pkg.setId(10);
        pkg.setName("Goi 1 Thang");

        Invoice inv1 = new Invoice();
        inv1.setInvoiceId(1);
        inv1.setMember(member);
        inv1.setMembershipPackage(pkg);
        inv1.setAmount(BigDecimal.valueOf(500000));
        inv1.setPaymentStatus("Paid");
        inv1.setPaymentMethod("Cash");
        inv1.setPaymentDate(LocalDateTime.now());

        Invoice inv2 = new Invoice();
        inv2.setInvoiceId(2);
        inv2.setMember(member);
        inv2.setAmount(BigDecimal.valueOf(1000000));
        inv2.setPaymentStatus("Pending");
        inv2.setPaymentMethod("BankTransfer");

        when(invoiceRepository.findAllWithDetails()).thenReturn(List.of(inv1, inv2));

        RevenueReportResponse res = reportService.revenueReport(LocalDate.now().minusDays(30), LocalDate.now().plusDays(1), "month");

        assertNotNull(res);
        assertEquals(BigDecimal.valueOf(500000), res.totalRevenue());
        assertEquals(1L, res.totalTransactions());
        assertEquals(1, res.revenueByPackage().size());
        assertEquals("Goi 1 Thang", res.revenueByPackage().get(0).packageName());
    }
}
