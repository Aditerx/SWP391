package com.sportscenter.user;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.dto.PermissionResponse;
import com.sportscenter.user.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
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
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        List<Permission> newPermissions = permissionRepository.findAllById(permissionIds);
        Set<Permission> permissionSet = new HashSet<>(newPermissions);
        role.setPermissions(permissionSet);

        Role saved = roleRepository.save(role);
        String permNames = newPermissions.stream().map(Permission::getName).reduce((a, b) -> a + ", " + b).orElse("None");
        auditService.log(null, "UPDATE_ROLE_PERMISSIONS", "ROLE", saved.getId(),
                "Updated permissions for role " + saved.getName() + " to: [" + permNames + "]");

        return RoleResponse.from(saved);
    }
}
