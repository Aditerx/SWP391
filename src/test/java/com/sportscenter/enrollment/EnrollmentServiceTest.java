package com.sportscenter.enrollment;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User sampleMember;
    private SportsClass sampleClass;
    private MemberPackage sampleActivePackage;

    @BeforeEach
    void setUp() {
        sampleMember = new User();
        sampleMember.setId(8);
        sampleMember.setFullName("Hoàng Thị Oanh");
        sampleMember.setEmail("oanh.hoang@fitzone.vn");
        sampleMember.setStatus("Active");

        sampleClass = new SportsClass();
        sampleClass.setId(1);
        sampleClass.setName("Yoga Buổi Sáng");
        sampleClass.setMaxCapacity(20);
        sampleClass.setStatus("Open");

        sampleActivePackage = new MemberPackage();
        sampleActivePackage.setSubscriptionId(1);
        sampleActivePackage.setMember(sampleMember);
        sampleActivePackage.setStatus("Active");
        sampleActivePackage.setStartDate(LocalDate.now().minusDays(10));
        sampleActivePackage.setEndDate(LocalDate.now().plusDays(20));
    }

    @Test
    @DisplayName("Enroll member successfully when active package and capacity available")
    void enroll_Success() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Registered")).thenReturn(10L);
        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.empty());

        ClassEnrollment saved = new ClassEnrollment();
        saved.setId(50);
        saved.setMember(sampleMember);
        saved.setSportsClass(sampleClass);
        saved.setStatus("Registered");
        saved.setEnrolledAt(LocalDateTime.now());

        when(enrollmentRepository.save(any(ClassEnrollment.class))).thenReturn(saved);

        EnrollmentResponse response = enrollmentService.enroll(1, 8, "admin@scms.com");

        assertNotNull(response);
        assertEquals(50, response.id());
        assertEquals("Hoàng Thị Oanh", response.memberName());
        assertEquals("Yoga Buổi Sáng", response.className());
        assertEquals("Registered", response.status());
        verify(enrollmentRepository).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Enroll throws exception when Member has no active membership package")
    void enroll_NoActivePackage_ThrowsException() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                enrollmentService.enroll(1, 8, "admin@scms.com"));
        assertTrue(ex.getMessage().contains("active membership package"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Enroll throws exception when Class is full")
    void enroll_ClassFull_ThrowsException() {
        sampleClass.setMaxCapacity(15);
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Registered")).thenReturn(15L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                enrollmentService.enroll(1, 8, "admin@scms.com"));
        assertTrue(ex.getMessage().contains("Class is full"));
        verify(enrollmentRepository, never()).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Enroll throws exception when Member is already registered")
    void enroll_AlreadyRegistered_ThrowsException() {
        when(userRepository.findById(8)).thenReturn(Optional.of(sampleMember));
        when(memberPackageRepository.findByMemberId(8)).thenReturn(List.of(sampleActivePackage));
        when(classRepository.findById(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatus(1, "Registered")).thenReturn(5L);

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
    @DisplayName("Cancel enrollment successfully")
    void cancelEnrollment_Success() {
        ClassEnrollment existing = new ClassEnrollment();
        existing.setId(50);
        existing.setMember(sampleMember);
        existing.setSportsClass(sampleClass);
        existing.setStatus("Registered");

        when(enrollmentRepository.findByMemberIdAndSportsClassId(8, 1)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(existing)).thenReturn(existing);

        EnrollmentResponse response = enrollmentService.cancelEnrollment(1, 8, "admin@scms.com");

        assertNotNull(response);
        assertEquals("Cancelled", existing.getStatus());
        verify(enrollmentRepository).save(existing);
    }
}
