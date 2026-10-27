package com.sportscenter.notification;

import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.subject.Subject;
import com.sportscenter.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotificationScheduleJobTest {
    private SessionRepository sessionRepository;
    private ClassEnrollmentRepository enrollmentRepository;
    private MemberPackageRepository memberPackageRepository;
    private ApplicationEventPublisher publisher;
    private NotificationScheduleJob job;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        enrollmentRepository = mock(ClassEnrollmentRepository.class);
        memberPackageRepository = mock(MemberPackageRepository.class);
        publisher = mock(ApplicationEventPublisher.class);
        job = new NotificationScheduleJob(sessionRepository, enrollmentRepository, memberPackageRepository, publisher, clock);
    }

    @Test
    void reminderUses24HourWindowAndStablePerRecipientDedupeKeys() {
        Session session = session(90, "Scheduled", LocalDate.of(2026, 10, 23), LocalTime.of(8, 5));
        session.setBookedMember(user(7));
        session.setCoach(user(8));
        when(sessionRepository.findScheduledBetweenDates(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23)))
                .thenReturn(List.of(session));

        job.sendSessionReminders();
        job.sendSessionReminders();

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(publisher, times(4)).publishEvent(captor.capture());
        assertEquals(List.of("SESSION_REMINDER:90:8:24h", "SESSION_REMINDER:90:7:24h",
                        "SESSION_REMINDER:90:8:24h", "SESSION_REMINDER:90:7:24h"),
                captor.getAllValues().stream().map(NotificationEvent::dedupeKey).toList());
    }

    @Test
    void cancelledOrPastSessionIsSkipped() {
        Session cancelled = session(90, "Cancelled", LocalDate.of(2026, 10, 23), LocalTime.of(8, 5));
        Session past = session(91, "Scheduled", LocalDate.of(2026, 10, 22), LocalTime.of(8, 0));
        cancelled.setBookedMember(user(7));
        past.setBookedMember(user(7));
        when(sessionRepository.findScheduledBetweenDates(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 23)))
                .thenReturn(List.of(cancelled, past));

        job.sendSessionReminders();

        verifyNoInteractions(publisher);
    }

    @Test
    void packageReminderTargetsThreeDaysIncludingBoundary() {
        MemberPackage memberPackage = new MemberPackage();
        memberPackage.setSubscriptionId(44);
        memberPackage.setMember(user(7));
        memberPackage.setEndDate(LocalDate.of(2026, 10, 25));
        MembershipPackage advanced = new MembershipPackage();
        advanced.setName("Gói 1:1");
        advanced.setPackageType("Advanced");
        memberPackage.setMembershipPackage(advanced);
        when(memberPackageRepository.findActiveAdvancedExpiringBetween(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 25)))
                .thenReturn(List.of(memberPackage));

        job.notifyPackagesExpiringSoon();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        NotificationEvent event = (NotificationEvent) captor.getValue();
        assertEquals(NotificationType.PACKAGE_EXPIRING_SOON, event.type());
        assertEquals("PACKAGE_EXPIRING_SOON:44:7:2026-10-25", event.dedupeKey());
    }

    private Session session(int id, String status, LocalDate date, LocalTime time) {
        Session session = new Session();
        session.setId(id); session.setStatus(status); session.setSessionDate(date);
        session.setStartTime(time); session.setEndTime(time.plusMinutes(90));
        SportsClass sportsClass = new SportsClass();
        Subject subject = new Subject(); subject.setName("Cầu lông");
        sportsClass.setSubject(subject);
        session.setSportsClass(sportsClass);
        return session;
    }

    private User user(int id) { User user = new User(); user.setId(id); user.setFullName("User " + id); return user; }
}
