package com.sportscenter.report;
import com.sportscenter.common.ActorPolicy;
import com.sportscenter.invoice.*;
import com.sportscenter.membership.*;
import com.sportscenter.user.*;
import com.sportscenter.session.Session;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Spy Clock clock=Clock.fixed(Instant.parse("2026-10-27T01:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
    @Mock ActorPolicy actors;
    @Mock ReportDataSource source;
    @InjectMocks ReportService service;
    User manager;
    Authentication auth=new UsernamePasswordAuthenticationToken("manager","",List.of(new SimpleGrantedAuthority("ROLE_CENTERMANAGER"),new SimpleGrantedAuthority("VIEW_REPORTS")));
    @BeforeEach void actor(){manager=new User();manager.setId(9);manager.setCenterId(1);var role=new Role();role.setName("CenterManager");manager.setRole(role);lenient().when(actors.actor(auth)).thenReturn(manager);}
    ReportDataSource.Data data(List<Invoice> invoices,List<PaymentResolution> resolutions){return new ReportDataSource.Data(List.of(manager),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),invoices,resolutions);}
    Invoice paid(int id,String amount,LocalDateTime date){var i=new Invoice();i.setInvoiceId(id);i.setAmount(new BigDecimal(amount));i.setPaymentStatus("Paid");i.setPaymentDate(date);i.setPaymentMethod("Cash");return i;}
    @Test void dashboardEmptyInvoicesRetainsCountsAndReturnsAllThreeMoneyFields(){
        var d=data(List.of(),List.of());when(source.load(1)).thenReturn(d);
        var result=service.dashboard(auth);assertEquals(1,result.totalUsers());assertEquals(0,result.totalInvoices());
        assertEquals(BigDecimal.ZERO,result.totalRevenue());assertEquals(BigDecimal.ZERO,result.moneyReceived());assertEquals(BigDecimal.ZERO,result.moneyRefunded());assertEquals(BigDecimal.ZERO,result.netReceived());verify(source).load(1);
    }
    @Test void revenueReportAggregatesPaidOnlyAndRetainsLegacyGrossMeaning(){
        var paid=paid(1,"500000",LocalDateTime.now(clock));var pkg=new MembershipPackage();pkg.setId(1);pkg.setName("Monthly");paid.setMembershipPackage(pkg);
        var pending=new Invoice();pending.setPaymentStatus("Pending");pending.setAmount(new BigDecimal("1000000"));when(source.load(1)).thenReturn(data(List.of(paid,pending),List.of()));
        var result=service.revenueReport(LocalDate.now(clock).minusDays(30),LocalDate.now(clock).plusDays(1),"month",auth);
        assertEquals(new BigDecimal("500000"),result.totalRevenue());assertEquals(new BigDecimal("500000"),result.moneyReceived());assertEquals(1,result.totalTransactions());assertEquals("Monthly",result.revenueByPackage().get(0).packageName());
    }
    @Test void emptyRevenueDoesNotInventATimelinePeriod(){
        when(source.load(1)).thenReturn(data(List.of(),List.of()));var result=service.revenueReport(LocalDate.of(2026,1,1),LocalDate.of(2026,1,31),"month",auth);assertTrue(result.timeline().isEmpty());assertEquals(BigDecimal.ZERO,result.netReceived());
    }
    @Test void refundUsesActualExecutionPeriodAndApprovalAloneIsNotMoneyReturned(){
        var receipt=paid(1,"3000000",LocalDateTime.of(2026,9,30,23,59,59,999999000));
        PaymentResolution refund=new PaymentResolution();refund.setType("Refund");refund.setAmount(receipt.getAmount());refund.setStatus("Approved");refund.setPerformedAt(LocalDateTime.of(2026,10,1,8,0));
        when(source.load(1)).thenReturn(data(List.of(receipt),List.of(refund)));
        assertEquals(BigDecimal.ZERO,service.revenueReport(LocalDate.of(2026,10,1),LocalDate.of(2026,10,31),"month",auth).moneyRefunded());
        refund.setStatus("Completed");var september=service.revenueReport(LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),"month",auth);var october=service.revenueReport(LocalDate.of(2026,10,1),LocalDate.of(2026,10,31),"month",auth);
        assertEquals(new BigDecimal("3000000"),september.moneyReceived());assertEquals(BigDecimal.ZERO,september.moneyRefunded());assertEquals(new BigDecimal("-3000000"),october.netReceived());
    }
    @Test void correctedCashIsNeverReportedAsActualReceiptOrFakeRefund(){
        var receipt=paid(1,"3000000",LocalDateTime.of(2026,9,30,8,0));receipt.setCashConfirmationReversed(true);
        var correction=new PaymentResolution();correction.setType("CashCorrection");correction.setStatus("Completed");correction.setAmount(receipt.getAmount());correction.setCompletedAt(LocalDateTime.of(2026,10,1,8,0));
        when(source.load(1)).thenReturn(data(List.of(receipt),List.of(correction)));
        var result=service.revenueReport(null,null,"month",auth);assertEquals(new BigDecimal("3000000"),result.totalRevenue());assertEquals(BigDecimal.ZERO,result.moneyReceived());assertEquals(BigDecimal.ZERO,result.moneyRefunded());assertEquals(new BigDecimal("3000000"),result.cashCorrectionsRecorded());
    }
    @Test void zeroCreditWithBookedSessionStillHasRightsAndNewMemberIsNotExpired(){
        User one=new User();one.setId(1);User two=new User();two.setId(2);Member a=new Member();a.setUserId(1);Member b=new Member();b.setUserId(2);
        var s=new MemberPackage();s.setSubscriptionId(1);s.setMember(one);s.setSubject(new com.sportscenter.subject.Subject());s.setPrimaryCoach(manager);s.setActivatedAt(LocalDateTime.now(clock).minusDays(1));s.setExpiresAt(LocalDateTime.now(clock).plusDays(40));s.setStatus("Active");s.setRemainingCredit(0);
        var invoice=paid(1,"3000000",LocalDateTime.now(clock));invoice.setSubscription(s);
        var booked=new Session();booked.setMemberPackage(s);booked.setSessionDate(LocalDate.now(clock).plusDays(1));booked.setStartTime(LocalTime.of(6,0));booked.setEndTime(LocalTime.of(7,30));
        var d=new ReportDataSource.Data(List.of(one,two),List.of(a,b),List.of(),List.of(),List.of(s),List.of(),List.of(booked),List.of(invoice),List.of());when(source.load(1)).thenReturn(d);
        var result=service.memberReport(auth);assertEquals(1,result.activeMembers());assertEquals(1,result.neverEntitledMembers());assertEquals(0,result.expiredMembers());
    }
    @Test void coachWithReportAuthorityStillCannotReadManagementMoney(){
        manager.getRole().setName("Coach");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.dashboard(auth));verifyNoInteractions(source);
    }
}
