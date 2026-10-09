package com.sportscenter.enrollment;

import com.sportscenter.audit.AuditService;
import com.sportscenter.invoice.InvoiceService;
import com.sportscenter.invoice.InvoiceOrderResponse;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
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

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User sampleMember;
    private User sampleAdmin;
    private SportsClass sampleClass;
    private MemberPackage sampleActivePackage;

    @BeforeEach
    void setUp() {
        sampleMember = new User();
        sampleMember.setId(8);
        sampleMember.setFullName("Hoàng Thị Oanh");
        sampleMember.setEmail("oanh.hoang@fitzone.vn");
        sampleMember.setStatus("Active");
        Role memberRole = new Role();
        memberRole.setName("Member");
        sampleMember.setRole(memberRole);

        sampleAdmin = new User();
        sampleAdmin.setId(1);
        sampleAdmin.setEmail("admin@scms.com");
        Role adminRole = new Role();
        adminRole.setName("Admin");
        sampleAdmin.setRole(adminRole);
        lenient().when(userRepository.findByEmailIgnoreCase("admin@scms.com")).thenReturn(Optional.of(sampleAdmin));

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
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(10L);
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
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(15L);

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
        when(classRepository.findByIdForUpdate(1)).thenReturn(Optional.of(sampleClass));
        when(enrollmentRepository.countBySportsClassIdAndStatusIn(1, List.of("Registered", "Pending"))).thenReturn(5L);

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
                .thenReturn(new InvoiceOrderResponse(88, "INV-2026-000088", new BigDecimal("125.00"), "VNPay", "Pending",
                        LocalDateTime.now().plusMinutes(15), "https://vnpay.test/pay"));

        EnrollmentResponse response = enrollmentService.enroll(1, 8, "admin@scms.com", "VNPay", "203.0.113.5");

        assertEquals("Pending", response.status());
        assertEquals(88, response.invoiceId());
        assertEquals("Pending", response.paymentStatus());
        assertEquals("https://vnpay.test/pay", response.paymentUrl());
    }
}
