package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.holiday.Holiday;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateSessionServiceTest {

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

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private Clock clock;

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Spy
    private SessionSlotValidator sessionSlotValidator = new SessionSlotValidator();

    @InjectMocks
    private SessionService sessionService;
    @Mock private ScheduleWriteLock scheduleWriteLock;
    @Mock private com.sportscenter.user.MemberRepository memberRepository;
    @Mock private com.sportscenter.specialization.CoachSpecializationRepository coachSpecializationRepository;


    private User sampleCoach;
    private User sampleMember;
    private MembershipPackage advancedPackage;
    private MemberPackage activeSubscription;
    private SportsClass advancedClass;
    private Room indoorRoom;
    private Room outdoorRoom;
    private Subject tennisSubject;
    private Subject badmintonSubject;
    private Authentication memberAuth;

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

        // Mock current time: 2026-10-20 08:00:00 Asia/Ho_Chi_Minh
        Instant fixedInstant = LocalDateTime.of(2026, 10, 20, 8, 0, 0)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));

        Role coachRole = new Role();
        coachRole.setId(2);
        coachRole.setName("Coach");

        sampleCoach = new User();
        sampleCoach.setId(10); sampleCoach.setCenterId(1);
        sampleCoach.setFullName("HLV Nguyễn Văn A");
        sampleCoach.setRole(coachRole);
        sampleCoach.setStatus("Active");

        Role memberRole = new Role();
        memberRole.setId(1);
        memberRole.setName("Member");

        sampleMember = new User();
        sampleMember.setId(20); sampleMember.setCenterId(1);
        sampleMember.setEmail("member@example.com");
        sampleMember.setFullName("Học Viên B");
        sampleMember.setRole(memberRole);
        sampleMember.setStatus("Active"); sampleMember.setCenterId(1);

        memberAuth = new UsernamePasswordAuthenticationToken(
                "member@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        lenient().when(userRepository.findByEmailIgnoreCase("member@example.com"))
                .thenReturn(Optional.of(sampleMember));

        advancedPackage = new MembershipPackage();
        advancedPackage.setId(100);
        advancedPackage.setName("Gói Tennis 1:1");
        advancedPackage.setPackageType("Advanced");
        advancedPackage.setTotalCredit(12);
        advancedPackage.setDurationDays(45);
        advancedPackage.setStatus("Active");

        activeSubscription = new MemberPackage();
        activeSubscription.setSubscriptionId(50);
        activeSubscription.setMember(sampleMember);
        activeSubscription.setMembershipPackage(advancedPackage);
        activeSubscription.setRemainingCredit(12);
        activeSubscription.setValidityDays(45);
        activeSubscription.setStartDate(LocalDate.of(2026, 10, 15));
        activeSubscription.setEndDate(LocalDate.of(2026, 11, 29)); // 45 days
        activeSubscription.setStatus("Active");
        lenient().when(memberPackageRepository.findCenterIdById(50)).thenReturn(Optional.of(1));

        tennisSubject = new Subject();
        tennisSubject.setId(1); tennisSubject.setOutdoor(true); tennisSubject.setOutdoor(true);
        tennisSubject.setName("Quần vợt (Tennis)");
        tennisSubject.setSlug("quan-vot-tennis");

        badmintonSubject = new Subject();
        badmintonSubject.setId(2); badmintonSubject.setOutdoor(false);
        badmintonSubject.setName("Cầu lông");
        badmintonSubject.setSlug("cau-long");

        advancedClass = new SportsClass();
        advancedClass.setId(200);
        advancedClass.setName("Tennis 1:1 Nâng Cao");
        advancedClass.setClassType("Advanced");
        advancedClass.setMaxCapacity(1);
        advancedClass.setSubject(tennisSubject);

        outdoorRoom = new Room();
        outdoorRoom.setId(1);
        outdoorRoom.setName("Sân Tennis Ngoài Trời 1");
        outdoorRoom.setFacilityType("Outdoor");
        outdoorRoom.setCapacity(4);
        outdoorRoom.setStatus("Available");

        indoorRoom = new Room();
        indoorRoom.setId(2);
        indoorRoom.setName("Sân Cầu Lông Trong Nhà 1");
        indoorRoom.setFacilityType("Indoor");
        indoorRoom.setCapacity(4);
        indoorRoom.setStatus("Available");
    }

    // =========================================================================
    // 1. GET COACH AVAILABLE SLOTS
    // =========================================================================

    @Test void bookingOutsideSubscriptionDatesIsRejectedBeforeCreditIsChanged() {
        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));
        var request = new PrivateBookingRequest(50,10,1,activeSubscription.getEndDate().plusDays(1),LocalTime.of(6,0),LocalTime.of(7,30),200,1);
        var error = assertThrows(BusinessException.class, () -> sessionService.bookPrivateSession(request, memberAuth));
        assertTrue(error.getMessage().startsWith("ADVANCED_PACKAGE_EXPIRED:"));
        assertEquals(12,activeSubscription.getRemainingCredit()); verify(sessionRepository,never()).save(any());
    }

    @Test void blockedMemberCannotBookEvenWithActiveCredit() {
        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));
        var profile = new com.sportscenter.user.Member(); profile.setBookingBlockedUntil(LocalDateTime.now(clock).plusDays(1));
        when(memberRepository.findById(20)).thenReturn(Optional.of(profile));
        var request = new PrivateBookingRequest(50,10,1,LocalDate.of(2026,10,25),LocalTime.of(6,0),LocalTime.of(7,30),200,1);
        assertTrue(assertThrows(BusinessException.class, () -> sessionService.bookPrivateSession(request,memberAuth)).getMessage().startsWith("BOOKING_BLOCKED:"));
        assertEquals(12,activeSubscription.getRemainingCredit());
    }

    @Test
    @DisplayName("getCoachAvailableSlots: Returns all slots unavailable if date is in the past")
    void getCoachAvailableSlots_DateInPast_AllUnavailable() {
        LocalDate pastDate = LocalDate.of(2026, 10, 19);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));

        List<CoachAvailableSlotResponse> slots = sessionService.getCoachAvailableSlots(10, pastDate, null, null);

        assertEquals(7, slots.size());
        assertTrue(slots.stream().noneMatch(CoachAvailableSlotResponse::isAvailable));
        assertTrue(slots.stream().allMatch(s -> "Ngày trong quá khứ".equals(s.reason())));
    }

    @Test
    @DisplayName("getCoachAvailableSlots: Returns holiday reason if date is an active holiday")
    void getCoachAvailableSlots_ActiveHoliday_AllUnavailable() {
        LocalDate holidayDate = LocalDate.of(2026, 10, 25);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(holidayDate);
        holiday.setHolidayName("Ngày Hội Trung Tâm");
        holiday.setActive(true);
        when(holidayRepository.findByHolidayDate(holidayDate)).thenReturn(Optional.of(holiday));

        List<CoachAvailableSlotResponse> slots = sessionService.getCoachAvailableSlots(10, holidayDate, null, null);

        assertEquals(7, slots.size());
        assertTrue(slots.stream().noneMatch(CoachAvailableSlotResponse::isAvailable));
        assertTrue(slots.stream().allMatch(s -> s.reason().contains("Trung tâm nghỉ lễ: Ngày Hội Trung Tâm")));
    }

    @Test
    @DisplayName("getCoachAvailableSlots: Marks slot < 12h unavailable, outdoor heat restricts Slot 3 and 4")
    void getCoachAvailableSlots_OutdoorHeatAndNoticeRules() {
        // Target date: tomorrow 2026-10-21. Current time is 2026-10-20 08:00.
        // Slot 1 tomorrow is 06:00 (22 hours away -> >= 12h)
        LocalDate targetDate = LocalDate.of(2026, 10, 21);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));
        when(subjectRepository.findById(1)).thenReturn(Optional.of(tennisSubject));
        when(sessionRepository.findCoachConflicts(eq(10), eq(targetDate), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        List<CoachAvailableSlotResponse> slots = sessionService.getCoachAvailableSlots(10, targetDate, 1, null);

        assertEquals(7, slots.size());

        // Ca 1 (06:00-07:30) is outdoor allowed and >= 12h
        CoachAvailableSlotResponse ca1 = slots.get(0);
        assertTrue(ca1.isAvailable());
        assertNull(ca1.reason());

        // Ca 3 (09:30-11:00) is locked for outdoor
        CoachAvailableSlotResponse ca3 = slots.get(2);
        assertFalse(ca3.isAvailable());
        assertTrue(ca3.reason().contains("chống sốc nhiệt"));

        // Ca 4 (13:00-14:30) is locked for outdoor
        CoachAvailableSlotResponse ca4 = slots.get(3);
        assertFalse(ca4.isAvailable());
        assertTrue(ca4.reason().contains("chống sốc nhiệt"));

        // Ca 5 (14:45-16:15) is outdoor allowed and >= 12h
        CoachAvailableSlotResponse ca5 = slots.get(4);
        assertTrue(ca5.isAvailable());
    }

    @Test
    @DisplayName("getCoachAvailableSlots: Marks slot unavailable if coach already has a conflict")
    void getCoachAvailableSlots_CoachConflict_SlotUnavailable() {
        LocalDate targetDate = LocalDate.of(2026, 10, 22);
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));
        when(subjectRepository.findById(2)).thenReturn(Optional.of(badmintonSubject));

        // Mock coach has a conflict on Ca 2 (07:45 - 09:15)
        Session conflictSession = new Session();
        lenient().when(sessionRepository.findCoachConflicts(eq(10), eq(targetDate), any(), any(), isNull()))
                .thenAnswer(inv -> {
                    LocalTime start = inv.getArgument(2);
                    if (start.equals(LocalTime.of(7, 45))) {
                        return List.of(conflictSession);
                    }
                    return Collections.emptyList();
                });

        List<CoachAvailableSlotResponse> slots = sessionService.getCoachAvailableSlots(10, targetDate, 2, null);

        CoachAvailableSlotResponse ca2 = slots.get(1);
        assertFalse(ca2.isAvailable());
        assertEquals("HLV đã có lịch dạy vào khung giờ này", ca2.reason());
    }

    // =========================================================================
    // 2. BOOK PRIVATE SESSION
    // =========================================================================

    @Test
    @DisplayName("bookPrivateSession: Successfully books 1:1 session and deducts 1 credit")
    void bookPrivateSession_Success() {
        // Book 2 days in advance: 2026-10-22 Ca 2 (07:45 - 09:15)
        LocalDate bookingDate = LocalDate.of(2026, 10, 22);
        PrivateBookingRequest request = new PrivateBookingRequest(
                50, 10, 2, bookingDate, LocalTime.of(7, 45), LocalTime.of(9, 15), 200, null);

        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));
        when(roomRepository.findById(2)).thenReturn(Optional.of(indoorRoom));
        when(sportsClassRepository.findById(200)).thenReturn(Optional.of(advancedClass));
        when(sessionRepository.findCoachConflicts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sessionRepository.findRoomConflicts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sessionRepository.findMemberConflicts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            s.setId(999);
            return s;
        });

        SessionResponse response = sessionService.bookPrivateSession(request, memberAuth);

        assertNotNull(response);
        assertEquals(999, response.id());
        assertEquals("Scheduled", response.status());
        assertEquals(1, response.enrolledCount());
        assertTrue(response.isPrivate());
        assertEquals(20, response.bookedMemberId());

        // Verify credit was deducted from 12 to 11
        assertEquals(11, activeSubscription.getRemainingCredit());
        verify(memberPackageRepository).save(activeSubscription);
        verify(auditService).log(eq(20), eq("BOOK_PRIVATE_SESSION"), eq("SESSION"), eq(999), contains("Remaining credit: 11"));
    }

    @Test
    @DisplayName("bookPrivateSession: Fails when remainingCredit = 0")
    void bookPrivateSession_ZeroCredit_ThrowsException() {
        activeSubscription.setRemainingCredit(0);
        LocalDate bookingDate = LocalDate.of(2026, 10, 22);
        PrivateBookingRequest request = new PrivateBookingRequest(
                50, 10, 2, bookingDate, LocalTime.of(7, 45), LocalTime.of(9, 15), 200, null);

        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.bookPrivateSession(request, memberAuth));
        assertTrue(ex.getMessage().contains("Credit = 0"));
    }

    @Test
    @DisplayName("bookPrivateSession: Fails when package validity past 45 days")
    void bookPrivateSession_ExpiredPackage_ThrowsException() {
        // Expired yesterday
        activeSubscription.setEndDate(LocalDate.of(2026, 10, 19));
        LocalDate bookingDate = LocalDate.of(2026, 10, 22);
        PrivateBookingRequest request = new PrivateBookingRequest(
                50, 10, 2, bookingDate, LocalTime.of(7, 45), LocalTime.of(9, 15), 200, null);

        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.bookPrivateSession(request, memberAuth));
        assertTrue(ex.getMessage().contains("hết hạn sử dụng 45 ngày"));
    }

    @Test
    @DisplayName("bookPrivateSession: Fails when booking notice is less than 12 hours")
    void bookPrivateSession_LessThan12HoursNotice_ThrowsException() {
        // Current time is 2026-10-20 08:00. Trying to book today at 14:45 (6h45m away < 12h)
        LocalDate bookingDate = LocalDate.of(2026, 10, 20);
        PrivateBookingRequest request = new PrivateBookingRequest(
                50, 10, 2, bookingDate, LocalTime.of(14, 45), LocalTime.of(16, 15), 200, null);

        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.bookPrivateSession(request, memberAuth));
        assertTrue(ex.getMessage().contains("tối thiểu 12 tiếng"));
    }

    @Test
    @DisplayName("bookPrivateSession: Fails when member tries to book with someone else's package")
    void bookPrivateSession_NotOwner_ThrowsAccessDenied() {
        User otherMember = new User();
        otherMember.setId(999);
        activeSubscription.setMember(otherMember);

        PrivateBookingRequest request = new PrivateBookingRequest(
                50, 10, 2, LocalDate.of(2026, 10, 22), LocalTime.of(7, 45), LocalTime.of(9, 15), 200, null);

        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));

        assertThrows(AccessDeniedException.class,
                () -> sessionService.bookPrivateSession(request, memberAuth));
    }

    @Test
    @DisplayName("bookPrivateSession: Fails when selecting non-advanced class")
    void bookPrivateSession_NonAdvancedClass_ThrowsException() {
        SportsClass normalClass = new SportsClass();
        normalClass.setId(300);
        normalClass.setClassType("Normal");

        LocalDate bookingDate = LocalDate.of(2026, 10, 22);
        PrivateBookingRequest request = new PrivateBookingRequest(
                50, 10, 2, bookingDate, LocalTime.of(7, 45), LocalTime.of(9, 15), 300, null);

        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));
        when(roomRepository.findById(2)).thenReturn(Optional.of(indoorRoom));
        when(sportsClassRepository.findById(300)).thenReturn(Optional.of(normalClass));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.bookPrivateSession(request, memberAuth));
        assertTrue(ex.getMessage().contains("không phải khóa học 1:1 Advanced"));
    }

    // =========================================================================
    // 3. CANCEL PRIVATE SESSION
    // =========================================================================

    @Test
    @DisplayName("cancelPrivateSession: Success >= 24h, sets Cancelled and refunds 1 credit")
    void cancelPrivateSession_SuccessWithRefund() {
        // Session is 48 hours away: 2026-10-22 08:00
        Session session = new Session(); session.setSportsClass(advancedClass);
        session.setId(777);
        session.setSessionDate(LocalDate.of(2026, 10, 22));
        session.setStartTime(LocalTime.of(7, 45));
        session.setEndTime(LocalTime.of(9, 15));
        session.setStatus("Scheduled");
        session.setBookedMember(sampleMember);
        session.setMemberPackage(activeSubscription);
        session.setSportsClass(advancedClass);
        session.setRoom(indoorRoom);

        activeSubscription.setRemainingCredit(11); // After booking

        when(sessionRepository.findByIdForUpdate(777)).thenReturn(Optional.of(session));
        when(memberPackageRepository.findByIdForUpdate(50)).thenReturn(Optional.of(activeSubscription));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = sessionService.cancelPrivateSession(777, memberAuth);

        assertEquals("Cancelled", response.status());
        assertEquals("Cancelled", session.getStatus());
        // Verify credit was refunded from 11 to 12
        assertEquals(12, activeSubscription.getRemainingCredit());
        verify(memberPackageRepository).save(activeSubscription);
        verify(auditService).log(eq(20), eq("CANCEL_PRIVATE_SESSION"), eq("SESSION"), eq(777), contains("Refunded 1 credit"));
    }

    @Test
    @DisplayName("cancelPrivateSession: Fails with CANCEL_TOO_LATE when cancelling under 24 hours")
    void cancelPrivateSession_Under24Hours_ThrowsCancelTooLate() {
        // Session is tomorrow morning 2026-10-21 07:45 (23h45m away < 24h)
        Session session = new Session(); session.setSportsClass(advancedClass);
        session.setId(778);
        session.setSessionDate(LocalDate.of(2026, 10, 21));
        session.setStartTime(LocalTime.of(7, 45));
        session.setEndTime(LocalTime.of(9, 15));
        session.setStatus("Scheduled");
        session.setBookedMember(sampleMember);
        session.setMemberPackage(activeSubscription);

        when(sessionRepository.findByIdForUpdate(778)).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.cancelPrivateSession(778, memberAuth));
        assertTrue(ex.getMessage().startsWith("CANCEL_TOO_LATE"));
        // Ensure status was NOT changed and credit was NOT refunded
        assertEquals("Scheduled", session.getStatus());
        verify(memberPackageRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelPrivateSession: Fails if session is not a private session")
    void cancelPrivateSession_NotPrivateSession_ThrowsException() {
        SportsClass groupClass = new SportsClass();
        groupClass.setClassType("Normal");

        Session session = new Session(); session.setSportsClass(advancedClass);
        session.setId(779);
        session.setSessionDate(LocalDate.of(2026, 10, 25));
        session.setStartTime(LocalTime.of(7, 45));
        session.setEndTime(LocalTime.of(9, 15));
        session.setStatus("Scheduled");
        session.setBookedMember(null); // No booked member
        session.setSportsClass(groupClass);

        when(sessionRepository.findByIdForUpdate(779)).thenReturn(Optional.of(session));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.cancelPrivateSession(779, memberAuth));
        assertTrue(ex.getMessage().contains("Chỉ có thể hủy lịch riêng cho ca học 1:1"));
    }
}
