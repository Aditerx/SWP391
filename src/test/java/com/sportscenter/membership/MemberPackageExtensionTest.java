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
import org.springframework.context.ApplicationEventPublisher;
import java.time.Clock;
import java.time.ZoneId;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberPackageExtensionTest {
    @Mock private com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @org.mockito.Spy
    private Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

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
        mp.setName("Legacy non-Advanced membership");
        mp.setPackageType("Normal");

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

    @Test void legacyActiveMembershipCannotBeExtended() {
        when(memberPackageRepository.findByIdForUpdate(101)).thenReturn(Optional.of(subscription));
        var originalEnd = subscription.getEndDate();
        var error = assertThrows(BusinessException.class, () -> memberPackageService.extendValidity(101,
                new PackageExtensionRequest(15,"Retired shared-membership extension"),managerAuth));
        assertTrue(error.getMessage().startsWith("LEGACY_MEMBERSHIP_RETIRED:"));
        assertEquals(originalEnd,subscription.getEndDate());
        assertEquals("Active",subscription.getStatus());
        verify(memberPackageRepository,never()).save(any());
        verifyNoInteractions(auditService);
    }
    @Test void legacyExpiredMembershipCannotBeReactivatedByExtension() {
        subscription.setStatus("Expired");
        when(memberPackageRepository.findByIdForUpdate(101)).thenReturn(Optional.of(subscription));
        var originalEnd = subscription.getEndDate();
        var error = assertThrows(BusinessException.class, () -> memberPackageService.extendValidity(101,
                new PackageExtensionRequest(30,"No revival"),managerAuth));
        assertTrue(error.getMessage().startsWith("LEGACY_MEMBERSHIP_RETIRED:"));
        assertEquals("Expired",subscription.getStatus());assertEquals(originalEnd,subscription.getEndDate());
        verify(memberPackageRepository,never()).save(any());
    }
    @Test void invalidDaysCannotReopenLegacyMembership() {
        when(memberPackageRepository.findByIdForUpdate(101)).thenReturn(Optional.of(subscription));
        var error = assertThrows(BusinessException.class, () -> memberPackageService.extendValidity(101,
                new PackageExtensionRequest(0,"No extension"),managerAuth));
        assertTrue(error.getMessage().startsWith("LEGACY_MEMBERSHIP_RETIRED:"));
        verify(memberPackageRepository,never()).save(any());
    }
    @Test void advancedExtensionStillRequiresIncidentRecovery() {
        subscription.getMembershipPackage().setPackageType("Advanced");
        when(memberPackageRepository.findByIdForUpdate(101)).thenReturn(Optional.of(subscription));
        assertTrue(assertThrows(BusinessException.class, () -> memberPackageService.extendValidity(101,
                new PackageExtensionRequest(15,"Unproven extension"),managerAuth)).getMessage().startsWith("USE_INCIDENT_RECOVERY:"));
        verify(memberPackageRepository,never()).save(any());
    }
}
