package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingBlockServiceTest {
    @Mock MemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    @org.mockito.Mock java.time.Clock clock;
    @InjectMocks BookingBlockService service;

    @Test
    void lift_RequiresReason() {
        User manager = new User(); manager.setId(1); manager.setEmail("manager@test.local");
        Member member = new Member();
        when(userRepository.findByEmailIgnoreCase("manager@test.local")).thenReturn(Optional.of(manager));
        when(memberRepository.findByIdForUpdate(8)).thenReturn(Optional.of(member));

        assertThrows(BusinessException.class, () -> service.lift(8, " ", "manager@test.local"));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void lift_ClearsBlockAuditsActorAndNotifiesMember() {
        User manager = new User(); manager.setId(1); manager.setEmail("manager@test.local");
        User memberUser = new User(); memberUser.setId(8);
        Member member = new Member(); member.setUser(memberUser);
        member.setBookingBlockedUntil(java.time.LocalDateTime.of(2026, 10, 20, 10, 0));
        when(userRepository.findByEmailIgnoreCase("manager@test.local")).thenReturn(Optional.of(manager));
        when(memberRepository.findByIdForUpdate(8)).thenReturn(Optional.of(member));

        service.lift(8, "Reviewed appeal", "manager@test.local");

        org.junit.jupiter.api.Assertions.assertNull(member.getBookingBlockedUntil());
        verify(auditService).log(1, "BOOKING_BLOCK_LIFTED", "members", 8, "Reason: Reviewed appeal");
        verify(notificationService).send(8, "Booking restriction lifted",
                "Your class booking restriction has been lifted.", "System");
    }
}
