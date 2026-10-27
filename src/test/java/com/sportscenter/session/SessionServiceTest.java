package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.holiday.Holiday;
import com.sportscenter.holiday.HolidayRepository;
import com.sportscenter.room.Room;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
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


    private SportsClass sampleClass;
    private Room sampleRoom;
    private User sampleCoach;

    @BeforeEach
    void setUp() {
        lenient().when(sessionRepository.findCenterIdById(anyInt())).thenReturn(Optional.of(1));
        lenient().when(sportsClassRepository.findCenterIdById(anyInt())).thenReturn(Optional.of(1));
        lenient().when(coachSpecializationRepository.findByCoachUserId(anyInt())).thenAnswer(call -> {
            var a = new com.sportscenter.specialization.CoachSpecialization();
            var spec = new com.sportscenter.specialization.Specialization();
            var subject = new Subject(); subject.setId(1); subject.setOutdoor(false); subject.setOutdoor(false); spec.setSubject(subject); spec.setStatus("Active"); a.setSpecialization(spec);
            var b = new com.sportscenter.specialization.CoachSpecialization();
            var spec2 = new com.sportscenter.specialization.Specialization();
            var subject2 = new Subject(); subject2.setId(2); subject2.setOutdoor(false); spec2.setSubject(subject2); spec2.setStatus("Active"); b.setSpecialization(spec2);
            return List.of(a,b);
        });
        lenient().when(sessionRepository.findByIdForUpdate(anyInt())).thenAnswer(call -> sessionRepository.findByIdWithDetails(call.getArgument(0)));

        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-16T03:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        sampleCoach = new User();
        sampleCoach.setId(3); sampleCoach.setCenterId(1); sampleCoach.setStatus("Active");
        var role = new com.sportscenter.user.Role(); role.setName("Coach"); sampleCoach.setRole(role);
        sampleCoach.setFullName("Trần Thị Hương");

        sampleClass = new SportsClass();
        sampleClass.setId(1); var subject = new Subject(); subject.setId(1); subject.setOutdoor(false); subject.setOutdoor(false); sampleClass.setSubject(subject);
        sampleClass.setName("Yoga Buổi Sáng");
        sampleClass.setCoach(sampleCoach);
        sampleClass.setMaxCapacity(20);
        sampleClass.setStatus("Open");
        sampleClass.setTotalSessions(12);

        sampleRoom = new Room();
        sampleRoom.setId(1);
        sampleRoom.setName("Phòng Yoga A");
        sampleRoom.setCapacity(25);
        sampleRoom.setStatus("Available");
        sampleRoom.setFacilityType("Indoor");
    }

    @Test
    @DisplayName("Create session successfully when room and coach are available with standard slot Ca 1")
    void createSession_Success() {
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(7, 30);
        SessionRequest request = new SessionRequest(1, 1, futureDate, startTime, endTime, "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));
        when(sessionRepository.findRoomConflicts(eq(1), eq(futureDate), eq(startTime), eq(endTime), isNull()))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCoachConflicts(eq(3), eq(futureDate), eq(startTime), eq(endTime), isNull()))
                .thenReturn(Collections.emptyList());

        Session savedSession = new Session();
        savedSession.setId(10);
        savedSession.setSportsClass(sampleClass);
        savedSession.setRoom(sampleRoom);
        savedSession.setSessionDate(futureDate);
        savedSession.setStartTime(startTime);
        savedSession.setEndTime(endTime);
        savedSession.setStatus("Scheduled");

        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);
        when(classEnrollmentRepository.countBySportsClassIdAndStatus(1, "Registered")).thenReturn(5L);

        SessionResponse response = sessionService.create(request);

        assertNotNull(response);
        assertEquals(10, response.id());
        assertEquals("Yoga Buổi Sáng", response.className());
        assertEquals("Phòng Yoga A", response.roomName());
        assertEquals("Trần Thị Hương", response.coachName());
        assertEquals(5, response.enrolledCount());
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("Create session throws error when Room has schedule conflict")
    void createSession_RoomConflict_ThrowsException() {
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(7, 30);
        SessionRequest request = new SessionRequest(1, 1, futureDate, startTime, endTime, "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        Session conflictingSession = new Session();
        conflictingSession.setId(99);
        conflictingSession.setSportsClass(sampleClass);
        conflictingSession.setRoom(sampleRoom);
        conflictingSession.setSessionDate(futureDate);
        conflictingSession.setStartTime(LocalTime.of(6, 0));
        conflictingSession.setEndTime(LocalTime.of(7, 30));

        when(sessionRepository.findRoomConflicts(eq(1), eq(futureDate), eq(startTime), eq(endTime), isNull()))
                .thenReturn(List.of(conflictingSession));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("Trùng phòng"));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Create session throws error when Coach has schedule conflict")
    void createSession_CoachConflict_ThrowsException() {
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(7, 30);
        SessionRequest request = new SessionRequest(1, 1, futureDate, startTime, endTime, "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        when(sessionRepository.findRoomConflicts(eq(1), eq(futureDate), eq(startTime), eq(endTime), isNull()))
                .thenReturn(Collections.emptyList());

        SportsClass otherClass = new SportsClass();
        otherClass.setId(2);
        otherClass.setName("Pilates Sáng");
        otherClass.setCoach(sampleCoach);

        Session conflictingCoachSession = new Session();
        conflictingCoachSession.setId(101);
        conflictingCoachSession.setSportsClass(otherClass);
        conflictingCoachSession.setRoom(sampleRoom);
        conflictingCoachSession.setSessionDate(futureDate);
        conflictingCoachSession.setStartTime(LocalTime.of(6, 0));
        conflictingCoachSession.setEndTime(LocalTime.of(7, 30));

        when(sessionRepository.findCoachConflicts(eq(3), eq(futureDate), eq(startTime), eq(endTime), isNull()))
                .thenReturn(List.of(conflictingCoachSession));
        when(userRepository.findById(3)).thenReturn(Optional.of(sampleCoach));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("Trùng HLV"));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Create session throws error when Room is Maintenance or Closed")
    void createSession_RoomUnavailable_ThrowsException() {
        sampleRoom.setStatus("Maintenance");
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(6, 0), LocalTime.of(7, 30), "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("Available room"));
    }

    @Test
    @DisplayName("Create session throws error when end time is before start time")
    void createSession_InvalidTimeRange_ThrowsException() {
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(9, 0), LocalTime.of(8, 0), "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("End time must be strictly after start time"));
    }

    @Test
    @DisplayName("Create session throws error when slot is non-standard (e.g. 07:00 - 08:00)")
    void createSession_NonStandardSlot_ThrowsException() {
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(7, 0), LocalTime.of(8, 0), "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("không hợp lệ"));
        assertTrue(ex.getMessage().contains("7 ca học chuẩn"));
    }

    @Test
    @DisplayName("Create session throws error when slot is Ca 3 (09:30 - 11:00) on Outdoor facility")
    void createSession_OutdoorHeatSlot3_ThrowsException() {
        sampleRoom.setFacilityType("Outdoor");
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(9, 30), LocalTime.of(11, 0), "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("sốc nhiệt") || ex.getMessage().contains("nắng gắt"));
    }

    @Test
    @DisplayName("Create session throws error when slot is Ca 4 (13:00 - 14:30) on Outdoor facility")
    void createSession_OutdoorHeatSlot4_ThrowsException() {
        sampleRoom.setFacilityType("Outdoor");
        LocalDate futureDate = LocalDate.now(clock).plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(13, 0), LocalTime.of(14, 30), "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("sốc nhiệt") || ex.getMessage().contains("nắng gắt"));
    }

    @Test
    @DisplayName("GetTimeSlots returns all 7 slots for indoor, 5 slots for outdoor")
    void getTimeSlots_FilterOutdoor() {
        List<TimeSlotResponse> allSlots = sessionService.getTimeSlots(false);
        assertEquals(7, allSlots.size());

        List<TimeSlotResponse> outdoorSlots = sessionService.getTimeSlots(true);
        assertEquals(5, outdoorSlots.size());
        assertTrue(outdoorSlots.stream().noneMatch(s -> s.slotNumber() == 3 || s.slotNumber() == 4));
    }

    @Test
    @DisplayName("Preview recurring sessions skips active holidays and collects 12 sessions")
    void previewRecurringSessions_SkipsHolidays() {
        LocalDate startDate = LocalDate.of(2026, 11, 2); // Monday
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(7, 30);
        RecurringPreviewRequest request = new RecurringPreviewRequest(
                1, 1, startDate, List.of(1, 3, 5), startTime, endTime
        );

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        Holiday nov20 = new Holiday();
        nov20.setId(5);
        nov20.setHolidayDate(LocalDate.of(2026, 11, 20)); // Friday
        nov20.setHolidayName("Ngày Nhà giáo Việt Nam");
        nov20.setTag("CENTER_EVENT");
        nov20.setActive(true);

        when(holidayRepository.findByIsActiveTrue()).thenReturn(List.of(nov20));
        when(sessionRepository.findRoomConflicts(anyInt(), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCoachConflicts(anyInt(), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        RecurringPreviewResponse response = sessionService.previewRecurringSessions(request);

        assertNotNull(response);
        assertEquals(12, response.totalSessions());
        assertEquals(12, response.sessions().size());
        assertEquals(1, response.skippedHolidays().size());
        assertEquals(LocalDate.of(2026, 11, 20), response.skippedHolidays().get(0).date());
        assertEquals("Ngày Nhà giáo Việt Nam", response.skippedHolidays().get(0).holidayName());
        assertFalse(response.hasConflict());

        // Verify that 2026-11-20 is NOT in sessions list
        assertTrue(response.sessions().stream().noneMatch(s -> s.date().equals(LocalDate.of(2026, 11, 20))));
    }

    @Test
    @DisplayName("Generate recurring sessions skips holidays, creates 12 sessions and syncs class end date")
    void generateRecurringSessions_Success() {
        LocalDate startDate = LocalDate.of(2026, 11, 2); // Monday
        LocalTime startTime = LocalTime.of(6, 0);
        LocalTime endTime = LocalTime.of(7, 30);
        SessionGenerateRequest request = new SessionGenerateRequest(
                1, 1, startDate, null, List.of(1, 3, 5), startTime, endTime
        );

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        Holiday nov20 = new Holiday();
        nov20.setId(5);
        nov20.setHolidayDate(LocalDate.of(2026, 11, 20));
        nov20.setHolidayName("Ngày Nhà giáo Việt Nam");
        nov20.setActive(true);

        when(holidayRepository.findByIsActiveTrue()).thenReturn(List.of(nov20));
        when(sessionRepository.findRoomConflicts(anyInt(), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findCoachConflicts(anyInt(), any(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        when(sessionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(classEnrollmentRepository.countBySportsClassIdAndStatus(1, "Registered")).thenReturn(0L);

        List<SessionResponse> responses = sessionService.generateRecurringSessions(request);

        assertNotNull(responses);
        assertEquals(12, responses.size());
        assertNotNull(sampleClass.getEndDate());
        verify(sportsClassRepository).save(sampleClass);
        verify(sessionRepository).saveAll(anyList());
    }
}
