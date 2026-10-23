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

    @Test void registeringAtCounterWithPackageCreatesPendingPaymentInsteadOfGrantingAccess() {
        var role = new Role(); role.setName("Member"); when(roles.findAll()).thenReturn(List.of(role));
        when(centers.centerForNewAccount(1,false)).thenReturn(1);
        when(passwords.generate()).thenReturn("test-temporary-password"); when(passwords.hash(anyString())).thenReturn("unused-test-hash");
        when(users.save(any())).thenAnswer(c -> { User u = c.getArgument(0); u.setId(7); return u; });
        when(members.save(any())).thenAnswer(c -> c.getArgument(0));
        var pkg = new MembershipPackage(); pkg.setId(2); pkg.setPrice(BigDecimal.TEN);
        var sub = new MemberPackage(); sub.setSubscriptionId(9); sub.setMembershipPackage(pkg); sub.setStatus("Pending"); sub.setRemainingCredit(0);
        when(subscriptions.findById(9)).thenReturn(Optional.of(sub));
        when(subscriptionService.subscribe(eq(7),any(MemberPackageRequest.class))).thenAnswer(call -> new SubscriptionOrderResponse(9,5,"TEST-INVOICE",BigDecimal.TEN,"Cash","Pending",LocalDateTime.now(clock).plusMinutes(5),null));
        var response = service.createMember(new MemberRequest("Test member","new@test.invalid",null,null,2,null,null,null,null,null,"Active",1));
        assertEquals("pending",response.membershipStatus()); assertEquals(BigDecimal.ZERO,response.totalSpent());
        verify(subscriptionService).subscribe(eq(7),argThat(r -> r.packageId().equals(2) && "Cash".equals(r.paymentMethod())));
        verify(subscriptions,never()).save(any());
        assertEquals(0,sub.getRemainingCredit());
    }

    @Test void totalSpentUsesPaidInvoiceSnapshotAndIgnoresPendingOrders() {
        var member = new User(); member.setId(7); member.setFullName("Test member");
        when(users.findById(7)).thenReturn(Optional.of(member));
        var pkg = new MembershipPackage(); pkg.setPrice(new BigDecimal("9999"));
        var sub = new MemberPackage(); sub.setMembershipPackage(pkg); sub.setMember(member); sub.setStatus("Active");
        when(subscriptions.findByMemberId(7)).thenReturn(List.of(sub));
        var paid = new com.sportscenter.invoice.Invoice(); paid.setPaymentStatus("Paid"); paid.setAmount(BigDecimal.TEN);
        var pending = new com.sportscenter.invoice.Invoice(); pending.setPaymentStatus("Pending"); pending.setAmount(new BigDecimal("1000"));
        when(invoices.findByMemberId(7)).thenReturn(List.of(paid,pending));
        assertEquals(BigDecimal.TEN,service.findById(7).totalSpent());
    }
}
