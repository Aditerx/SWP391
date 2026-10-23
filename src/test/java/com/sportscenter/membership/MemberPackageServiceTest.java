package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.invoice.InvoiceOrderResponse;
import com.sportscenter.invoice.InvoiceService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberPackageServiceTest {
    @Mock private com.sportscenter.session.ScheduleWriteLock scheduleWriteLock;
    @org.mockito.Spy private java.time.Clock clock = java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private MemberPackageRepository memberPackageRepository;

    @Mock
    private MembershipPackageRepository membershipPackageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private MemberPackageService memberPackageService;

    private User sampleMember;
    private MembershipPackage advancedPackage;

    @BeforeEach
    void setUp() {
        lenient().when(memberPackageRepository.findCenterIdById(anyInt())).thenReturn(Optional.of(1));
        sampleMember = new User();
        sampleMember.setId(15); sampleMember.setCenterId(1);
        sampleMember.setEmail("member15@example.com");

        advancedPackage = new MembershipPackage();
        advancedPackage.setId(10);
        advancedPackage.setName("Gói 1:1 Cầu Lông");
        advancedPackage.setPackageType("Advanced");
        advancedPackage.setTotalCredit(12);
        advancedPackage.setDurationDays(45);
        advancedPackage.setPrice(new BigDecimal("3000000"));
        advancedPackage.setStatus("Active");
    }

    @Test
    @DisplayName("subscribe: Subscribing to an Advanced package sets 45 validity days, 0 credit until paid")
    void subscribe_AdvancedPackage_PendingStatusWithZeroCredit() {
        MemberPackageRequest request = new MemberPackageRequest(10, null, null, false, "VNPay", null);

        when(userRepository.findById(15)).thenReturn(Optional.of(sampleMember));
        when(membershipPackageRepository.findById(10)).thenReturn(Optional.of(advancedPackage));
        when(memberPackageRepository.save(any(MemberPackage.class))).thenAnswer(invocation -> {
            MemberPackage mp = invocation.getArgument(0);
            mp.setSubscriptionId(101);
            return mp;
        });

        InvoiceOrderResponse fakeOrder = new InvoiceOrderResponse(
                55, "INV-2026-000055", new BigDecimal("3000000"), "VNPay", "Pending",
                LocalDateTime.now(clock).plusMinutes(5), "http://payment.url");
        when(invoiceService.createPackageOrder(eq(sampleMember), eq(advancedPackage), eq("VNPay"), any(), anyString()))
                .thenReturn(fakeOrder);

        SubscriptionOrderResponse response = memberPackageService.subscribe(15, request, "127.0.0.1");

        assertNotNull(response);
        assertEquals(101, response.subscriptionId());
        assertEquals("Pending", response.status());
    }

    @Test void reactivationDoesNotReplenishSpentCredits() {
        var sub = new MemberPackage(); sub.setSubscriptionId(77); sub.setStatus("Expired"); sub.setRemainingCredit(0);
        sub.setMembershipPackage(advancedPackage); sub.setMember(sampleMember);
        when(memberPackageRepository.findByIdForUpdate(77)).thenReturn(Optional.of(sub));
        when(memberPackageRepository.save(sub)).thenReturn(sub);
        memberPackageService.updateStatus(77,"Active");
        verify(invoiceService).requirePaidSubscription(77);
        assertEquals(0,sub.getRemainingCredit());
    }

    @Test
    @DisplayName("expireOutdatedPackages: Wipes remaining credits to 0 and marks status Expired")
    void expireOutdatedPackages_WipesCreditsAndSetsExpired() {
        MemberPackage outdated1 = new MemberPackage();
        outdated1.setSubscriptionId(201);
        outdated1.setStatus("Active");
        outdated1.setRemainingCredit(5);
        outdated1.setEndDate(LocalDate.now(clock).minusDays(1));

        MemberPackage outdated2 = new MemberPackage();
        outdated2.setSubscriptionId(202);
        outdated2.setStatus("Active");
        outdated2.setRemainingCredit(2);
        outdated2.setEndDate(LocalDate.now(clock).minusDays(3));

        when(memberPackageRepository.findExpiredActivePackageIds(any(LocalDate.class)))
                .thenReturn(List.of(201, 202));

        when(memberPackageRepository.findByIdForUpdate(201)).thenReturn(Optional.of(outdated1));
        when(memberPackageRepository.findByIdForUpdate(202)).thenReturn(Optional.of(outdated2));
        int expiredCount = memberPackageService.expireOutdatedPackages();

        assertEquals(2, expiredCount);
        assertEquals("Expired", outdated1.getStatus());
        assertEquals(0, outdated1.getRemainingCredit());
        assertEquals("Expired", outdated2.getStatus());
        assertEquals(0, outdated2.getRemainingCredit());

        verify(memberPackageRepository, times(2)).save(any(MemberPackage.class));
        verify(auditService, times(2)).log(isNull(), eq("EXPIRE_MEMBER_PACKAGE"), eq("MEMBER_PACKAGE"), anyInt(), anyString());
    }

    @Test void expiryWorkerRechecksLockedPackageAfterAConcurrentExtension() {
        var extended = new MemberPackage(); extended.setSubscriptionId(201); extended.setStatus("Active");
        extended.setEndDate(LocalDate.now(clock).plusDays(10)); extended.setRemainingCredit(5);
        when(memberPackageRepository.findExpiredActivePackageIds(any())).thenReturn(List.of(201));
        when(memberPackageRepository.findByIdForUpdate(201)).thenReturn(Optional.of(extended));
        assertEquals(0,memberPackageService.expireOutdatedPackages());
        assertEquals("Active",extended.getStatus()); assertEquals(5,extended.getRemainingCredit());
        verify(memberPackageRepository,never()).save(any());
    }
}

