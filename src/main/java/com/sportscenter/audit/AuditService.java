package com.sportscenter.audit;

import lombok.RequiredArgsConstructor;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final SystemLogRepository repository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String action, String targetEntity, Integer targetId, String detail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Cannot audit a mutation without an authenticated user");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found in the database"));

        SystemLog log = new SystemLog();
        log.setUserId(user.getId());
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetail(detail);
        repository.save(log);
    }
}
