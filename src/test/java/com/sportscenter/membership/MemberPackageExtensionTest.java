package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberPackageExtensionTest {
    @Mock private com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    @org.mockito.Spy private java.time.Clock clock = java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MemberPackageService memberPackageService;

    private User managerUser;
    private Authentication managerAuth;
    private MemberPackage subscription;

    @BeforeEach
    void setUp() {
        lenient().when(memberPackageRepository.findCenterIdById(anyInt())).thenReturn(Optional.of(1));
        managerUser = new User();
        managerUser.setId(1);
        managerUser.setEmail("manager@sportscenter.com");
        managerAuth = new UsernamePasswordAuthenticationToken(managerUser.getEmail(), null);

        User member = new User();
        member.setId(20);
        member.setFullName("Nguyễn Văn A");

        MembershipPackage mp = new MembershipPackage();
        mp.setId(2);
        mp.setName("Gói 1:1 Tennis");
        mp.setPackageType("Advanced");

        subscription = new MemberPackage();
        subscription.setSubscriptionId(101);
        subscription.setMember(member);
        subscription.setMembershipPackage(mp);
        subscription.setStartDate(LocalDate.of(2026, 10, 1));
        subscription.setEndDate(LocalDate.of(2026, 11, 15));
        subscription.setValidityDays(45);
        subscription.setRemainingCredit(6);
        subscription.setStatus("Active");
    }

    @Test
    @DisplayName("Manager gia hạn gói thành công thêm 15 ngày")
    void extendValidity_Success() {
        when(userRepository.findByEmailIgnoreCase(managerUser.getEmail())).thenReturn(Optional.of(managerUser));
        when(memberPackageRepository.findByIdForUpdate(101)).thenReturn(Optional.of(subscription));
        when(memberPackageRepository.save(any(MemberPackage.class))).thenAnswer(i -> i.getArgument(0));

        PackageExtensionRequest request = new PackageExtensionRequest(15, "Bù thời gian do trung tâm sửa chữa sân");

        MemberPackageResponse response = memberPackageService.extendValidity(101, request, managerAuth);

        assertNotNull(response);
        assertEquals(LocalDate.of(2026, 11, 30), response.endDate());
        assertEquals(60, response.validityDays());
        assertEquals("Active", response.status());

        verify(auditService).log(eq(managerUser.getId()), eq("EXTEND_PACKAGE_VALIDITY"), eq("MEMBER_PACKAGE"), eq(101),
                contains("Bù thời gian do trung tâm sửa chữa sân"));
    }

    @Test
    @DisplayName("Gia hạn gói đã Expired thành công và tự động phục hồi Active")
    void extendValidity_ExpiredPackage_ReinstatesActive() {
        subscription.setStatus("Expired");
        subscription.setEndDate(LocalDate.of(2026, 10, 10)); // Đã hết hạn trước hôm nay

        when(userRepository.findByEmailIgnoreCase(managerUser.getEmail())).thenReturn(Optional.of(managerUser));
        when(memberPackageRepository.findByIdForUpdate(101)).thenReturn(Optional.of(subscription));
        when(memberPackageRepository.save(any(MemberPackage.class))).thenAnswer(i -> i.getArgument(0));

        PackageExtensionRequest request = new PackageExtensionRequest(30, "Trung tâm bù ngày cho học viên sau khi mở cửa lại");

        MemberPackageResponse response = memberPackageService.extendValidity(101, request, managerAuth);

        assertNotNull(response);
        assertEquals("Active", response.status());
        assertTrue(response.endDate().isAfter(LocalDate.now(clock)));
    }

    @Test
    @DisplayName("Gia hạn với số ngày <= 0 -> Ném BusinessException")
    void extendValidity_InvalidDays_ThrowsException() {
        PackageExtensionRequest request = new PackageExtensionRequest(0, "Lỗi");

        assertThrows(BusinessException.class, () ->
                memberPackageService.extendValidity(101, request, managerAuth));
    }
}

