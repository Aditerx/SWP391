package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.room.Room;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.Role;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionIncidentServiceTest {
    @org.mockito.Mock com.sportscenter.membership.CoachCapacityService phaseCapacity;
    @org.mockito.Mock com.sportscenter.session.SessionChangeRepository phaseChanges;
    @org.mockito.Mock com.sportscenter.session.SessionAbsenceService phaseAbsence;
    @org.mockito.Mock com.sportscenter.session.CenterIncidentService phaseIncidents;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SportsClassRepository sportsClassRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock
    private AuditService auditService;

    @Spy
    private SessionSlotValidator sessionSlotValidator = new SessionSlotValidator();

    @Mock
    private HolidayRepository holidayRepository;

    @Spy
    private Clock clock = Clock.fixed(
            LocalDate.of(2026, 10, 20).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
            ZoneId.of("Asia/Ho_Chi_Minh")
    );

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SessionService sessionService;
    @Mock private ScheduleWriteLock scheduleWriteLock;
    @Mock private com.sportscenter.user.MemberRepository memberRepository;
    @Mock private com.sportscenter.specialization.CoachSpecializationRepository coachSpecializationRepository;


    private User managerUser;
    private Authentication managerAuth;
    private User coach1;
    private User coach2;
    private Room room1;
    private Room room2;
    private SportsClass normalClass;
    private Session normalSession;
    private SportsClass advancedClass;
    private MemberPackage subscription;
    private Session privateSession;

    @BeforeEach
    void setUp() {
        lenient().when(sessionRepository.findCenterIdById(anyInt())).thenReturn(Optional.of(1));
        lenient().when(sportsClassRepository.findCenterIdById(anyInt())).thenReturn(Optional.of(1));
        lenient().when(coachSpecializationRepository.findByCoachUserId(anyInt())).thenAnswer(call -> {
            var a = new com.sportscenter.specialization.CoachSpecialization();
            var spec = new com.sportscenter.specialization.Specialization();
            var subject = new Subject(); subject.setId(1); subject.setOutdoor(false); spec.setSubject(subject); spec.setStatus("Active"); a.setSpecialization(spec);
            var b = new com.sportscenter.specialization.CoachSpecialization();
            var spec2 = new com.sportscenter.specialization.Specialization();
            var subject2 = new Subject(); subject2.setId(2); subject2.setOutdoor(false); spec2.setSubject(subject2); spec2.setStatus("Active"); b.setSpecialization(spec2);
            return List.of(a,b);
        });
        lenient().when(sessionRepository.findByIdForUpdate(anyInt())).thenAnswer(call -> sessionRepository.findByIdWithDetails(call.getArgument(0)));

        Role managerRole = new Role();
        managerRole.setName("CenterManager");
        managerUser = new User();
        managerUser.setId(1);
        managerUser.setEmail("manager@sportscenter.com");
        managerUser.setFullName("Quản Lý Trung Tâm");
        managerUser.setRole(managerRole);
        managerUser.setStatus("Active");managerUser.setCenterId(1);
        managerAuth = new UsernamePasswordAuthenticationToken(managerUser.getEmail(), "n/a",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CENTERMANAGER")));
        lenient().when(userRepository.findByEmailIgnoreCase(managerUser.getEmail())).thenReturn(Optional.of(managerUser));

        Role coachRole = new Role();
        coachRole.setName("Coach");
        coach1 = new User();
        coach1.setId(10); coach1.setCenterId(1);
        coach1.setFullName("HLV Nguyễn Văn A");
        coach1.setRole(coachRole); coach1.setStatus("Active");

        coach2 = new User();
        coach2.setId(11); coach2.setCenterId(1);
        coach2.setFullName("HLV Trần Văn B");
        coach2.setRole(coachRole); coach2.setStatus("Active");

        Subject indoorSubject = new Subject();
        indoorSubject.setId(1); indoorSubject.setOutdoor(false);
        indoorSubject.setName("Cầu Lông");
        indoorSubject.setSlug("cau-long");

        room1 = new Room();
        room1.setId(101);
        room1.setName("Sân Cầu Lông 1");
        room1.setStatus("Available");
        room1.setCapacity(20);
        room1.setFacilityType("Indoor");

        room2 = new Room();
        room2.setId(102);
        room2.setName("Sân Cầu Lông 2");
        room2.setStatus("Available");
        room2.setCapacity(20);
        room2.setFacilityType("Indoor");

        normalClass = new SportsClass();
        normalClass.setId(201);
        normalClass.setName("Lớp Cầu Lông K01");
        normalClass.setClassType("Normal");
        normalClass.setMaxCapacity(15);
        normalClass.setCoach(coach1);
        normalClass.setSubject(indoorSubject);
        normalClass.setStartDate(LocalDate.of(2026, 10, 20));
        normalClass.setEndDate(LocalDate.of(2026, 11, 20));

        normalSession = new Session();
        normalSession.setId(301);
        normalSession.setSportsClass(normalClass);
        normalSession.setRoom(room1);
        normalSession.setCoach(coach1);
        normalSession.setSessionDate(LocalDate.of(2026, 10, 22));
        normalSession.setStartTime(LocalTime.of(6, 0));
        normalSession.setEndTime(LocalTime.of(7, 30));
        normalSession.setStatus("Scheduled");

        MembershipPackage mp = new MembershipPackage();
        mp.setId(5);
        mp.setName("Gói 1:1 Tennis");
        mp.setPackageType("Advanced");
        mp.setTotalCredit(12);

        User member = new User();
        member.setId(20);
        member.setFullName("Học Viên C");

        subscription = new MemberPackage();
        subscription.setSubscriptionId(501);
        subscription.setMember(member);
        subscription.setMembershipPackage(mp);
        subscription.setStartDate(LocalDate.of(2026, 10, 20));
        subscription.setEndDate(LocalDate.of(2026, 11, 20));
        subscription.setValidityDays(45);
        subscription.setRemainingCredit(8);
        subscription.setStatus("Active");subscription.setSubject(indoorSubject);subscription.setPrimaryCoach(coach1);subscription.setActivatedAt(java.time.LocalDateTime.of(2026,10,20,0,0));subscription.setExpiresAt(java.time.LocalDateTime.of(2026,11,20,20,0));

        advancedClass = new SportsClass();
        advancedClass.setId(202);
        advancedClass.setName("Tennis 1:1 Nâng Cao");
        advancedClass.setClassType("Advanced");
        advancedClass.setMaxCapacity(1);
        advancedClass.setSubject(indoorSubject);

        privateSession = new Session();
        privateSession.setId(302);
        privateSession.setSportsClass(advancedClass);
        privateSession.setRoom(room1);
        privateSession.setCoach(coach1);
        privateSession.setBookedMember(member);
        privateSession.setMemberPackage(subscription);
        privateSession.setSessionDate(LocalDate.of(2026, 10, 25));
        privateSession.setStartTime(LocalTime.of(7, 45));
        privateSession.setEndTime(LocalTime.of(9, 15));
        privateSession.setStatus("Scheduled");
    }

    @Test
    @DisplayName("Sự cố thời tiết/mưa bão: Manager dời lịch buổi học lớp Normal thành công")
    void rescheduleSession_WeatherIncident_NormalClass_Success() {
        when(userRepository.findByEmailIgnoreCase(managerUser.getEmail())).thenReturn(Optional.of(managerUser));
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        SessionRescheduleRequest request = move(
                LocalDate.of(2026, 10, 24),
                LocalTime.of(7, 45),
                LocalTime.of(9, 15),
                null, // Giữ nguyên room1
                "Mưa bão lớn ngập sân tập ngoài trời",
                null
        );

        SessionResponse response = sessionService.rescheduleSession(normalSession.getId(), request, managerAuth);

        assertNotNull(response);
        assertEquals(LocalDate.of(2026, 10, 24), response.sessionDate());
        assertEquals(LocalTime.of(7, 45), response.startTime());
        assertEquals(LocalTime.of(9, 15), response.endTime());
        assertEquals("Scheduled", response.status());

        verify(auditService).log(eq(managerUser.getId()), eq("INCIDENT_RESCHEDULE_SESSION"), eq("SESSION"), eq(301),
                contains("Mưa bão lớn ngập sân"));
    }

    @Test
    @DisplayName("Manager dời buổi học vượt quá ngày kết thúc của lớp Normal -> tự động cập nhật SportsClass.endDate")
    void rescheduleSession_BeyondClassEndDate_ExtendsClassEndDate() {
        when(userRepository.findByEmailIgnoreCase(managerUser.getEmail())).thenReturn(Optional.of(managerUser));
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        LocalDate newDateBeyondEnd = LocalDate.of(2026, 11, 25); // Class endDate là 2026-11-20
        SessionRescheduleRequest request = move(
                newDateBeyondEnd,
                LocalTime.of(6, 0),
                LocalTime.of(7, 30),
                null,
                "Sự cố sửa chữa mặt sân thi đấu",
                null
        );

        sessionService.rescheduleSession(normalSession.getId(), request, managerAuth);

        assertEquals(newDateBeyondEnd, normalClass.getEndDate());
        verify(sportsClassRepository).save(normalClass);
    }

    @Test
    @DisplayName("Sự cố trung tâm dời buổi 1:1 vượt quá hạn 45 ngày của gói -> Tự động gia hạn endDate & validityDays")
    void rescheduleSession_PrivateSession_BeyondPackageEndDate_RequiresApprovedRecovery() {
        when(sessionRepository.findByIdWithDetails(privateSession.getId())).thenReturn(Optional.of(privateSession));
        when(memberPackageRepository.findByIdForUpdate(subscription.getSubscriptionId())).thenReturn(Optional.of(subscription));
        var request=move(LocalDate.of(2026,11,30),LocalTime.of(7,45),LocalTime.of(9,15),null,"Actual incident",null);
        assertThrows(BusinessException.class,()->sessionService.rescheduleSession(privateSession.getId(),request,managerAuth));
        assertEquals(LocalDate.of(2026,11,20),subscription.getEndDate());
        verify(phaseIncidents).approveMakeupEnd(eq(1),eq(subscription),eq(LocalDate.of(2026,11,30).atTime(9,15)),anyString(),eq(managerUser));
        verify(sessionRepository,never()).save(any());
    }

    @Test
    @DisplayName("Manager dời buổi 1:1 kèm gia hạn thêm ngày (extendValidityDays)")
    void rescheduleSession_PrivateSession_ExplicitExtendValidityDaysIsRejected() {
        when(sessionRepository.findByIdWithDetails(privateSession.getId())).thenReturn(Optional.of(privateSession));
        var request=move(LocalDate.of(2026,10,28),LocalTime.of(7,45),LocalTime.of(9,15),null,"Actual incident",7);
        assertTrue(assertThrows(BusinessException.class,()->sessionService.rescheduleSession(privateSession.getId(),request,managerAuth)).getMessage().startsWith("USE_INCIDENT_RECOVERY:"));
        assertEquals(LocalDate.of(2026,11,20),subscription.getEndDate());
        verify(sessionRepository,never()).save(any());
    }

    @Test
    @DisplayName("Dời lịch buổi học về ngày trong quá khứ -> Ném BusinessException")
    void rescheduleSession_DateInPast_ThrowsException() {
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));

        SessionRescheduleRequest request = move(
                LocalDate.of(2026, 10, 19), // Trước mốc today 2026-10-20
                LocalTime.of(6, 0),
                LocalTime.of(7, 30),
                null,
                "Lý do",
                null
        );

        assertThrows(BusinessException.class, () ->
                sessionService.rescheduleSession(normalSession.getId(), request, managerAuth));
    }

    @Test
    @DisplayName("Dời lịch buổi học vào ngày nghỉ lễ của trung tâm -> Ném BusinessException")
    void rescheduleSession_Holiday_ThrowsException() {
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(holidayRepository.existsByHolidayDateAndIsActiveTrue(LocalDate.of(2026, 11, 2))).thenReturn(true);

        SessionRescheduleRequest request = move(
                LocalDate.of(2026, 11, 2),
                LocalTime.of(6, 0),
                LocalTime.of(7, 30),
                null,
                "Lý do",
                null
        );

        assertThrows(BusinessException.class, () ->
                sessionService.rescheduleSession(normalSession.getId(), request, managerAuth));
    }

    @Test
    @DisplayName("Dời lịch buổi học trùng phòng đã có lịch khác -> Ném BusinessException")
    void rescheduleSession_RoomConflict_ThrowsException() {
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(sessionRepository.findRoomConflicts(eq(room1.getId()), any(), any(), any(), eq(normalSession.getId())))
                .thenReturn(List.of(new Session()));

        SessionRescheduleRequest request = move(
                LocalDate.of(2026, 10, 24),
                LocalTime.of(6, 0),
                LocalTime.of(7, 30),
                null,
                "Lý do",
                null
        );

        assertThrows(BusinessException.class, () ->
                sessionService.rescheduleSession(normalSession.getId(), request, managerAuth));
    }

    @Test
    @DisplayName("Dời lịch buổi học trùng HLV đã có lịch dạy -> Ném BusinessException")
    void rescheduleSession_CoachConflict_ThrowsException() {
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(sessionRepository.findCoachConflicts(eq(coach1.getId()), any(), any(), any(), eq(normalSession.getId())))
                .thenReturn(List.of(new Session()));

        SessionRescheduleRequest request = move(
                LocalDate.of(2026, 10, 24),
                LocalTime.of(6, 0),
                LocalTime.of(7, 30),
                null,
                "Lý do",
                null
        );

        assertThrows(BusinessException.class, () ->
                sessionService.rescheduleSession(normalSession.getId(), request, managerAuth));
    }

    @Test
    @DisplayName("Dời lịch buổi học đã Completed -> Ném BusinessException")
    void rescheduleSession_AlreadyCompleted_ThrowsException() {
        normalSession.setStatus("Completed");
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));

        SessionRescheduleRequest request = move(
                LocalDate.of(2026, 10, 24),
                LocalTime.of(6, 0),
                LocalTime.of(7, 30),
                null,
                "Lý do",
                null
        );

        assertThrows(BusinessException.class, () ->
                sessionService.rescheduleSession(normalSession.getId(), request, managerAuth));
    }

    @Test
    @DisplayName("Sự cố HLV nghỉ ốm/đột xuất: Manager chọn HLV thay thế thành công")
    void substituteCoach_Success() {
        when(userRepository.findByEmailIgnoreCase(managerUser.getEmail())).thenReturn(Optional.of(managerUser));
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(userRepository.findById(coach2.getId())).thenReturn(Optional.of(coach2));
        when(sessionRepository.findCoachConflicts(eq(coach2.getId()), any(), any(), any(), eq(normalSession.getId())))
                .thenReturn(List.of());
        when(sessionRepository.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        SubstituteCoachRequest request = substitute(coach2.getId(), "HLV Nguyễn Văn A nghỉ ốm đột xuất");

        SessionResponse response = sessionService.substituteCoach(normalSession.getId(), request, managerAuth);

        assertNotNull(response);
        assertEquals(coach2.getId(), response.coachId());
        assertEquals("HLV Trần Văn B", response.coachName());

        verify(auditService).log(eq(managerUser.getId()), eq("INCIDENT_SUBSTITUTE_COACH"), eq("SESSION"), eq(301),
                contains("HLV Nguyễn Văn A nghỉ ốm đột xuất"));
    }

    @Test
    @DisplayName("Chọn HLV thay thế khi HLV mới bị trùng lịch dạy -> Ném BusinessException")
    void substituteCoach_Conflict_ThrowsException() {
        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(userRepository.findById(coach2.getId())).thenReturn(Optional.of(coach2));
        when(sessionRepository.findCoachConflicts(eq(coach2.getId()), any(), any(), any(), eq(normalSession.getId())))
                .thenReturn(List.of(new Session()));

        SubstituteCoachRequest request = substitute(coach2.getId(), "HLV bận");

        assertThrows(BusinessException.class, () ->
                sessionService.substituteCoach(normalSession.getId(), request, managerAuth));
    }

    @Test
    @DisplayName("Chọn người dùng không có vai trò HLV thay thế -> Ném BusinessException")
    void substituteCoach_NonCoachUser_ThrowsException() {
        User nonCoach = new User();
        nonCoach.setId(99);
        Role memberRole = new Role();
        memberRole.setName("Member");
        nonCoach.setRole(memberRole);

        when(sessionRepository.findByIdWithDetails(normalSession.getId())).thenReturn(Optional.of(normalSession));
        when(userRepository.findById(99)).thenReturn(Optional.of(nonCoach));

        SubstituteCoachRequest request = substitute(99, "Gán nhầm");

        assertThrows(BusinessException.class, () ->
                sessionService.substituteCoach(normalSession.getId(), request, managerAuth));
    }
    private SessionRescheduleRequest move(LocalDate date,LocalTime start,LocalTime end,Integer room,String reason,Integer days) {
        return new SessionRescheduleRequest(date,start,end,room,reason,days,"CENTER_INCIDENT",true,java.util.UUID.randomUUID().toString(),1,"Minimum extension for the agreed makeup slot");
    }
    private SubstituteCoachRequest substitute(Integer id,String reason) {
        return new SubstituteCoachRequest(id,reason,true,java.util.UUID.randomUUID().toString(),1);
    }
}
