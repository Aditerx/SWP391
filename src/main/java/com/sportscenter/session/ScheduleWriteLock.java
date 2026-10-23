package com.sportscenter.session;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.sportscenter.common.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class ScheduleWriteLock {
    private final EntityManager entityManager;
    private final com.sportscenter.user.UserRepository users;

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(Integer centerId) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            var actor = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Account not found"));
            if (!java.util.Objects.equals(centerId, actor.getCenterId())) throw new org.springframework.security.access.AccessDeniedException("Schedule belongs to another center");
        }
        if (centerId == null || entityManager.find(ScheduleMutex.class, centerId, LockModeType.PESSIMISTIC_WRITE) == null) {
            throw new BusinessException("CENTER_REQUIRED: Schedule writes require a valid center");
        }
    }
}
