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

    /** Baseline room administration also supports the existing Admin-without-center scope. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquireForResourceAdministration(Integer centerId) {
        com.sportscenter.common.AccountScope.require(com.sportscenter.common.AccountScope.actor(users), centerId);
        if (centerId == null || entityManager.find(ScheduleMutex.class, centerId, LockModeType.PESSIMISTIC_WRITE) == null)
            throw new BusinessException("CENTER_REQUIRED: Resource changes require a valid center");
    }

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
