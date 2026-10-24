package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistrationOtpServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final EmailOtpRepository otps = mock(EmailOtpRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EmailService mail = mock(EmailService.class);
    private final AuditService audit = mock(AuditService.class);
    private final PasswordResetService reset = mock(PasswordResetService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RegistrationService service = new RegistrationService(users, roles, members, otps, encoder, mail, audit,
            reset, Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")), events);

    @Test
    void newRegistrationStoresPendingUserAndOnlyOtpHashThenPublishesMailAfterCommit() {
        when(mail.isConfigured()).thenReturn(true);
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        Role role = mock(Role.class);
        when(roles.findByNameIgnoreCase("Member")).thenReturn(Optional.of(role));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode(any())).thenReturn("stored-hash");
        when(otps.findFirstByEmailIgnoreCaseAndPurposeOrderByCreatedAtDesc(anyString(), anyString())).thenReturn(Optional.empty());
        when(otps.findUnusedForUpdate(anyString(), anyString())).thenReturn(List.of());

        OtpMessageResponse result = service.register(new RegisterRequest("New Member", "new@example.com", "Password123", null, null, null));

        assertNotNull(result);
        verify(users).save(argThat(user -> "Pending".equals(user.getStatus())));
        verify(otps).save(argThat(otp -> "stored-hash".equals(otp.getOtpHash())));
        verify(events).publishEvent(argThat((Object event) -> event instanceof OtpMailEvent mailEvent
                && "REGISTER".equals(mailEvent.purpose()) && mailEvent.otp().matches("\\d{6}")));
        verify(mail, never()).sendOtpEmail(anyString(), anyString(), anyString());
    }

    @Test
    void activeEmailIsRejectedWithExplicitCode() {
        when(mail.isConfigured()).thenReturn(true);
        User active = mock(User.class);
        when(active.getStatus()).thenReturn("Active");
        when(users.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(active));
        BusinessException error = assertThrows(BusinessException.class, () -> service.register(
                new RegisterRequest("Name", "taken@example.com", "Password123", null, null, null)));
        assertTrue(error.getMessage().startsWith("EMAIL_ALREADY_REGISTERED:"));
    }
}
