package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingResultServiceTest {
    @Mock com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    private final Authentication coachAuth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("coach@example.test","",List.of(new SimpleGrantedAuthority("ROLE_COACH"), new SimpleGrantedAuthority("RECORD_RESULT")));

    @Mock
    private TrainingResultRepository trainingResultRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private com.sportscenter.enrollment.ClassEnrollmentRepository enrollmentRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private TrainingResultService trainingResultService;

    private Session sampleSession;
    private User sampleMember;
    private User sampleCoach;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        sampleCoach = new User();
        sampleCoach.setId(2); sampleCoach.setCenterId(1);
        sampleCoach.setFullName("Trần Thị Hương");sampleCoach.setStatus("Active");sampleCoach.setEmail("coach@example.test");
        var coachRole = new com.sportscenter.user.Role();
        coachRole.setName("Coach");
        sampleCoach.setRole(coachRole);

        sampleMember = new User();
        sampleMember.setId(7); sampleMember.setCenterId(1);
        sampleMember.setFullName("Hoàng Thị Oanh");
        sampleMember.setStatus("Active");
        var memberRole = new com.sportscenter.user.Role();
        memberRole.setName("Member");
        sampleMember.setRole(memberRole);

        sampleSession = new Session();
        sampleSession.setId(1);sampleSession.setSessionDate(java.time.LocalDate.of(2026,10,16));sampleSession.setStartTime(java.time.LocalTime.of(9,30));sampleSession.setEndTime(java.time.LocalTime.of(11,0));
        lenient().when(sessionRepository.findCenterIdById(1)).thenReturn(Optional.of(1));
        lenient().when(userRepository.findByEmailIgnoreCase("coach@example.test")).thenReturn(Optional.of(sampleCoach));
        var sportsClass = new com.sportscenter.sportclass.SportsClass();
        sportsClass.setId(3);
        sportsClass.setCoach(sampleCoach);
        sampleSession.setSportsClass(sportsClass);
        lenient().when(enrollmentRepository.existsByMemberIdAndSportsClassIdAndStatus(7, 3, "Registered")).thenReturn(true);
    }

    @Test
    @DisplayName("Record training result successfully and syncs attendance")
    void recordResult_Success() {
        TrainingResultRequest request = new TrainingResultRequest(1, 7, 2, "Tập rất tốt, đúng kỹ thuật", "Present");

        when(sessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleSession));
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findById(2)).thenReturn(Optional.of(sampleCoach));
        when(trainingResultRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());

        TrainingResult saved = new TrainingResult();
        saved.setResultId(10);
        saved.setSession(sampleSession);
        saved.setMember(sampleMember);
        saved.setCoach(sampleCoach);
        saved.setContent("Tập rất tốt, đúng kỹ thuật");
        saved.setAttendanceStatus("Present");

        when(trainingResultRepository.save(any(TrainingResult.class))).thenReturn(saved);
        when(attendanceRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());

        TrainingResultResponse response = trainingResultService.recordResult(request,coachAuth);

        assertNotNull(response);
        assertEquals(10, response.resultId());
        assertEquals("Present", response.attendanceStatus());
        assertEquals("Hoàng Thị Oanh", response.memberName());
        verify(trainingResultRepository).save(any(TrainingResult.class));
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Member can only search own training results")
    void searchResults_MemberIsScopedToSelf() {
        sampleMember.setEmail("member@example.test");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("member@example.test");
        when(userRepository.findByEmailIgnoreCase("member@example.test")).thenReturn(Optional.of(sampleMember));
        when(trainingResultRepository.searchResults(null, 7, null, null)).thenReturn(List.of());

        assertTrue(trainingResultService.searchResults(null, null, null, authentication).isEmpty());
        verify(trainingResultRepository).searchResults(null, 7, null, null);
    }

    @Test
    @DisplayName("Member cannot search another member's training results")
    void searchResults_MemberCannotSpoofMemberId() {
        sampleMember.setEmail("member@example.test");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("member@example.test");
        when(userRepository.findByEmailIgnoreCase("member@example.test")).thenReturn(Optional.of(sampleMember));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> trainingResultService.searchResults(null, 999, null, authentication));
        verifyNoInteractions(trainingResultRepository);
    }

    @Test
    @DisplayName("Member can search own training results with matching memberId")
    void searchResults_MemberMayPassOwnMemberId() {
        sampleMember.setEmail("member@example.test");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("member@example.test");
        when(userRepository.findByEmailIgnoreCase("member@example.test")).thenReturn(Optional.of(sampleMember));
        when(trainingResultRepository.searchResults(1, 7, null, null)).thenReturn(List.of());

        assertTrue(trainingResultService.searchResults(1, 7, null, authentication).isEmpty());
        verify(trainingResultRepository).searchResults(1, 7, null, null);
    }

    @Test
    @DisplayName("Bulk record results for whole session")
    void recordBulkResults_Success() {
        BulkTrainingResultRequest.Item item1 = new BulkTrainingResultRequest.Item(7, "Present", "Tốt", null);
        BulkTrainingResultRequest request = new BulkTrainingResultRequest(1, 2, List.of(item1));

        when(sessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleSession));
        when(userRepository.findById(2)).thenReturn(Optional.of(sampleCoach));
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(trainingResultRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());
        when(trainingResultRepository.save(any(TrainingResult.class))).thenAnswer(i -> {
            TrainingResult tr = i.getArgument(0);
            tr.setResultId(11);
            return tr;
        });
        when(attendanceRepository.findBySessionIdAndMemberId(1, 7)).thenReturn(Optional.empty());

        List<TrainingResultResponse> responses = trainingResultService.recordBulkResults(request,coachAuth);

        assertEquals(1, responses.size());
        assertEquals("Present", responses.get(0).attendanceStatus());
        verify(trainingResultRepository, atLeastOnce()).save(any(TrainingResult.class));
    }

    @Test
    @DisplayName("Coach cannot spoof coachId to record results for another coach's class")
    void recordResult_CoachCannotSpoofCoachId() {
        sampleCoach.setEmail("coach@example.test");
        SportsClass sportsClass = new SportsClass();
        User otherCoach = new User();
        otherCoach.setId(9);
        sportsClass.setCoach(otherCoach);
        sampleSession.setSportsClass(sportsClass);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("coach@example.test");
        when(authentication.getAuthorities()).thenAnswer(invocation -> java.util.List.of(new SimpleGrantedAuthority("ROLE_COACH"), new SimpleGrantedAuthority("RECORD_RESULT")));
        when(sessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleSession));
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findById(2)).thenReturn(Optional.of(sampleCoach));
        when(userRepository.findByEmailIgnoreCase("coach@example.test")).thenReturn(Optional.of(sampleCoach));

        TrainingResultRequest request = new TrainingResultRequest(1, 7, 9, "spoof", "Present");
        assertThrows(com.sportscenter.common.exception.BusinessException.class,
                () -> trainingResultService.recordResult(request, authentication));
        verify(userRepository, never()).findById(9);
        verify(trainingResultRepository, never()).save(any(TrainingResult.class));
    }

    @Test
    @DisplayName("Content writer must have the persisted Coach role even with Coach authorities")
    void recordResult_PersistedRoleProtectsServiceEntry() {
        sampleCoach.getRole().setName("CenterManager");
        when(sessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleSession));
        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> trainingResultService.recordResult(new TrainingResultRequest(1, 7, 2, "", "Present"), coachAuth));
        verifyNoInteractions(trainingResultRepository, attendanceRepository, auditService);
    }

    @Test
    void searchResults_StaffWithoutCenterCannotBecomeGlobal() {
        sampleCoach.getRole().setName("CenterManager"); sampleCoach.setCenterId(null);
        Authentication staff = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "coach@example.test", "", List.of(new SimpleGrantedAuthority("MANAGE_CLASSES")));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> trainingResultService.searchResults(null, null, null, staff));
        verifyNoInteractions(trainingResultRepository);
    }

    @Test
    void searchResults_GlobalAdminStillRequiresReadAuthority() {
        sampleCoach.getRole().setName("Admin"); sampleCoach.setCenterId(null);
        Authentication admin = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "coach@example.test", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> trainingResultService.searchResults(null, null, null, admin));
        verifyNoInteractions(trainingResultRepository);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"date","start","end"})
    void updateContent_MissingLegacyTimeFailsClosedWithoutWrites(String missing) {
        var permission = new com.sportscenter.user.Permission(); permission.setName("RECORD_RESULT");
        sampleCoach.getRole().getPermissions().add(permission);
        var result = new TrainingResult(); result.setResultId(10); result.setSession(sampleSession);
        result.setCoach(sampleCoach); result.setMember(sampleMember); result.setContent("Original");
        when(trainingResultRepository.findSessionIdByResultId(10)).thenReturn(Optional.of(1));
        when(sessionRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleSession));
        when(trainingResultRepository.findByIdForUpdate(10)).thenReturn(Optional.of(result));
        switch (missing) {
            case "date" -> sampleSession.setSessionDate(null);
            case "start" -> sampleSession.setStartTime(null);
            default -> sampleSession.setEndTime(null);
        }
        var ex = assertThrows(com.sportscenter.common.exception.BusinessException.class,
                () -> trainingResultService.updateContent(10, new TrainingResultContentRequest("Updated"), coachAuth));
        assertTrue(ex.getMessage().startsWith("SESSION_TIME_REQUIRED:")); assertEquals("Original", result.getContent());
        verify(trainingResultRepository, never()).save(any()); verifyNoInteractions(attendanceRepository, auditService);
        var ordered = inOrder(scheduleWriteLock, sessionRepository, trainingResultRepository);
        ordered.verify(scheduleWriteLock).acquire(1);
        ordered.verify(trainingResultRepository).findSessionIdByResultId(10);
        ordered.verify(sessionRepository).findCenterIdById(1);
        ordered.verify(sessionRepository).findByIdForUpdate(1);
        ordered.verify(trainingResultRepository).findByIdForUpdate(10);
    }
}
