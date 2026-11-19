package com.sportscenter.user;

import com.sportscenter.common.AccountScope;
import com.sportscenter.common.exception.BusinessConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

/** D01: serialize all changes that can remove the last effective administrator. */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AdministrationGuard {
    private final RoleRepository roles;
    private final UserRepository users;

    /** Always acquire the same role rows in ID order, BEFORE any target user row. */
    public User lockAndRequire(String... authorities) {
        roles.lockAllForAdministration();
        User actor = AccountScope.actor(users);
        Set<String> allowed = Set.of(authorities);
        if (actor.getRole().getPermissions().stream().noneMatch(p -> allowed.contains(p.getName())))
            throw new AccessDeniedException("Current account permission required");
        return actor;
    }

    public User requirePermissionAdministrator() {
        User actor = lockAndRequire("MANAGE_PERMISSIONS");
        if (!"Admin".equalsIgnoreCase(actor.getRole().getName()))
            throw new AccessDeniedException("Only active Admin with MANAGE_PERMISSIONS may change role permissions");
        return actor;
    }

    public void checkRolePermissions(Role role, Collection<Permission> proposed) {
        // A role change affects EVERY user of that role, including other Admin accounts.
        if (users.countAdministratorsExcludingRole(role.getId()) > 0) return;
        if ("Admin".equalsIgnoreCase(role.getName()) && capable(proposed)
                && users.countActiveByRole(role.getId()) > 0) return;
        throw lastAdministrator();
    }

    public void checkAccount(User proposed) {
        if (users.countAdministratorsExcludingUser(proposed.getId()) > 0) return;
        if ("Active".equalsIgnoreCase(proposed.getStatus()) && proposed.getRole() != null
                && "Admin".equalsIgnoreCase(proposed.getRole().getName())
                && capable(proposed.getRole().getPermissions())) return;
        throw lastAdministrator();
    }

    private boolean capable(Collection<Permission> permissions) {
        return permissions.stream().anyMatch(p -> "MANAGE_USERS".equals(p.getName()))
                && permissions.stream().anyMatch(p -> "MANAGE_PERMISSIONS".equals(p.getName()));
    }

    private BusinessConflictException lastAdministrator() {
        return new BusinessConflictException("LAST_ADMIN_REQUIRED: Keep at least one Active Admin with MANAGE_USERS and MANAGE_PERMISSIONS");
    }
}
