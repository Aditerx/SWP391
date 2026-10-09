package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {
    @Test
    void locksAccountAfterFiveFailuresAndWritesAudit() {
        UserRepository users = mock(UserRepository.class);
        AuditService audit = mock(AuditService.class);
        User user = new User();
        user.setId(25);
        user.setEmail("member@example.com");
        user.setStatus("Active");
        when(users.findByEmailIgnoreCaseForUpdate(user.getEmail())).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LoginAttemptService service = new LoginAttemptService(users, audit);

        for (int attempt = 0; attempt < 5; attempt++) service.recordFailure(user.getEmail());

        assertEquals(0, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now().plusMinutes(14)));
        verify(audit).log(null, "TEMPORARY_ACCOUNT_LOCK", "USER", 25,
                "Account temporarily locked after repeated failed login attempts");
    }
}
