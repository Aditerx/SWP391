package com.sportscenter.user;

import com.sportscenter.user.dto.PermissionResponse;
import com.sportscenter.user.dto.RolePermissionUpdateRequest;
import com.sportscenter.user.dto.RoleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/roles")
    public List<RoleResponse> getAllRoles() {
        return roleService.findAllRoles();
    }

    @GetMapping("/roles/{id}")
    public RoleResponse getRoleById(@PathVariable Integer id) {
        return roleService.findRoleById(id);
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
    public RoleResponse updateRolePermissions(@PathVariable Integer id, @Valid @RequestBody RolePermissionUpdateRequest request) {
        return roleService.updateRolePermissions(id, request.permissionIds());
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> getAllPermissions() {
        return roleService.findAllPermissions();
    }
}
