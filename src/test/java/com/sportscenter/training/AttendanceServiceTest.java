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
    @Mock com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    @Mock com.sportscenter.session.SessionAbsenceNoticeRepository noticeRepository;
    @Mock com.sportscenter.common.ActorPolicy actors;
    @InjectMocks com.sportscenter.session.SessionAbsenceService absenceService;
    private final org.springframework.security.core.Authentication memberAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("member@example.test","",java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER")));


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
        sampleMember.setId(7); sampleMember.setCenterId(1);
        sampleMember.setFullName("Hoàng Thị Oanh");

        sampleReceptionist = new User();
        sampleReceptionist.setId(5); sampleReceptionist.setCenterId(1);
        sampleReceptionist.setEmail("staff@example.test");
        sampleReceptionist.setFullName("Đỗ Thị Mai");sampleReceptionist.setStatus("Active");
        var role=new com.sportscenter.user.Role();role.setName("CenterManager");sampleReceptionist.setRole(role);
        var memberRole=new com.sportscenter.user.Role();memberRole.setName("Member");sampleMember.setRole(memberRole);sampleMember.setStatus("Active");
        lenient().when(actors.actor(memberAuth)).thenReturn(sampleMember);

    }

    @Test
    @DisplayName("Counter Check-In creates attendance record with CheckedIn state")
    void checkIn_Success() {
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService,"lobbyCheckinEnabled",true);
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
        current.setState("Absent");current.setSession(endedSession(15));
        when(sessionRepository.findByIdForUpdate(15)).thenReturn(Optional.of(current.getSession()));

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
    void correctAttendance_Present_SyncsTrainingResult() {
        Session session = endedSession(15);
        when(sessionRepository.findByIdForUpdate(15)).thenReturn(Optional.of(session));
        Attendance current = new Attendance();
        current.setAttendanceId(61); current.setSession(session); current.setMember(sampleMember); current.setState("Absent");
        TrainingResult result = new TrainingResult(); result.setAttendanceStatus("Absent");
        when(userRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(sampleReceptionist));
        when(attendanceRepository.findById(61)).thenReturn(Optional.of(current));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> call.getArgument(0));
        when(trainingResultRepository.findBySessionIdAndMemberId(15, 7)).thenReturn(Optional.of(result));
        when(trainingResultRepository.save(any(TrainingResult.class))).thenAnswer(call -> call.getArgument(0));

        AttendanceResponse response = attendanceService.correctAttendance(
                new AttendanceCorrectionRequest(61, "Present", "Approved absence"), "staff@example.test");

        assertEquals("Present", response.state());
        assertEquals("Present", result.getAttendanceStatus());
        verify(auditService).log(eq(5), eq("ATTENDANCE_CORRECTION"), eq("attendances"), eq(61), contains("Approved absence"));
    }

    @Test void requestAbsence_NormalIsRejected() {
        var session=noticeSession(88);session.getSportsClass().setClassType("Normal");session.setBookedMember(null);
        when(sessionRepository.findByIdForUpdate(88)).thenReturn(Optional.of(session));
        assertTrue(assertThrows(com.sportscenter.common.exception.BusinessException.class,()->absenceService.report(88,"Reason",memberAuth)).getMessage().startsWith("NORMAL_CLASS_NON_CANCELLABLE:"));
        verifyNoInteractions(attendanceRepository);
    }
    @Test void requestAbsence_AfterStartIsRejected() {
        var session=noticeSession(89);session.setStartTime(java.time.LocalTime.of(9,0));when(sessionRepository.findByIdForUpdate(89)).thenReturn(Optional.of(session));
        assertThrows(com.sportscenter.common.exception.BusinessException.class,()->absenceService.report(89,"Reason",memberAuth));verifyNoInteractions(attendanceRepository);
    }
    @Test void requestAbsence_CancelledBookingIsRejected() {
        var session=noticeSession(91);session.setStatus("Cancelled");when(sessionRepository.findByIdForUpdate(91)).thenReturn(Optional.of(session));
        assertThrows(com.sportscenter.common.exception.BusinessException.class,()->absenceService.report(91,"Reason",memberAuth));verifyNoInteractions(attendanceRepository);
    }
    @Test void requestAbsence_UnownedBookingIsRejected() {
        var session=noticeSession(92);User other=new User();other.setId(9);session.setBookedMember(other);when(sessionRepository.findByIdForUpdate(92)).thenReturn(Optional.of(session));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->absenceService.report(92,"Reason",memberAuth));verifyNoInteractions(attendanceRepository);
    }
    private Session noticeSession(int id) {
        var s=new Session();s.setId(id);s.setSessionDate(java.time.LocalDate.of(2026,10,16));s.setStartTime(java.time.LocalTime.of(13,0));s.setEndTime(java.time.LocalTime.of(14,30));
        var c=new com.sportscenter.sportclass.SportsClass();c.setId(3);c.setClassType("Advanced");c.setCenterId(1);s.setSportsClass(c);s.setBookedMember(sampleMember);return s;
    }
    private Session endedSession(int id){var s=noticeSession(id);s.setStartTime(java.time.LocalTime.of(7,45));s.setEndTime(java.time.LocalTime.of(9,15));return s;}
    @Test void lobbyCheckinIsDisabledByDefault(){assertThrows(com.sportscenter.common.exception.BusinessException.class,()->attendanceService.checkIn(7,"staff@example.test"));verifyNoInteractions(attendanceRepository);}

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

        sampleMember.setCenterId(1); sampleReceptionist.setCenterId(1);
        when(userRepository.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(sampleReceptionist));
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("staff@example.test", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("REGISTER_MEMBER")));
        var all = attendanceService.searchAttendances(null, null, null, null, null, null, auth);
        assertEquals(2, all.size());

        var filtered = attendanceService.searchAttendances(null, null, null, "Present", null, null, auth);
        assertEquals(1, filtered.size());
        assertEquals("Present", filtered.get(0).state());
    }
}
