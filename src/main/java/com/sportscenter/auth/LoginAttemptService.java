package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public boolean isTemporarilyLocked(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void recordFailure(String email) {
        userRepository.findByEmailIgnoreCaseForUpdate(email).ifPresent(user -> {
            if (!"Active".equalsIgnoreCase(user.getStatus())) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                return;
            }

            int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                userRepository.save(user);
                auditService.log(null, "TEMPORARY_ACCOUNT_LOCK", "USER", user.getId(),
                        "Account temporarily locked after repeated failed login attempts");
            } else {
                user.setFailedLoginAttempts(attempts);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void recordSuccess(String email) {
        userRepository.findByEmailIgnoreCaseForUpdate(email).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        });
    }
}
