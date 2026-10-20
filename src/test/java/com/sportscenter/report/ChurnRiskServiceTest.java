package com.sportscenter.report;

import com.sportscenter.center.CenterContext;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.training.AttendanceRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChurnRiskServiceTest {
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock AttendanceRepository attendanceRepository;
    @Mock UserRepository userRepository;
    @Mock CenterContext centerContext;
    @Mock Authentication authentication;
    @InjectMocks ChurnRiskService service;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-16T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Test
    void coachReportUsesCoachCenterAndExcludesMembersWithRecentAttendance() {
        when(authentication.getName()).thenReturn("coach@example.test");
        User coach = new User(); coach.setId(5); coach.setCenterId(2);
        when(userRepository.findByEmailIgnoreCase("coach@example.test")).thenReturn(Optional.of(coach));
        when(centerContext.currentCenterId()).thenReturn(2);
        User atRisk = new User(); atRisk.setId(10); atRisk.setFullName("Alex"); atRisk.setEmail("alex@example.test");
        User active = new User(); active.setId(11); active.setFullName("Sam"); active.setEmail("sam@example.test");
        when(enrollmentRepository.findRiskCandidatesForCoach(5, 2)).thenReturn(List.of(enrollment(atRisk), enrollment(active)));
        when(attendanceRepository.hasRecentAttendance(eq(10), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);
        when(attendanceRepository.hasRecentAttendance(eq(11), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(true);
        // InjectMocks uses a mock Clock unless supplied; service must use the fixed business clock.
        org.springframework.test.util.ReflectionTestUtils.setField(service, "clock", clock);

        List<AtRiskMemberResponse> result = service.forCoach(authentication);

        assertEquals(List.of(new AtRiskMemberResponse(10, "Alex", "alex@example.test")), result);
        verify(enrollmentRepository).findRiskCandidatesForCoach(5, 2);
        verify(attendanceRepository).hasRecentAttendance(eq(10), eq(LocalDateTime.of(2026, 10, 16, 10, 0).minusDays(14)),
                eq(LocalDateTime.of(2026, 10, 16, 10, 0)));
    }

    @Test
    void centerReportUsesCurrentCenterAndDeduplicatesMembers() {
        when(centerContext.currentCenterId()).thenReturn(4);
        User member = new User(); member.setId(12); member.setFullName("Jamie"); member.setEmail("jamie@example.test");
        when(enrollmentRepository.findRiskCandidatesByCenter(4)).thenReturn(List.of(enrollment(member), enrollment(member)));
        when(attendanceRepository.hasRecentAttendance(eq(12), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "clock", clock);

        List<AtRiskMemberResponse> result = service.forCenter();

        assertEquals(1, result.size());
        assertEquals(12, result.get(0).memberId());
        verify(enrollmentRepository).findRiskCandidatesByCenter(4);
    }

    private ClassEnrollment enrollment(User member) {
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setMember(member);
        return enrollment;
    }
}
