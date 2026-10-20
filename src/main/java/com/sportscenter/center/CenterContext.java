package com.sportscenter.center;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CenterContext {
    private final UserRepository userRepository;

    public Integer currentCenterId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("An authenticated center is required");
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessException("Authenticated user was not found"));
        if (user.getCenterId() == null && (user.getRole() == null
                || !"Admin".equalsIgnoreCase(user.getRole().getName()))) {
            throw new BusinessException("User account is missing a center assignment");
        }
        return user.getCenterId();
    }

    public Integer centerForNewAccount(Integer requestedCenterId, boolean adminTarget) {
        if (adminTarget) return null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            if (requestedCenterId == null) {
                throw new BusinessException("center_id is required for non-admin users");
            }
            return requestedCenterId;
        }

        User actor = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessException("Authenticated user was not found"));
        boolean actorIsAdmin = actor.getRole() != null && "Admin".equalsIgnoreCase(actor.getRole().getName());
        if (actorIsAdmin) {
            if (requestedCenterId == null) {
                throw new BusinessException("center_id is required when an Admin creates a non-admin account");
            }
            return requestedCenterId;
        }
        if (actor.getCenterId() == null) {
            throw new BusinessException("User account is missing a center assignment");
        }
        return actor.getCenterId();
    }
}
