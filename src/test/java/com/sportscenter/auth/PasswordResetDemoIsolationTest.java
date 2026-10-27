package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetDemoIsolationTest {
    @Test
    void passwordResetAlwaysGeneratesItsOwnOtpAndNeverUsesRegistrationDemoOtp() {
        EmailOtpRepository otps = mock(EmailOtpRepository.class);
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        User user = mock(User.class);
        when(otps.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(users.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn("Active");
        when(encoder.encode(any())).thenReturn("encoded-hash");
        PasswordResetService service = new PasswordResetService(otps, users, encoder, mock(EmailService.class),
                mock(AuditService.class), mock(SessionRegistry.class), mock(UserDetailsService.class),
                Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")), events);

        service.requestReset("member@example.com");

        var event = org.mockito.ArgumentCaptor.forClass(OtpMailEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("RESET_PASSWORD", event.getValue().purpose());
        assertEquals(6, event.getValue().otp().length());
    }
}
