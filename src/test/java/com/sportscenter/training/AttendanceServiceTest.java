package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private TrainingResultRepository trainingResultRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private Clock clock;

    @Mock
    private ClassEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private User sampleMember;
    private User sampleReceptionist;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        sampleMember = new User();
        sampleMember.setId(7);
        sampleMember.setFullName("Hoàng Thị Oanh");

        sampleReceptionist = new User();
        sampleReceptionist.setId(5);
        sampleReceptionist.setEmail("staff@example.test");
        sampleReceptionist.setFullName("Đỗ Thị Mai");
    }

    @Test
    @DisplayName("Counter Check-In creates attendance record with CheckedIn state")
    void checkIn_Success() {
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(sampleReceptionist));

        Attendance saved = new Attendance();
        saved.setAttendanceId(50);
        saved.setMember(sampleMember);
        saved.setRecordedBy(sampleReceptionist);
        saved.setState("CheckedIn");

        when(attendanceRepository.save(any(Attendance.class))).thenReturn(saved);

        AttendanceResponse response = attendanceService.checkIn(7, "staff@example.test");

        assertNotNull(response);
        assertEquals(50, response.attendanceId());
        assertEquals("CheckedIn", response.state());
        assertEquals("Hoàng Thị Oanh", response.memberName());
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Attendance correction requires reason and updates status")
    void correctAttendance_Success() {
        Attendance current = new Attendance();
        current.setAttendanceId(60);
        current.setMember(sampleMember);
        current.setState("Absent");

        when(attendanceRepository.findById(60)).thenReturn(Optional.of(current));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceCorrectionRequest req = new AttendanceCorrectionRequest(60, "Present", "Học viên đến muộn có xin phép trước");
        when(userRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(sampleReceptionist));
        AttendanceResponse response = attendanceService.correctAttendance(req, "staff@example.test");

        assertNotNull(response);
        assertEquals("Present", response.state());
        verify(auditService).log(eq(5), eq("ATTENDANCE_CORRECTION"), eq("attendances"), eq(60), contains("Reason: Học viên đến muộn"));
    }

    @Test
    @DisplayName("Correcting attendance to Excused syncs the training result")
    void correctAttendance_Excused_SyncsTrainingResult() {
        Session session = new Session(); session.setId(15);
        Attendance current = new Attendance();
        current.setAttendanceId(61); current.setSession(session); current.setMember(sampleMember); current.setState("Absent");
        TrainingResult result = new TrainingResult(); result.setAttendanceStatus("Absent");
        when(userRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(sampleReceptionist));
        when(attendanceRepository.findById(61)).thenReturn(Optional.of(current));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));
        when(trainingResultRepository.findBySessionIdAndMemberId(15, 7)).thenReturn(Optional.of(result));
        when(trainingResultRepository.save(any(TrainingResult.class))).thenAnswer(call -> call.getArgument(0));

        AttendanceResponse response = attendanceService.correctAttendance(
                new AttendanceCorrectionRequest(61, "Excused", "Approved absence"), "staff@example.test");

        assertEquals("Excused", response.state());
        assertEquals("Excused", result.getAttendanceStatus());
        verify(auditService).log(eq(5), eq("ATTENDANCE_CORRECTION"), eq("attendances"), eq(61), contains("Approved absence"));
    }

    @Test
    @DisplayName("Member may excuse an upcoming registered session with notice")
    void requestAbsence_WithinNotice_SavesExcused() {
        sampleMember.setEmail("member@example.test");
        Session session = new Session();
        session.setId(88);
        session.setSessionDate(java.time.LocalDate.of(2026, 10, 16));
        session.setStartTime(java.time.LocalTime.of(13, 0));
        com.sportscenter.sportclass.SportsClass sportsClass = new com.sportscenter.sportclass.SportsClass();
        sportsClass.setId(3);
        session.setSportsClass(sportsClass);
        when(userRepository.findByEmailIgnoreCase("member@example.test")).thenReturn(Optional.of(sampleMember));
        when(sessionRepository.findById(88)).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(7, 3, "Registered")).thenReturn(true);
        when(attendanceRepository.findBySessionIdAndMemberId(88, 7)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));

        AttendanceResponse response = attendanceService.requestAbsence(88, "Medical appointment", "member@example.test");

        assertEquals("Excused", response.state());
        verify(attendanceRepository).save(argThat(a -> "Medical appointment".equals(a.getNote())));
        verify(trainingResultRepository, never()).save(any(TrainingResult.class));
    }

    @Test
    @DisplayName("Member absence request inside two hour window is rejected")
    void requestAbsence_TooLate_Throws() {
        sampleMember.setEmail("member@example.test");
        Session session = new Session();
        session.setId(89);
        session.setSessionDate(java.time.LocalDate.of(2026, 10, 16));
        session.setStartTime(java.time.LocalTime.of(9, 0));
        com.sportscenter.sportclass.SportsClass sportsClass = new com.sportscenter.sportclass.SportsClass();
        sportsClass.setId(3);
        session.setSportsClass(sportsClass);
        when(userRepository.findByEmailIgnoreCase("member@example.test")).thenReturn(Optional.of(sampleMember));
        when(sessionRepository.findById(89)).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(7, 3, "Registered")).thenReturn(true);

        assertThrows(com.sportscenter.common.exception.BusinessException.class,
                () -> attendanceService.requestAbsence(89, "Reason", "member@example.test"));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Pending enrollment cannot request an excused absence")
    void requestAbsence_PendingEnrollment_IsRejected() {
        assertAbsenceRejectedForNonRegistered(91);
    }

    @Test
    @DisplayName("Waitlisted enrollment cannot request an excused absence")
    void requestAbsence_WaitlistedEnrollment_IsRejected() {
        assertAbsenceRejectedForNonRegistered(92);
    }

    private void assertAbsenceRejectedForNonRegistered(int sessionId) {
        sampleMember.setEmail("member@example.test");
        Session session = new Session();
        session.setId(sessionId);
        session.setSessionDate(java.time.LocalDate.of(2026, 10, 16));
        session.setStartTime(java.time.LocalTime.of(13, 0));
        com.sportscenter.sportclass.SportsClass sportsClass = new com.sportscenter.sportclass.SportsClass();
        sportsClass.setId(3);
        session.setSportsClass(sportsClass);
        when(userRepository.findByEmailIgnoreCase("member@example.test")).thenReturn(Optional.of(sampleMember));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(7, 3, "Registered")).thenReturn(false);

        var error = assertThrows(com.sportscenter.common.exception.BusinessException.class,
                () -> attendanceService.requestAbsence(sessionId, "Reason", "member@example.test"));

        assertTrue(error.getMessage().startsWith("NOT_YOUR_CLASS:"));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Search attendances with null-safe filter")
    void searchAttendances_Success() {
        Attendance att1 = new Attendance();
        att1.setAttendanceId(1);
        att1.setMember(sampleMember);
        att1.setState("Present");

        Attendance att2 = new Attendance();
        att2.setAttendanceId(2);
        att2.setMember(sampleMember);
        att2.setState("Absent");

        when(attendanceRepository.findAllWithDetails()).thenReturn(java.util.List.of(att1, att2));

        var all = attendanceService.searchAttendances(null, null, null, null, null, null, null);
        assertEquals(2, all.size());

        var filtered = attendanceService.searchAttendances(null, null, null, "Present", null, null, null);
        assertEquals(1, filtered.size());
        assertEquals("Present", filtered.get(0).state());
    }
}
