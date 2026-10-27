package com.sportscenter.session;
import com.sportscenter.membership.*;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.user.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CenterIncidentRecoveryTest {
    @Test void completedEntitlementRestoresOnlyOneDeferredOriginalCreditAfterApprovedMinimumExtension(){
        Clock clock=Clock.fixed(Instant.parse("2026-10-27T01:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime now=LocalDateTime.now(clock);
        User manager=new User();manager.setId(10);manager.setCenterId(1);var role=new Role();role.setName("CenterManager");manager.setRole(role);
        User member=new User();member.setId(20);member.setCenterId(1);
        MemberPackage sub=new MemberPackage();sub.setSubscriptionId(1);sub.setMember(member);sub.setSubject(new com.sportscenter.subject.Subject());sub.setStatus("Completed");sub.setRemainingCredit(0);sub.setDeferredCreditReturn(1);
        sub.setActivatedAt(now.minusDays(50));sub.setExpiresAt(sub.getActivatedAt().plusDays(45).plusHours(1));
        Session session=new Session();session.setId(2);session.setMemberPackage(sub);
        CenterIncident incident=new CenterIncident();incident.setId(3);incident.setSession(session);incident.setStatus("Resolved");incident.setOriginalDeadline(sub.getActivatedAt().plusDays(45));incident.setStartedAt(incident.getOriginalDeadline().minusDays(1));incident.setEndedAt(incident.getStartedAt().plusHours(1));
        var incidents=mock(CenterIncidentRepository.class);when(incidents.findById(3)).thenReturn(Optional.of(incident));
        var invoices=mock(InvoiceRepository.class);when(invoices.existsBySubscriptionSubscriptionIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(1,"Paid")).thenReturn(true);
        var pauses=mock(SubscriptionPauseRepository.class);SubscriptionPause pause=new SubscriptionPause();pause.setStartsAt(incident.getStartedAt());pause.setEndsAt(incident.getEndedAt());when(pauses.findBySubscriptionSubscriptionId(1)).thenReturn(List.of(pause));
        var policy=new EntitlementTimePolicy(mock(HolidayRepository.class),pauses);
        var service=new CenterIncidentService(incidents,mock(SessionRepository.class),mock(MemberPackageRepository.class),pauses,policy,
                mock(com.sportscenter.enrollment.ClassEnrollmentRepository.class),mock(ScheduleWriteLock.class),mock(com.sportscenter.common.ActorPolicy.class),
                mock(com.sportscenter.audit.AuditService.class),clock,mock(org.springframework.context.ApplicationEventPublisher.class),invoices);
        LocalDateTime target=now.plusDays(2).toLocalDate().atTime(7,30);
        service.approveMakeupEnd(3,sub,target,"Minimum for verified replacement session",manager);
        assertEquals("Active",sub.getStatus());assertEquals(1,sub.getRemainingCredit());assertEquals(0,sub.getDeferredCreditReturn());assertEquals(target,sub.getExpiresAt());
        long extra=sub.getApprovedExtraSeconds();service.approveMakeupEnd(3,sub,target,"Idempotent retry",manager);
        assertEquals(1,sub.getRemainingCredit());assertEquals(extra,sub.getApprovedExtraSeconds());assertEquals(now.minusDays(50),sub.getActivatedAt());
    }
}
