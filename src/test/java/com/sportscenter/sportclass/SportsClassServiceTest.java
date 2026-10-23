package com.sportscenter.sportclass;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.enrollment.EnrollmentService;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SportsClassServiceTest {
    @org.mockito.Spy private java.time.Clock clock = java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private SportsClassRepository repository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock com.sportscenter.session.SessionRepository sessionRepository;
    @Mock com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    @InjectMocks
    private SportsClassService sportsClassService;

    private Subject sampleSubject;
    private User sampleCoach;

    @BeforeEach
    void setUp() {
        sampleSubject = new Subject();
        sampleSubject.setId(1);
        sampleSubject.setName("Quần vợt (Tennis)");
        sampleSubject.setSlug("quan-vot-tennis");
        sampleSubject.setNormalPrice(new BigDecimal("2500000.00"));
        sampleSubject.setAdvancedPrice(new BigDecimal("8000000.00"));

        Role coachRole = new Role();
        coachRole.setName("Coach");

        sampleCoach = new User();
        sampleCoach.setId(10); sampleCoach.setCenterId(1);
        sampleCoach.setFullName("Coach Tuấn");
        sampleCoach.setRole(coachRole);
    }

    @Test
    @DisplayName("Create Normal class auto-assigns Subject.normal_price and totalSessions = 12")
    void create_NormalClass_AutoAssignsNormalPrice() {
        LocalDate startDate = LocalDate.now(clock).plusDays(5);
        LocalDate endDate = startDate.plusMonths(1);
        SportsClassRequest request = new SportsClassRequest(
                "Tennis Nhóm K01", 1, 10, 15, null, startDate, endDate, "Open", "Normal", 12
        );

        when(subjectRepository.findById(1)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));
        when(repository.save(any(SportsClass.class))).thenAnswer(inv -> {
            SportsClass c = inv.getArgument(0);
            c.setId(101);
            return c;
        });

        SportsClassResponse response = sportsClassService.create(request);

        assertNotNull(response);
        assertEquals(101, response.id());
        assertEquals("Normal", response.classType());
        assertEquals(12, response.totalSessions());
        assertEquals(new BigDecimal("2500000.00"), response.tuitionFee(), "Must freeze normal_price from Subject");
        assertEquals(15, response.maxCapacity());
    }

    @Test
    @DisplayName("Create Normal class with capacity > 15 throws BusinessException")
    void create_NormalClass_ExceedCapacity15_ThrowsException() {
        LocalDate startDate = LocalDate.now(clock).plusDays(5);
        SportsClassRequest request = new SportsClassRequest(
                "Tennis Nhóm Lớn", 1, 10, 20, null, startDate, startDate.plusMonths(1), "Open", "Normal", 12
        );

        when(subjectRepository.findById(1)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));

        BusinessException ex = assertThrows(BusinessException.class, () -> sportsClassService.create(request));
        assertTrue(ex.getMessage().contains("tối đa 15 học viên"));
        verify(repository, never()).save(any(SportsClass.class));
    }

    @Test
    @DisplayName("Create Advanced class with capacity != 1 throws BusinessException")
    void create_AdvancedClass_CapacityNotOne_ThrowsException() {
        LocalDate startDate = LocalDate.now(clock).plusDays(5);
        SportsClassRequest request = new SportsClassRequest(
                "Tennis 1:1", 1, 10, 2, null, startDate, startDate.plusMonths(1), "Open", "Advanced", 12
        );

        when(subjectRepository.findById(1)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));

        BusinessException ex = assertThrows(BusinessException.class, () -> sportsClassService.create(request));
        assertTrue(ex.getMessage().contains("sức chứa bằng 1"));
        verify(repository, never()).save(any(SportsClass.class));
    }

    @Test
    @DisplayName("Create Advanced class auto-assigns Subject.advanced_price")
    void create_AdvancedClass_AutoAssignsAdvancedPrice() {
        LocalDate startDate = LocalDate.now(clock).plusDays(5);
        SportsClassRequest request = new SportsClassRequest(
                "Tennis 1:1 VIP", 1, 10, 1, null, startDate, startDate.plusMonths(1), "Open", "Advanced", 12
        );

        when(subjectRepository.findById(1)).thenReturn(Optional.of(sampleSubject));
        when(userRepository.findById(10)).thenReturn(Optional.of(sampleCoach));
        when(repository.save(any(SportsClass.class))).thenAnswer(inv -> {
            SportsClass c = inv.getArgument(0);
            c.setId(102);
            return c;
        });

        SportsClassResponse response = sportsClassService.create(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("8000000.00"), response.tuitionFee(), "Must freeze advanced_price from Subject");
        assertEquals(1, response.maxCapacity());
        assertEquals("Advanced", response.classType());
    }
}

