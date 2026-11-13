package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.dto.PermissionResponse;
import com.sportscenter.user.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RoleResponse> findAllRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse findRoleById(Integer id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return RoleResponse.from(role);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional
    public RoleResponse updateRolePermissions(Integer roleId, List<Integer> permissionIds) {
        if (permissionIds == null || permissionIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException("INVALID_PERMISSION_IDS: Supply an array of positive permission IDs");
        Role role = roleRepository.findByIdForUpdate(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        List<Permission> newPermissions = permissionRepository.findAllById(permissionIds);
        if (newPermissions.size() != new HashSet<>(permissionIds).size())
            throw new BusinessException("INVALID_PERMISSION_IDS: Every requested permission ID must exist; no permissions were changed");
        String previous = role.getPermissions().stream().map(Permission::getName).sorted()
                .reduce((a, b) -> a + ", " + b).orElse("None");
        Set<Permission> permissionSet = new HashSet<>(newPermissions);
        role.setPermissions(permissionSet);

        Role saved = roleRepository.save(role);
        String permNames = newPermissions.stream().map(Permission::getName).sorted().reduce((a, b) -> a + ", " + b).orElse("None");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated actor is required for permission changes");
        }
        Integer actorId = userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated actor account was not found"));
        auditService.log(actorId, "UPDATE_ROLE_PERMISSIONS", "ROLE", saved.getId(),
                "Updated permissions for role " + saved.getName() + " from: [" + previous + "] to: [" + permNames + "]");

        return RoleResponse.from(saved);
    }
}
