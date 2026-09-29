package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.room.Room;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

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

    @InjectMocks
    private SessionService sessionService;

    private SportsClass sampleClass;
    private Room sampleRoom;
    private User sampleCoach;

    @BeforeEach
    void setUp() {
        sampleCoach = new User();
        sampleCoach.setId(3);
        sampleCoach.setFullName("Trần Thị Hương");

        sampleClass = new SportsClass();
        sampleClass.setId(1);
        sampleClass.setName("Yoga Buổi Sáng");
        sampleClass.setCoach(sampleCoach);
        sampleClass.setMaxCapacity(20);
        sampleClass.setStatus("Open");

        sampleRoom = new Room();
        sampleRoom.setId(1);
        sampleRoom.setName("Phòng Yoga A");
        sampleRoom.setCapacity(25);
        sampleRoom.setStatus("Available");
    }

    @Test
    @DisplayName("Create session successfully when room and coach are available")
    void createSession_Success() {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(7, 0);
        LocalTime endTime = LocalTime.of(8, 0);
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
        LocalDate futureDate = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(7, 0);
        LocalTime endTime = LocalTime.of(8, 0);
        SessionRequest request = new SessionRequest(1, 1, futureDate, startTime, endTime, "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        Session conflictingSession = new Session();
        conflictingSession.setId(99);
        conflictingSession.setSportsClass(sampleClass);
        conflictingSession.setRoom(sampleRoom);
        conflictingSession.setSessionDate(futureDate);
        conflictingSession.setStartTime(LocalTime.of(6, 30));
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
        LocalDate futureDate = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(7, 0);
        LocalTime endTime = LocalTime.of(8, 0);
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
        conflictingCoachSession.setStartTime(LocalTime.of(7, 0));
        conflictingCoachSession.setEndTime(LocalTime.of(8, 30));

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
        LocalDate futureDate = LocalDate.now().plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(7, 0), LocalTime.of(8, 0), "Scheduled");

        when(sportsClassRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(roomRepository.findById(1)).thenReturn(Optional.of(sampleRoom));

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("Available room"));
    }

    @Test
    @DisplayName("Create session throws error when end time is before start time")
    void createSession_InvalidTimeRange_ThrowsException() {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        SessionRequest request = new SessionRequest(1, 1, futureDate, LocalTime.of(9, 0), LocalTime.of(8, 0), "Scheduled");

        BusinessException ex = assertThrows(BusinessException.class, () -> sessionService.create(request));
        assertTrue(ex.getMessage().contains("End time must be strictly after start time"));
    }
}
