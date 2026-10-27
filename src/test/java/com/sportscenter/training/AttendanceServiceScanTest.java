package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceScanTest {
    @Mock com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    @Mock AttendanceRepository attendanceRepository;
    @Mock TrainingResultRepository trainingResultRepository;
    @Mock SessionRepository sessionRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock Clock clock;
    @Mock ClassEnrollmentRepository enrollmentRepository;
    @Mock MemberRepository memberRepository;
    @Mock MemberPackageRepository memberPackageRepository;
    @InjectMocks AttendanceService service;

    private static final int MEMBER_ID = 801;
    private static final int STAFF_ID = 802;
    private static final byte[] SECRET = "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private User memberUser;
    private User staff;
    private Member profile;
    private long currentStep;

    @BeforeEach
    void setUp() {
        Object attempts = ReflectionTestUtils.getField(AttendanceService.class, "SCAN_ATTEMPTS");
        if (attempts instanceof java.util.Map<?, ?> map) map.clear();
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        memberUser = new User(); memberUser.setId(MEMBER_ID); memberUser.setFullName("Test Member"); memberUser.setStatus("Active");
        staff = new User(); staff.setId(STAFF_ID); staff.setEmail("staff@test.local");staff.setCenterId(1);staff.setStatus("Active");var role=new com.sportscenter.user.Role();role.setName("Receptionist");staff.setRole(role);memberUser.setCenterId(1);
        lenient().when(sessionRepository.findByIdForUpdate(anyInt())).thenAnswer(call->Optional.of(session(call.getArgument(0),LocalTime.of(9,30))));
        lenient().when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(MEMBER_ID,40,"Registered")).thenReturn(true);
        profile = new Member(); profile.setUser(memberUser); profile.setCheckinSecret(Totp.encodeSecret(SECRET));
        currentStep = clock.instant().getEpochSecond() / 30;
        lenient().when(userRepository.findByEmailIgnoreCase("staff@test.local")).thenReturn(Optional.of(staff));
        lenient().when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(profile));
        lenient().when(memberPackageRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(activePackage()));
        lenient().when(memberRepository.save(profile)).thenReturn(profile);
        lenient().when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> {
            Attendance saved = call.getArgument(0);
            if (saved.getAttendanceId() == null) saved.setAttendanceId(1);
            return saved;
        });
    }

    @Test
    void scanCheckin_ValidCodeRecordsPresentWithoutExposingSecret() {
        String code = Totp.code(SECRET, currentStep);
        AttendanceResponse response = service.scanCheckin(MEMBER_ID, code, 120, staff.getEmail());

        assertEquals("Present", response.state());
        assertEquals(currentStep, profile.getCheckinLastStep());
        assertFalse(java.util.Arrays.stream(CheckinCodeResponse.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("secret")));
        org.mockito.ArgumentCaptor<String> auditDetail = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditService).log(eq(STAFF_ID), eq("SCAN_CHECKIN"), eq("attendances"), any(), auditDetail.capture());
        assertFalse(auditDetail.getValue().contains(profile.getCheckinSecret()));
        assertFalse(auditDetail.getValue().contains(code));
    }

    @Test
    void createCheckinCode_GeneratesTwentyByteSecretAndReturnsNoSecret() {
        profile.setCheckinSecret(null);
        when(userRepository.findByEmailIgnoreCase("member@test.local")).thenReturn(Optional.of(memberUser));

        CheckinCodeResponse response = service.createCheckinCode("member@test.local");

        assertTrue(response.payload().matches("\\d{6}"));
        assertTrue(response.expiresInSeconds() >= 1 && response.expiresInSeconds() <= 30);
        assertEquals(20, java.util.Base64.getDecoder().decode(profile.getCheckinSecret()).length);
        assertFalse(java.util.Arrays.stream(CheckinCodeResponse.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("secret")));
    }

    @Test
    void scanCheckin_CodeFromFutureStepIsRejectedUntilItsValidityWindow() {
        assertTrue(assertThrows(BusinessException.class,()->service.scanCheckin(MEMBER_ID,Totp.code(SECRET,currentStep+1),120,staff.getEmail())).getMessage().startsWith("INVALID_CHECKIN_CODE:"));
        verify(attendanceRepository,never()).save(any());
    }

    @Test
    void scanCheckin_BookingBlockedMemberCanStillCheckIn() {
        profile.setBookingBlockedUntil(java.time.LocalDateTime.of(2026, 10, 17, 10, 0));

        AttendanceResponse response = service.scanCheckin(MEMBER_ID, Totp.code(SECRET, currentStep), 120, staff.getEmail());

        assertEquals("Present", response.state());
    }

    @Test
    void scanCheckin_ReusingAcceptedStepIsRejected() {
        String code = Totp.code(SECRET, currentStep);
        service.scanCheckin(MEMBER_ID, code, 120, staff.getEmail());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.scanCheckin(MEMBER_ID, code, 120, staff.getEmail()));

        assertTrue(error.getMessage().startsWith("INVALID_CHECKIN_CODE:"));
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    void scanCheckin_SixthInvalidAttemptInOneMinuteIsRateLimited() {
        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class,
                    () -> service.scanCheckin(MEMBER_ID, "not-a-code", 120, staff.getEmail()));
        }

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.scanCheckin(MEMBER_ID, "not-a-code", 120, staff.getEmail()));

        assertTrue(error.getMessage().contains("scan limit reached"));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void scanCheckin_ExpiredOriginalPrivateEntitlementIsRejected() {
        Session privateSession=session(120,LocalTime.of(9,30));privateSession.setBookedMember(memberUser);
        var expired=activePackage();expired.setExpiresAt(java.time.LocalDateTime.of(2026,10,15,8,0));privateSession.setMemberPackage(expired);
        when(sessionRepository.findByIdForUpdate(120)).thenReturn(Optional.of(privateSession));
        assertTrue(assertThrows(BusinessException.class,()->service.scanCheckin(MEMBER_ID,Totp.code(SECRET,currentStep),120,staff.getEmail())).getMessage().startsWith("PACKAGE_EXPIRED:"));
        verify(attendanceRepository,never()).save(any());
    }

    @Test
    void scanCheckin_BeforeSessionStartIsRejected() {
        Session session = session(123, LocalTime.of(12, 0));
        when(sessionRepository.findByIdForUpdate(123)).thenReturn(Optional.of(session));
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.scanCheckin(MEMBER_ID, Totp.code(SECRET, currentStep), 123, staff.getEmail()));

        assertTrue(error.getMessage().startsWith("ATTENDANCE_WINDOW_CLOSED:"));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void scanCheckin_RepeatedSessionScanUpdatesSameAttendanceRow() {
        Session session = session(124, LocalTime.of(9, 30));
        when(sessionRepository.findByIdForUpdate(124)).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(MEMBER_ID, 40, "Registered")).thenReturn(true);
        AtomicReference<Attendance> row = new AtomicReference<>();
        when(attendanceRepository.findBySessionIdAndMemberId(124, MEMBER_ID)).thenAnswer(call -> Optional.ofNullable(row.get()));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(call -> {
            Attendance saved = call.getArgument(0);
            if (saved.getAttendanceId() == null) saved.setAttendanceId(5);
            row.set(saved);
            return saved;
        });

        service.scanCheckin(MEMBER_ID, Totp.code(SECRET, currentStep), 124, staff.getEmail());
        when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:30Z"));
        service.scanCheckin(MEMBER_ID, Totp.code(SECRET, currentStep + 1), 124, staff.getEmail());

        assertNotNull(row.get());
        assertEquals(5, row.get().getAttendanceId());
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
    }

    private MemberPackage activePackage() {
        MemberPackage memberPackage = new MemberPackage();
        memberPackage.setStatus("Active");
        memberPackage.setEndDate(LocalDate.of(2026, 11, 1));memberPackage.setActivatedAt(java.time.LocalDateTime.of(2026,10,1,8,0));memberPackage.setExpiresAt(java.time.LocalDateTime.of(2026,11,1,8,0));
        return memberPackage;
    }

    private Session session(Integer id, LocalTime start) {
        SportsClass sportsClass = new SportsClass(); sportsClass.setId(40);
        Session session = new Session(); session.setId(id); session.setSportsClass(sportsClass);
        session.setSessionDate(LocalDate.of(2026, 10, 16)); session.setStartTime(start); session.setEndTime(start.plusMinutes(90));
        return session;
    }
}
