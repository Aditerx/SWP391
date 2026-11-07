package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.auth.*;
import com.sportscenter.center.CenterContext;
import com.sportscenter.membership.*;
import com.sportscenter.user.dto.MemberRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MemberRegistrationPaymentTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock MemberRepository members;
    @Mock MemberPackageRepository subscriptions;
    @Mock MemberPackageService subscriptionService;
    @Mock com.sportscenter.invoice.InvoiceRepository invoices;
    @Mock AuditService audit;
    @Mock CenterContext centers;
    @Mock EmailService email;
    @Mock TemporaryPasswordGenerator passwords;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-10-23T01:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
    @InjectMocks MemberService service;

    @Test void registeringAtCounterWithRetiredPackageIsRejectedBeforeCreatingAccount() {
        var request = new MemberRequest("Test member","new@test.invalid",null,null,2,null,null,null,null,null,"Active",1);
        var error = assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> service.createMember(request));
        assertTrue(error.getMessage().startsWith("LEGACY_MEMBERSHIP_RETIRED:"));
        verifyNoInteractions(users,members,subscriptionService,email,passwords);
    }
    @Test void totalSpentUsesPaidInvoiceSnapshotAndIgnoresPendingOrders() {
        var member = new User(); member.setId(7); member.setFullName("Test member");
        var role = new Role(); role.setName("Member"); member.setRole(role); member.setStatus("Active"); member.setCenterId(1);
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("member@test.invalid", "n/a", List.of()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        when(users.findByEmailIgnoreCase("member@test.invalid")).thenReturn(Optional.of(member));
        when(users.findById(7)).thenReturn(Optional.of(member));
        var pkg = new MembershipPackage(); pkg.setPrice(new BigDecimal("9999"));
        var sub = new MemberPackage(); sub.setMembershipPackage(pkg); sub.setMember(member); sub.setStatus("Active");
        when(subscriptions.findByMemberId(7)).thenReturn(List.of(sub));
        var paid = new com.sportscenter.invoice.Invoice(); paid.setPaymentStatus("Paid"); paid.setAmount(BigDecimal.TEN);
        var pending = new com.sportscenter.invoice.Invoice(); pending.setPaymentStatus("Pending"); pending.setAmount(new BigDecimal("1000"));
        when(invoices.findByMemberId(7)).thenReturn(List.of(paid,pending));
        assertEquals(BigDecimal.TEN,service.findById(7).totalSpent());
    }
    @org.junit.jupiter.api.AfterEach void clearIdentity() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
}
