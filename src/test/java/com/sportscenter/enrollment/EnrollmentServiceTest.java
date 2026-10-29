package com.sportscenter.enrollment;

import com.sportscenter.audit.AuditService;
import com.sportscenter.invoice.InvoiceService;
import com.sportscenter.invoice.InvoiceOrderResponse;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.notification.NotificationService;
import com.sportscenter.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private ClassEnrollmentRepository enrollmentRepository;

    @Mock
    private SportsClassRepository classRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Clock clock;

    @InjectMocks
    private EnrollmentService enrollmentService;
    @Mock private com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;

    private User sampleMember;
    private User sampleAdmin;
    private SportsClass sampleClass;
    private MemberPackage sampleActivePackage;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        sampleMember = new User();
        sampleMember.setId(8); sampleMember.setCenterId(1);
        sampleMember.setFullName("Hoàng Thị Oanh");
        sampleMember.setEmail("oanh.hoang@fitzone.vn");
        sampleMember.setStatus("Active");
        Role memberRole = new Role();
        memberRole.setName("Member");
        sampleMember.setRole(memberRole);
        lenient().when(userRepository.findByEmailIgnoreCase(sampleMember.getEmail())).thenReturn(Optional.of(sampleMember));

        sampleAdmin = new User();
        sampleAdmin.setId(1);
        sampleAdmin.setEmail("admin@scms.com");
        Role adminRole = new Role();
        adminRole.setName("Admin");
        sampleAdmin.setRole(adminRole);
        lenient().when(userRepository.findByEmailIgnoreCase("admin@scms.com")).thenReturn(Optional.of(sampleAdmin));

        sampleClass = new SportsClass();
        sampleClass.setId(1);
        sampleClass.setName("Cầu lông Buổi Sáng");
        sampleClass.setMaxCapacity(20);
        sampleClass.setStatus("Open");
        sampleClass.setClassType("Advanced");

        sampleActivePackage = new MemberPackage();
        sampleActivePackage.setSubscriptionId(1);
        sampleActivePackage.setMember(sampleMember);
        sampleActivePackage.setStatus("Active");
        sampleActivePackage.setStartDate(LocalDate.now(clock).minusDays(10));
        sampleActivePackage.setEndDate(LocalDate.now(clock).plusDays(20));
    }

    @Test
    @DisplayName("Enroll member successfully when active package and capacity available")
    void enroll_Success() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(10L);
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());

        ClassEnrollment saved = new ClassEnrollment();
        saved.setId(50);
        saved.setMember(sampleMember);
        saved.setSportsClass(sampleClass);
        saved.setStatus("Registered");
        saved.setEnrolledAt(LocalDateTime.now(clock));

        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenReturn(saved);

        EnrollmentResponse response = enrollmentService.enroll(1, 8, "admin@scms.com");

        assertNotNull(response);
        assertEquals(50, response.id());
        assertEquals("Hoàng Thị Oanh", response.memberName());
        assertEquals("Cầu lông Buổi Sáng", response.className());
        assertEquals("Registered", response.status());
        verify(enrollmentRepository).save(any(ClassEnrollment.class));
        verify(classRepository).findByIdForUpdate(1);
    }

    @Test
    @DisplayName("Enroll throws exception when Member has no active membership package")
    void enroll_NoActivePackage_ThrowsException() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                enrollmentService.enroll(1, 8, "admin@scms.com"));
        assertTrue(ex.getMessage().startsWith("PACKAGE_EXPIRED"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Re-enrolling after cancellation resets cancellation data and enrollment time")
    void enroll_ReactivatesCancelledRecordAndResetsCancellationFields() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Waitlisted")).thenReturn(0L);
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(0L);
        User cancelledBy = new User(); cancelledBy.setId(9);
        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(85); existing.setMember(sampleMember); existing.setSportsClass(sampleClass);
        existing.setStatus("Cancelled"); existing.setCancelledAt(LocalDateTime.now(clock).minusDays(1));
        existing.setCancelledBy(cancelledBy); existing.setCancelReason("old reason");
        existing.setEnrolledAt(LocalDateTime.now(clock).minusDays(10));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(existing)).thenReturn(existing);

        enrollmentService.enroll(1, 8, "admin@scms.com");

        assertEquals("Registered", existing.getStatus());
        assertNull(existing.getCancelledAt());
        assertNull(existing.getCancelledBy());
        assertNull(existing.getCancelReason());
        assertEquals(LocalDateTime.now(clock), existing.getEnrolledAt());
    }

    @Test
    @DisplayName("Enroll adds member to waitlist when Class is full")
    void enroll_ClassFull_AddsToWaitlist() {
        sampleClass.setMaxCapacity(15);
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(15L);
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Waitlisted")).thenReturn(0L);
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment enrollment = invocation.getArgument(0);
            enrollment.setId(61);
            return enrollment;
        });

        EnrollmentResponse response = enrollmentService.enroll(1, 8, "admin@scms.com");
        assertEquals("Waitlisted", response.status());
        assertEquals(1, response.waitlistPosition());
        verify(enrollmentRepository).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Enroll throws exception when Member is already registered")
    void enroll_AlreadyRegistered_ThrowsException() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(50);
        existing.setMember(sampleMember);
        existing.setSportsClass(sampleClass);
        existing.setStatus("Registered");

        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                enrollmentService.enroll(1, 8, "admin@scms.com"));
        assertTrue(ex.getMessage().contains("already enrolled"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Member with an active booking block cannot enroll")
    void enroll_BookingBlocked_ThrowsStableCode() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        com.sportscenter.user.Member profile = new com.sportscenter.user.Member();
        profile.setBookingBlockedUntil(LocalDateTime.now(clock).plusDays(2));
        when(memberRepository.findById(8)).thenReturn(Optional.of(profile));

        BusinessException error = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(1, 8, "admin@scms.com"));

        assertTrue(error.getMessage().startsWith("BOOKING_BLOCKED:"));
        verify(memberPackageRepository, never()).findByMemberId(8);
    }

    @Test
    @DisplayName("Enrollment rejects overlapping future sessions in another registered class")
    void enroll_ScheduleConflict_ThrowsException() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());
        Session clash = new Session();
        SportsClass otherClass = new SportsClass();
        otherClass.setId(4);
        clash.setSportsClass(otherClass);
        clash.setSessionDate(LocalDate.of(2026, 10, 20));
        clash.setStartTime(java.time.LocalTime.of(10, 0));
        clash.setEndTime(java.time.LocalTime.of(11, 0));
        when(sessionRepository.findFutureEnrollmentScheduleConflicts(eq(8), eq(1), any(), any()))
                .thenReturn(List.of(clash));

        BusinessException error = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(1, 8, "admin@scms.com"));
        assertTrue(error.getMessage().startsWith("SCHEDULE_CONFLICT"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Full class adds the next member to the waitlist with position")
    void enroll_FullClass_WaitlistsMember() {
        sampleClass.setMaxCapacity(1);
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Waitlisted")).thenReturn(0L);
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(1L);
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment saved = invocation.getArgument(0);
            saved.setId(60);
            return saved;
        });

        EnrollmentResponse response = enrollmentService.enroll(1, 8, "admin@scms.com");

        assertEquals("Waitlisted", response.status());
        assertEquals(1, response.waitlistPosition());
        verify(enrollmentRepository).save(argThat(e -> "Waitlisted".equals(e.getStatus())));
    }

    @Test
    @DisplayName("Competing requests serialize through the locked class row")
    void enroll_CompetingRequests_LastSeatIsReservedOnce() {
        sampleClass.setMaxCapacity(1);
        User secondMember = activeMember(9); secondMember.setCenterId(1);
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findById(9)).thenReturn(Optional.of(secondMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(memberPackageRepository.findByMemberId(9)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending")))
                .thenReturn(0L, 1L);
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Waitlisted")).thenReturn(0L, 0L);
        when(enrollmentRepository.findByMemberIdAndSportsClassId(anyInt(), eq(1))).thenReturn(Optional.empty());
        when(sessionRepository.findFutureEnrollmentScheduleConflicts(anyInt(), eq(1), any(), any())).thenReturn(List.of());
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(call -> {
            ClassEnrollment enrollment = call.getArgument(0);
            enrollment.setId(100 + enrollment.getMember().getId());
            return enrollment;
        });

        EnrollmentResponse first = enrollmentService.enroll(1, 8, "admin@scms.com");
        EnrollmentResponse second = enrollmentService.enroll(1, 9, "admin@scms.com");

        assertEquals("Registered", first.status());
        assertEquals("Waitlisted", second.status());
        verify(classRepository, times(2)).findByIdForUpdate(1);
        // Mockito serializes the calls; this verifies the lock path, not a real database race.
    }

    @Test
    @DisplayName("Waitlist rejects an eleventh member")
    void enroll_WaitlistFull_ThrowsException() {
        sampleClass.setMaxCapacity(1);
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Waitlisted")).thenReturn(10L);
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class,
                () -> enrollmentService.enroll(1, 8, "admin@scms.com"));
        assertTrue(error.getMessage().startsWith("WAITLIST_FULL"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Staff cancellation with a reason succeeds and skips the two hour rule")
    void cancelEnrollment_Success() {
        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(50);
        existing.setMember(sampleMember);
        existing.setSportsClass(sampleClass);
        existing.setStatus("Registered");

        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(existing)).thenReturn(existing);

        EnrollmentResponse response = enrollmentService.cancelEnrollment(1, 8, "admin@scms.com", "staff cancellation");

        assertNotNull(response);
        assertEquals("Cancelled", existing.getStatus());
        verify(enrollmentRepository).save(existing);
    }

    @Test
    @DisplayName("Cancel by id rejects an already completed enrollment with the stable code")
    void cancelById_Completed_ThrowsAlreadyCancelled() {
        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(51);
        existing.setMember(sampleMember);
        existing.setSportsClass(sampleClass);
        existing.setStatus("Completed");
        when(enrollmentRepository.findById(51)).thenReturn(Optional.of(existing));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));

        BusinessException error = assertThrows(BusinessException.class,
                () -> enrollmentService.cancelById(51, "admin@scms.com", "reason"));

        assertTrue(error.getMessage().startsWith("ALREADY_CANCELLED:"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Promote eligible waitlisted member into the class state and notify them")
    void promoteWaitlist_EligibleMember_PromotesAndNotifies() {
        ClassEnrollment waiting = new ClassEnrollment();
        waiting.setId(70);
        waiting.setMember(sampleMember);
        waiting.setSportsClass(sampleClass);
        waiting.setStatus("Waitlisted");
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(0L);
        when(enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(1, "Waitlisted"))
                .thenReturn(List.of(waiting));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(sessionRepository.findFutureEnrollmentScheduleConflicts(eq(8), eq(1), any(), any())).thenReturn(List.of());
        when(enrollmentRepository.save(waiting)).thenReturn(waiting);
        doThrow(new IllegalStateException("notification storage unavailable"))
                .when(notificationService).send(eq(8), anyString(), anyString(), eq("Schedule"));

        assertDoesNotThrow(() -> enrollmentService.promoteWaitlist(1, sampleAdmin.getId()));

        assertEquals("Registered", waiting.getStatus());
        assertEquals(LocalDateTime.now(clock), waiting.getPromotedAt());
        verify(notificationService).send(8, "Waitlist update",
                "A place opened in " + sampleClass.getName() + ". Your status is Registered.", "Schedule");
        verify(auditService).log(sampleAdmin.getId(), "PROMOTE_WAITLIST", "CLASS_ENROLLMENT", 70,
                "Promoted member 8 from waitlist for class 1");
    }

    @Test
    @DisplayName("Promotion skips an expired package without removing the member from the waitlist")
    void promoteWaitlist_SkipsExpiredPackageAndPromotesNextEligibleMember() {
        sampleClass.setMaxCapacity(2);
        User expiredUser = activeMember(18);
        User firstEligibleUser = activeMember(19);
        User beyondCapacityUser = activeMember(20);
        ClassEnrollment expired = waiting(81, expiredUser);
        ClassEnrollment firstEligible = waiting(82, firstEligibleUser);
        ClassEnrollment beyondCapacity = waiting(83, beyondCapacityUser);
        MemberPackage expiredPackage = new MemberPackage();
        expiredPackage.setStatus("Active");
        expiredPackage.setEndDate(LocalDate.of(2026, 10, 15));
        MemberPackage validPackage = new MemberPackage();
        validPackage.setStatus("Active");
        validPackage.setEndDate(LocalDate.of(2026, 11, 30));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(1L);
        when(enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(1, "Waitlisted"))
                .thenReturn(List.of(expired, firstEligible, beyondCapacity));
        when(memberPackageRepository.findByMemberId(18)).thenReturn(List.of(expiredPackage));
        when(memberPackageRepository.findByMemberId(19)).thenReturn(List.of(validPackage));
        when(sessionRepository.findFutureEnrollmentScheduleConflicts(eq(19), eq(1), any(), any())).thenReturn(List.of());
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(call -> call.getArgument(0));

        enrollmentService.promoteWaitlist(1, sampleAdmin.getId());

        assertEquals("Waitlisted", expired.getStatus());
        assertEquals("Registered", firstEligible.getStatus());
        assertEquals("Waitlisted", beyondCapacity.getStatus());
        verify(enrollmentRepository, times(1)).save(any(ClassEnrollment.class));
        verify(notificationService, times(1)).send(eq(19), anyString(), anyString(), eq("Schedule"));
    }

    @Test
    @DisplayName("Promotion gives a paid class the Pending state")
    void promoteWaitlist_PaidClass_PromotesAsPending() {
        sampleClass.setTuitionFee(new BigDecimal("50.00"));
        ClassEnrollment waiting = waiting(84, sampleMember);
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(0L);
        when(enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(1, "Waitlisted"))
                .thenReturn(List.of(waiting));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(sessionRepository.findFutureEnrollmentScheduleConflicts(eq(8), eq(1), any(), any())).thenReturn(List.of());
        when(enrollmentRepository.save(waiting)).thenReturn(waiting);

        enrollmentService.promoteWaitlist(1, sampleAdmin.getId());

        assertEquals("Pending", waiting.getStatus());
    }

    @Test
    @DisplayName("Promotion leaves a member with an active booking block on the waitlist")
    void promoteWaitlist_BookingBlockedMember_IsSkipped() {
        ClassEnrollment waiting = waiting(86, sampleMember);
        com.sportscenter.user.Member profile = new com.sportscenter.user.Member();
        profile.setBookingBlockedUntil(LocalDateTime.now(clock).plusDays(1));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(0L);
        when(enrollmentRepository.findBySportsClassIdAndStatusOrderByEnrolledAtAscIdAsc(1, "Waitlisted"))
                .thenReturn(List.of(waiting));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(memberRepository.findById(8)).thenReturn(Optional.of(profile));

        enrollmentService.promoteWaitlist(1, sampleAdmin.getId());

        assertEquals("Waitlisted", waiting.getStatus());
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
        verifyNoInteractions(notificationService);
    }

    private User activeMember(Integer id) {
        User member = new User();
        member.setId(id);
        member.setStatus("Active");
        return member;
    }

    private ClassEnrollment waiting(Integer id, User member) {
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setId(id);
        enrollment.setMember(member);
        enrollment.setSportsClass(sampleClass);
        enrollment.setStatus("Waitlisted");
        enrollment.setEnrolledAt(LocalDateTime.now(clock).minusMinutes(id));
        return enrollment;
    }

    @Test
    @DisplayName("Member may cancel when the next session is two hours and one minute away")
    void cancelEnrollment_AtTwoHoursOneMinute_IsAllowed() {
        assertMemberCancellationAt(LocalDate.of(2026, 10, 16), java.time.LocalTime.of(12, 1));
    }

    @Test
    @DisplayName("Member may cancel exactly two hours before the next session")
    void cancelEnrollment_AtExactlyTwoHours_IsAllowed() {
        assertMemberCancellationAt(LocalDate.of(2026, 10, 16), java.time.LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("Member cancellation one hour and fifty nine minutes before class is rejected")
    void cancelEnrollment_AtOneHourFiftyNineMinutes_TooLate() {
        ClassEnrollment existing = registeredEnrollment();
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));
        when(sessionRepository.findNextUpcomingForClass(eq(1), any(), any())).thenReturn(List.of(sessionAt(11, 59)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> enrollmentService.cancelEnrollment(1, 8, sampleMember.getEmail()));

        assertTrue(error.getMessage().startsWith("CANCEL_TOO_LATE:"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Staff cancellation requires a reason")
    void cancelEnrollment_StaffMissingReason_IsRejected() {
        ClassEnrollment existing = registeredEnrollment();
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class,
                () -> enrollmentService.cancelEnrollment(1, 8, "admin@scms.com"));
        verify(sessionRepository, never()).findNextUpcomingForClass(anyInt(), any(), any());
    }

    @Test
    @DisplayName("Waitlisted member may cancel without the two hour rule")
    void cancelEnrollment_Waitlisted_IsAlwaysAllowedForMember() {
        ClassEnrollment existing = registeredEnrollment();
        existing.setStatus("Waitlisted");
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(existing)).thenReturn(existing);

        EnrollmentResponse response = enrollmentService.cancelEnrollment(1, 8, sampleMember.getEmail());

        assertEquals("Cancelled", response.status());
        verify(sessionRepository, never()).findNextUpcomingForClass(anyInt(), any(), any());
    }

    private void assertMemberCancellationAt(LocalDate date, java.time.LocalTime startTime) {
        ClassEnrollment existing = registeredEnrollment();
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(existing)).thenReturn(existing);
        when(sessionRepository.findNextUpcomingForClass(eq(1), any(), any())).thenReturn(List.of(sessionAt(date, startTime)));

        EnrollmentResponse response = enrollmentService.cancelEnrollment(1, 8, sampleMember.getEmail());
        assertEquals("Cancelled", response.status());
    }

    private ClassEnrollment registeredEnrollment() {
        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(52);
        existing.setMember(sampleMember);
        existing.setSportsClass(sampleClass);
        existing.setStatus("Registered");
        return existing;
    }

    private Session sessionAt(int hour, int minute) {
        return sessionAt(LocalDate.of(2026, 10, 16), java.time.LocalTime.of(hour, minute));
    }

    private Session sessionAt(LocalDate date, java.time.LocalTime startTime) {
        Session session = new Session();
        session.setSessionDate(date);
        session.setStartTime(startTime);
        return session;
    }

    @Test
    @DisplayName("Member cannot enroll another member")
    void enroll_MemberCannotTargetAnotherMember() {
        Role memberRole = new Role();
        memberRole.setName("Member");
        sampleMember.setRole(memberRole);
        when(userRepository.findByEmailIgnoreCase(sampleMember.getEmail())).thenReturn(Optional.of(sampleMember));

        assertThrows(AccessDeniedException.class,
                () -> enrollmentService.enroll(1, 9, sampleMember.getEmail()));
        verifyNoInteractions(memberPackageRepository);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("Paid class tuition holds capacity with a Pending enrollment and invoice")
    void enroll_PaidClassCreatesPendingInvoiceAndReservation() {
        sampleClass.setTuitionFee(new BigDecimal("125.00"));
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(2L);
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
            ClassEnrollment e = invocation.getArgument(0); e.setId(77); return e;
        });
        when(invoiceService.createClassOrder(sampleMember, sampleClass, "VNPay", "203.0.113.5"))
                .thenAnswer(call -> new InvoiceOrderResponse(88, "INV-2026-000088", new BigDecimal("125.00"), "VNPay", "Pending",
                        LocalDateTime.now(clock).plusMinutes(15), "https://vnpay.test/pay"));

        EnrollmentResponse response = enrollmentService.enroll(1, 8, "admin@scms.com", "VNPay", "203.0.113.5");

        assertEquals("Pending", response.status());
        assertEquals(88, response.invoiceId());
        assertEquals("Pending", response.paymentStatus());
        assertEquals("https://vnpay.test/pay", response.paymentUrl());
    }

    @Test
    @DisplayName("Member cancellation for Normal class is strictly blocked")
    void cancelEnrollment_NormalClass_IsStrictlyBlocked() {
        SportsClass normalClass = new SportsClass();
        normalClass.setId(2);
        normalClass.setName("Normal Badminton");
        normalClass.setClassType("Normal");

        ClassEnrollment existing = registeredEnrollment();
        existing.setSportsClass(normalClass);

        when(classRepository.findByIdForUpdate(2)).thenReturn(Optional.of(normalClass));
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 2)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> enrollmentService.cancelEnrollment(2, 8, sampleMember.getEmail()));
        assertTrue(ex.getMessage().contains("NORMAL_CLASS_NON_CANCELLABLE"));
    }

    @Test void inactiveSubjectCannotSellAnotherClassRegistration() {
        var subject = new com.sportscenter.subject.Subject(); subject.setStatus("Inactive"); sampleClass.setSubject(subject);
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        assertTrue(assertThrows(BusinessException.class, () -> enrollmentService.enroll(1,8,"admin@scms.com")).getMessage().startsWith("SUBJECT_INACTIVE:"));
        verify(enrollmentRepository,never()).save(any()); verifyNoInteractions(invoiceService);
    }
}
