package com.sportscenter.common;

import com.sportscenter.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component @RequiredArgsConstructor
public class ActorPolicy {
    private final UserRepository users;
    public User actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Authentication required");
        User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow(() -> new AccessDeniedException("Account not found"));
        if (!"Active".equalsIgnoreCase(user.getStatus())) throw new AccessDeniedException("Account is not Active");
        return user;
    }
    public static boolean role(User actor, String role) {
        return actor.getRole() != null && role.equalsIgnoreCase(actor.getRole().getName());
    }
    public static void center(User actor, Integer center) {
        if (center == null || !Objects.equals(actor.getCenterId(), center)) throw new AccessDeniedException("Resource belongs to another center");
    }
    public static void manager(User actor) {
        if (!role(actor, "CenterManager") && !role(actor, "Admin")) throw new AccessDeniedException("CenterManager required");
    }
    public static void reason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 2000)
            throw new com.sportscenter.common.exception.BusinessException("REASON_REQUIRED: Supply a reason of at most 2000 characters");
    }
}
