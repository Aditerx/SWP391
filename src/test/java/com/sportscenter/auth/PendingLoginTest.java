package com.sportscenter.auth;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingLoginTest {
    @Test
    void pendingAccountWithCorrectPasswordGetsRegistrationCode() {
        var auth = mock(AuthenticationManager.class);
        var users = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        User pending = mock(User.class);
        when(auth.authenticate(any())).thenThrow(new DisabledException("disabled"));
        when(users.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(pending));
        when(pending.getStatus()).thenReturn("Pending");
        when(pending.getPasswordHash()).thenReturn("hash");
        when(encoder.matches("Correct123", "hash")).thenReturn(true);
        AuthController controller = controller(auth, users, encoder);
        BusinessException ex = assertThrows(BusinessException.class, () -> controller.login(
                new LoginRequest("pending@example.com", "Correct123"), new MockHttpServletRequest(), new MockHttpServletResponse()));
        assertTrue(ex.getMessage().startsWith("REGISTRATION_PENDING:"));
    }

    @Test
    void pendingAccountWithWrongPasswordGetsGenericLoginFailure() {
        var auth = mock(AuthenticationManager.class);
        var users = mock(UserRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var attempts = mock(LoginAttemptService.class);
        User pending = mock(User.class);
        when(auth.authenticate(any())).thenThrow(new DisabledException("disabled"));
        when(users.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(pending));
        when(pending.getStatus()).thenReturn("Pending");
        when(pending.getPasswordHash()).thenReturn("hash");
        when(encoder.matches("Wrong123", "hash")).thenReturn(false);
        AuthController controller = controller(auth, users, encoder, attempts);
        assertThrows(BadCredentialsException.class, () -> controller.login(
                new LoginRequest("pending@example.com", "Wrong123"), new MockHttpServletRequest(), new MockHttpServletResponse()));
        verify(attempts).recordFailure("pending@example.com");
    }

    private AuthController controller(AuthenticationManager auth, UserRepository users, PasswordEncoder encoder) {
        return controller(auth, users, encoder, mock(LoginAttemptService.class));
    }
    private AuthController controller(AuthenticationManager auth, UserRepository users, PasswordEncoder encoder, LoginAttemptService attempts) {
        return new AuthController(auth, mock(SecurityContextRepository.class), users, mock(MemberRepository.class), attempts,
                mock(UserService.class), mock(UserDetailsService.class), mock(SessionRegistry.class), mock(PasswordResetService.class),
                mock(RegistrationService.class), encoder);
    }
}
