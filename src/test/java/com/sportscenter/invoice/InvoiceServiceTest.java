package com.sportscenter.invoice;

import com.sportscenter.audit.AuditService;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {
    @Mock InvoiceRepository invoiceRepository;
    @Mock UserRepository userRepository;
    @Mock MembershipPackageRepository membershipPackageRepository;
    @Mock MemberPackageRepository memberPackageRepository;
    @Mock SportsClassRepository sportsClassRepository;
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock AuditService auditService;
    @Mock PaymentProperties paymentProperties;
    @Mock VnPayGateway vnPayGateway;
    @InjectMocks InvoiceService invoiceService;

    @Test
    void createPackageOrderUsesPackagePriceAndPendingStatus() {
        User member = new User(); member.setId(7);
        MembershipPackage pkg = new MembershipPackage(); pkg.setId(2); pkg.setStatus("Active"); pkg.setPrice(new BigDecimal("250.00"));
        when(invoiceRepository.findByMemberId(7)).thenReturn(List.of());
        when(invoiceRepository.nextInvoiceCodeValue()).thenReturn(41L);
        when(paymentProperties.getExpireMinutes()).thenReturn(15);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceOrderResponse response = invoiceService.createPackageOrder(member, pkg, "Cash", null, "127.0.0.1");

        assertEquals(new BigDecimal("250.00"), response.amount());
        assertEquals("Pending", response.status());
        assertEquals("Cash", response.paymentMethod());
        assertTrue(response.invoiceCode().endsWith("000041"));
        assertNull(response.paymentUrl());
    }

    @Test
    void memberInvoiceListingIsScopedToAuthenticatedUser() {
        User member = new User(); member.setId(7); member.setEmail("member@example.com");
        var auth = new UsernamePasswordAuthenticationToken("member@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
        when(invoiceRepository.findByMemberId(7)).thenReturn(List.of());
        assertTrue(invoiceService.findAll(null, null, null, null, null, null, null, null, auth).isEmpty());
        verify(invoiceRepository).findByMemberId(7);
    }

    @Test
    void memberCannotOverrideInvoiceScope() {
        User member = new User(); member.setId(7); member.setEmail("member@example.com");
        var auth = new UsernamePasswordAuthenticationToken("member@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
        assertThrows(AccessDeniedException.class,
                () -> invoiceService.findAll(8, null, null, null, null, null, null, null, auth));
        verifyNoInteractions(invoiceRepository);
    }

    @Test
    void cashConfirmationActivatesSubscriptionAtomically() {
        User receptionist = new User(); receptionist.setId(5); receptionist.setEmail("desk@example.com");
        User member = new User(); member.setId(7);
        MembershipPackage pkg = new MembershipPackage(); pkg.setDurationDays(30);
        MemberPackage subscription = new MemberPackage(); subscription.setMember(member); subscription.setMembershipPackage(pkg);
        subscription.setStatus("Pending"); subscription.setStartDate(LocalDate.now()); subscription.setEndDate(LocalDate.now().plusDays(30));
        Invoice invoice = new Invoice(); invoice.setInvoiceId(22); invoice.setInvoiceCode("INV-2026-000022");
        invoice.setMember(member); invoice.setSubscription(subscription); invoice.setPaymentMethod("Cash"); invoice.setPaymentStatus("Pending");
        var auth = new UsernamePasswordAuthenticationToken("desk@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_RECEPTIONIST")));
        when(userRepository.findByEmailIgnoreCase("desk@example.com")).thenReturn(Optional.of(receptionist));
        when(invoiceRepository.findByIdForUpdate(22)).thenReturn(Optional.of(invoice));
        when(memberPackageRepository.findByMemberId(7)).thenReturn(List.of(subscription));

        InvoiceResponse response = invoiceService.confirmCash(22, auth);

        assertEquals("Paid", response.paymentStatus());
        assertEquals("Active", subscription.getStatus());
        assertNotNull(invoice.getPaymentDate());
        assertEquals(receptionist, invoice.getReceptionist());
        verify(auditService).log(eq(5), eq("PROCESS_PAYMENT"), eq("invoices"), eq(22), anyString());
    }

    @Test
    void verifiedVnPayCallbackIsIdempotentAndActivatesPackage() {
        User member = new User(); member.setId(7);
        MembershipPackage pkg = new MembershipPackage(); pkg.setDurationDays(30);
        MemberPackage subscription = new MemberPackage(); subscription.setMember(member); subscription.setMembershipPackage(pkg); subscription.setStatus("Pending");
        Invoice invoice = new Invoice(); invoice.setInvoiceId(23); invoice.setInvoiceCode("INV-2026-000023");
        invoice.setMember(member); invoice.setSubscription(subscription); invoice.setPaymentMethod("VNPay");
        invoice.setPaymentStatus("Pending"); invoice.setAmount(new BigDecimal("500.00"));
        Map<String, String> params = Map.of("vnp_TxnRef", "INV-2026-000023", "vnp_Amount", "50000",
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_TransactionNo", "gateway-1");
        when(vnPayGateway.isValid(params)).thenReturn(true);
        when(invoiceRepository.findByInvoiceCodeForUpdate("INV-2026-000023")).thenReturn(Optional.of(invoice));
        when(memberPackageRepository.findByMemberId(7)).thenReturn(List.of(subscription));

        var result = invoiceService.processVnpayResult(params);
        assertEquals("00", result.code());
        assertEquals("Paid", invoice.getPaymentStatus());
        assertEquals("Active", subscription.getStatus());
        assertEquals("gateway-1", invoice.getGatewayTransactionRef());

        when(invoiceRepository.findByInvoiceCodeForUpdate("INV-2026-000023")).thenReturn(Optional.of(invoice));
        assertEquals("02", invoiceService.processVnpayResult(params).code());
        verify(memberPackageRepository, times(1)).findByMemberId(7);
    }

    @Test
    void invalidVnPaySignatureDoesNotChangeInvoice() {
        Map<String, String> params = Map.of("vnp_TxnRef", "INV-2026-000023");
        when(vnPayGateway.isValid(params)).thenReturn(false);
        assertEquals("97", invoiceService.processVnpayResult(params).code());
        verifyNoInteractions(invoiceRepository);
    }

    @Test
    void expiredVnPayInvoiceKeepsClassSeatUntilVerifiedCallback() {
        User member = new User(); member.setId(9);
        com.sportscenter.sportclass.SportsClass sportsClass = new com.sportscenter.sportclass.SportsClass(); sportsClass.setId(4); sportsClass.setMaxCapacity(1);
        Invoice invoice = new Invoice(); invoice.setInvoiceId(24); invoice.setMember(member); invoice.setSportsClass(sportsClass);
        invoice.setPaymentMethod("VNPay"); invoice.setPaymentStatus("Pending"); invoice.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(invoiceRepository.findByIdForUpdate(24)).thenReturn(Optional.of(invoice));

        assertTrue(invoiceService.expireInvoice(24));

        assertEquals("Expired", invoice.getPaymentStatus());
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void lateSuccessfulVnPayCallbackRegistersClassUsingReservedSeat() {
        User member = new User(); member.setId(9);
        com.sportscenter.sportclass.SportsClass sportsClass = new com.sportscenter.sportclass.SportsClass(); sportsClass.setId(4); sportsClass.setMaxCapacity(1);
        com.sportscenter.enrollment.ClassEnrollment enrollment = new com.sportscenter.enrollment.ClassEnrollment(); enrollment.setStatus("Pending");
        Invoice invoice = new Invoice(); invoice.setInvoiceId(25); invoice.setInvoiceCode("INV-2026-000025"); invoice.setMember(member);
        invoice.setSportsClass(sportsClass); invoice.setPaymentMethod("VNPay"); invoice.setPaymentStatus("Expired");
        invoice.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(2)); invoice.setAmount(new BigDecimal("100.00"));
        Map<String, String> params = Map.of("vnp_TxnRef", "INV-2026-000025", "vnp_Amount", "10000",
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_TransactionNo", "gateway-class");
        when(vnPayGateway.isValid(params)).thenReturn(true);
        when(invoiceRepository.findByInvoiceCodeForUpdate("INV-2026-000025")).thenReturn(Optional.of(invoice));
        when(sportsClassRepository.findByIdForUpdate(4)).thenReturn(Optional.of(sportsClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(4, List.of("Registered", "Pending"))).thenReturn(1L);
        when(enrollmentRepository.findByMemberIdAndSportsClassId(9, 4)).thenReturn(Optional.of(enrollment));

        var result = invoiceService.processVnpayResult(params);

        assertEquals("00", result.code());
        assertEquals("Paid", invoice.getPaymentStatus());
        assertEquals("Registered", enrollment.getStatus());
        verify(auditService).log(isNull(), eq("PAID_AFTER_EXPIRY"), eq("invoices"), eq(25), anyString());
    }
}
