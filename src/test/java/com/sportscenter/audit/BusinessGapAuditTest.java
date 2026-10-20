package com.sportscenter.audit;

import com.sportscenter.enrollment.*;
import com.sportscenter.invoice.*;
import com.sportscenter.membership.*;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.*;
import com.sportscenter.user.*;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Regression tests for the three business gaps found in the audit. */
class BusinessGapAuditTest {
    @Test void cancelledSubscriptionRequiresPaidInvoiceBeforeReactivation() {
        var packages=mock(MemberPackageRepository.class);
        var invoices=mock(InvoiceService.class);
        var service=new MemberPackageService(packages,mock(MembershipPackageRepository.class),mock(UserRepository.class),mock(AuditService.class),invoices);
        var mp=new MemberPackage(); mp.setSubscriptionId(9); mp.setStatus("Cancelled");
        // Supply DTO relations only; there is deliberately no Paid invoice.
        var user=new User(); user.setId(7); var pkg=new MembershipPackage(); pkg.setId(2);
        mp.setMember(user); mp.setMembershipPackage(pkg);
        when(packages.findById(9)).thenReturn(Optional.of(mp));
        doThrow(new com.sportscenter.common.exception.BusinessException("Invoice not Paid")).when(invoices).requirePaidSubscription(9);
        assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> service.updateStatus(9,"Active"));
        assertEquals("Cancelled",mp.getStatus()); verify(packages,never()).save(any());
    }
    @Test void memberIdAndOtherInvoiceFiltersAreAppliedTogether() {
        var repo=mock(InvoiceRepository.class);
        var service=new InvoiceService(repo,mock(UserRepository.class),mock(MembershipPackageRepository.class),mock(MemberPackageRepository.class),
            mock(SportsClassRepository.class),mock(ClassEnrollmentRepository.class),mock(AuditService.class),mock(PaymentProperties.class),mock(VnPayGateway.class));
        service.findAll(7,null,null,"Pending","Cash","INV-",null,null,null);
        verify(repo).searchInvoices(7,null,null,"Pending","Cash","INV-",null,null);
    }
    @Test void futureActivePackageCannotEnrollToday() {
        var er=mock(ClassEnrollmentRepository.class); var cr=mock(SportsClassRepository.class);
        var ur=mock(UserRepository.class); var mr=mock(MemberPackageRepository.class);
        var service=new EnrollmentService(er,cr,ur,mr,mock(AuditService.class),mock(InvoiceService.class),
            Clock.systemDefaultZone(),mock(SessionRepository.class),mock(NotificationService.class),mock(MemberRepository.class));
        var role=new Role(); role.setName("Member");
        var member=new User(); member.setId(7); member.setStatus("Active"); member.setRole(role);
        when(ur.findByEmailIgnoreCase("audit@example.com")).thenReturn(Optional.of(member));
        when(ur.findById(7)).thenReturn(Optional.of(member));
        SportsClass sampleClass = new SportsClass();
        sampleClass.setId(1);
        sampleClass.setStatus("Open");
        when(cr.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        var pkg=new MemberPackage(); pkg.setStatus("Active"); pkg.setStartDate(LocalDate.now().plusDays(30)); pkg.setEndDate(LocalDate.now().plusDays(60));
        when(mr.findByMemberId(7)).thenReturn(List.of(pkg));
        assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> service.enroll(1,null,"audit@example.com"));
    }
}
