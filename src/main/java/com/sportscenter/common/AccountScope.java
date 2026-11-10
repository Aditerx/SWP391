package com.sportscenter.common;

import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Arrays;
import java.util.Objects;

/** Scope for baseline account/resource administration, never a financial scope override. */
public final class AccountScope {
    private AccountScope() { }

    public static User actor(UserRepository users) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Authenticated account required");
        User actor = users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account not found"));
        if (!"Active".equalsIgnoreCase(actor.getStatus()) || actor.getRole() == null)
            throw new AccessDeniedException("Active account with a role required");
        if (actor.getCenterId() == null && !ActorPolicy.role(actor, "Admin"))
            throw new AccessDeniedException("Account center required");
        return actor;
    }

    public static boolean visible(User actor, Integer centerId) {
        return ActorPolicy.role(actor, "Admin") && actor.getCenterId() == null
                || centerId != null && Objects.equals(actor.getCenterId(), centerId);
    }

    /** Staff DTOs contain private account/financial fields; learners/coaches use me/assigned workflows. */
    public static User staffReadActor(UserRepository users) {
        User actor = actor(users);
        if (ActorPolicy.role(actor, "Coach") || ActorPolicy.role(actor, "Member"))
            throw new AccessDeniedException("Use your own profile or assigned roster; staff account records are restricted");
        return actor;
    }

    public static void require(User actor, Integer centerId) {
        if (!visible(actor, centerId)) throw new AccessDeniedException("Resource belongs to another center");
    }

    public static void target(User actor, User target, String... roles) {
        require(actor, target.getCenterId());
        if (target.getRole() == null || Arrays.stream(roles).noneMatch(r -> ActorPolicy.role(target, r)))
            throw new AccessDeniedException("Target account does not belong to this resource type");
    }

    /** RP-04 applies to both the existing account and the requested role, including aliases. */
    public static void manageRole(User actor, User existingTarget, String requestedRole) {
        String existingRole = existingTarget == null || existingTarget.getRole() == null ? null : existingTarget.getRole().getName();
        if ((privileged(existingRole) || privileged(requestedRole)) && !ActorPolicy.role(actor, "Admin"))
            throw new AccessDeniedException("Only Admin may manage Admin or CenterManager accounts");
    }

    private static boolean privileged(String role) {
        return "Admin".equalsIgnoreCase(role) || "CenterManager".equalsIgnoreCase(role);
    }
}
