package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks private RoleService roleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateRolePermissions_AuditsAuthenticatedActorId() {
        Role role = new Role();
        role.setId(4);
        role.setName("CenterManager");
        Permission permission = new Permission();
        permission.setId(1);
        permission.setName("MANAGE_USERS");
        User admin = new User();
        admin.setId(99);

        when(roleRepository.findByIdWithPermissions(4)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(1))).thenReturn(List.of(permission));
        when(roleRepository.save(role)).thenReturn(role);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "n/a", List.of()));

        roleService.updateRolePermissions(4, List.of(1));

        verify(auditService).log(eq(99), eq("UPDATE_ROLE_PERMISSIONS"), eq("ROLE"), eq(4),
                eq("Updated permissions for role CenterManager to: [MANAGE_USERS]"));
    }
}
