package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
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

    @InjectMocks
    private AttendanceService attendanceService;

    private User sampleMember;
    private User sampleReceptionist;

    @BeforeEach
    void setUp() {
        sampleMember = new User();
        sampleMember.setId(7);
        sampleMember.setFullName("Hoàng Thị Oanh");

        sampleReceptionist = new User();
        sampleReceptionist.setId(5);
        sampleReceptionist.setFullName("Đỗ Thị Mai");
    }

    @Test
    @DisplayName("Counter Check-In creates attendance record with CheckedIn state")
    void checkIn_Success() {
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findById(5)).thenReturn(Optional.of(sampleReceptionist));

        Attendance saved = new Attendance();
        saved.setAttendanceId(50);
        saved.setMember(sampleMember);
        saved.setRecordedBy(sampleReceptionist);
        saved.setState("CheckedIn");

        when(attendanceRepository.save(any(Attendance.class))).thenReturn(saved);

        AttendanceResponse response = attendanceService.checkIn(7, 5);

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
        AttendanceResponse response = attendanceService.correctAttendance(req);

        assertNotNull(response);
        assertEquals("Present", response.state());
        verify(auditService).log(isNull(), eq("ATTENDANCE_CORRECTION"), eq("attendances"), eq(60), contains("Reason: Học viên đến muộn"));
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
