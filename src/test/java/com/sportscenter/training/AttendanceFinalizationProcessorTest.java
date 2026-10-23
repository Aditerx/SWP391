package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.notification.NotificationEvent;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceFinalizationProcessorTest {
    @Mock SessionRepository sessionRepository;
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock AttendanceRepository attendanceRepository;
    @Mock MemberRepository memberRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock Clock clock;
    @InjectMocks AttendanceFinalizationProcessor processor;

    private Session session;
    private User memberUser;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        session = new Session();
        session.setId(90);
        session.setStatus("Completed");
        session.setSessionDate(LocalDate.of(2026, 10, 16));
        session.setStartTime(LocalTime.of(8, 0));
        session.setEndTime(LocalTime.of(9, 0));
        SportsClass sportsClass = new SportsClass();
        sportsClass.setId(12);
        session.setSportsClass(sportsClass);
        memberUser = new User();
        memberUser.setId(31);
    }

    @Test
    void process_CreatesAbsentForRegisteredMemberAndIsIdempotent() {
        ClassEnrollment registered = enrollment("Registered");
        Member profile = new Member(); profile.setUser(memberUser);
        when(sessionRepository.findByIdForUpdate(90)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassIdWithDetails(12)).thenReturn(List.of(registered));
        when(attendanceRepository.findBySessionIdAndMemberId(90, 31)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));
        when(attendanceRepository.countAbsencesForMemberInRange(31, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))).thenReturn(1L);
        when(memberRepository.findByIdForUpdate(31)).thenReturn(Optional.of(profile));

        processor.process(90);
        processor.process(90);

        verify(attendanceRepository, times(1)).save(argThat(a -> "Absent".equals(a.getState()) && a.getRecordedBy() == null));
        verify(sessionRepository, times(1)).save(session);
        org.mockito.ArgumentCaptor<Object> eventCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        NotificationEvent event = (NotificationEvent) eventCaptor.getValue();
        assertEquals("NO_SHOW_RECORDED", event.type().name());
        assertEquals(List.of(31), event.recipientIds());
        verifyNoInteractions(notificationService);
    }

    @Test
    void process_DoesNotOverwriteExcusedAttendanceOrCreateTrainingResult() {
        ClassEnrollment registered = enrollment("Registered");
        Attendance excused = new Attendance(); excused.setState("Excused");
        when(sessionRepository.findByIdForUpdate(90)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassIdWithDetails(12)).thenReturn(List.of(registered));
        when(attendanceRepository.findBySessionIdAndMemberId(90, 31)).thenReturn(Optional.of(excused));

        processor.process(90);

        assertEquals("Excused", excused.getState());
        verify(attendanceRepository, never()).save(any(Attendance.class));
        verify(memberRepository, never()).findByIdForUpdate(anyInt());
    }

    @Test
    void process_OnlyRegisteredEnrollmentIsFinalized() {
        ClassEnrollment pending = enrollment("Pending");
        ClassEnrollment waitlisted = enrollment("Waitlisted");
        when(sessionRepository.findByIdForUpdate(90)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassIdWithDetails(12)).thenReturn(List.of(pending, waitlisted));

        processor.process(90);

        verifyNoInteractions(attendanceRepository);
        verify(sessionRepository).save(argThat(s -> s.isAttendanceFinalized()));
    }

    @Test
    void process_ThirdMonthlyAbsenceLocksBookingForSevenDays() {
        ClassEnrollment registered = enrollment("Registered");
        Member profile = new Member(); profile.setUser(memberUser);
        when(sessionRepository.findByIdForUpdate(90)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassIdWithDetails(12)).thenReturn(List.of(registered));
        when(attendanceRepository.findBySessionIdAndMemberId(90, 31)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));
        when(attendanceRepository.countAbsencesForMemberInRange(31, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))).thenReturn(3L);
        when(memberRepository.findByIdForUpdate(31)).thenReturn(Optional.of(profile));

        processor.process(90);

        LocalDateTime expectedUntil = LocalDateTime.of(2026, 10, 16, 10, 0).plusDays(7);
        assertEquals(expectedUntil, profile.getBookingBlockedUntil());
        verify(auditService).log(isNull(), eq("BOOKING_BLOCK_APPLIED"), eq("members"), eq(31), contains("after 3 absences"));
        verify(notificationService).send(eq(31), eq("Class booking restricted"), anyString(), eq("System"));
    }

    @Test
    void process_TwoAbsencesAndOneExcusedDoNotTriggerBookingBlock() {
        ClassEnrollment registered = enrollment("Registered");
        Member profile = new Member(); profile.setUser(memberUser);
        when(sessionRepository.findByIdForUpdate(90)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassIdWithDetails(12)).thenReturn(List.of(registered));
        when(attendanceRepository.findBySessionIdAndMemberId(90, 31)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));
        when(attendanceRepository.countAbsencesForMemberInRange(31, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))).thenReturn(2L);
        when(memberRepository.findByIdForUpdate(31)).thenReturn(Optional.of(profile));

        processor.process(90);

        assertNull(profile.getBookingBlockedUntil());
        verify(auditService, never()).log(any(), eq("BOOKING_BLOCK_APPLIED"), anyString(), any(), anyString());
        verify(attendanceRepository).countAbsencesForMemberInRange(31, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
    }

    @Test
    void process_DoesNotExtendExistingBookingBlock() {
        ClassEnrollment registered = enrollment("Registered");
        Member profile = new Member(); profile.setUser(memberUser);
        LocalDateTime oldUntil = LocalDateTime.of(2026, 10, 18, 10, 0);
        profile.setBookingBlockedUntil(oldUntil);
        when(sessionRepository.findByIdForUpdate(90)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByClassIdWithDetails(12)).thenReturn(List.of(registered));
        when(attendanceRepository.findBySessionIdAndMemberId(90, 31)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));
        when(attendanceRepository.countAbsencesForMemberInRange(31, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))).thenReturn(4L);
        when(memberRepository.findByIdForUpdate(31)).thenReturn(Optional.of(profile));

        processor.process(90);

        assertEquals(oldUntil, profile.getBookingBlockedUntil());
        verify(auditService, never()).log(any(), eq("BOOKING_BLOCK_APPLIED"), anyString(), any(), anyString());
    }

    private ClassEnrollment enrollment(String status) {
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setMember(memberUser);
        enrollment.setStatus(status);
        return enrollment;
    }
}
