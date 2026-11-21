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
    @PreAuthorize("hasRole('ADMIN')")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Role catalog for Active Admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public List<RoleResponse> getAllRoles() {
        return roleService.findAllRoles();
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Role with permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Role not found", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public RoleResponse getRoleById(@PathVariable Integer id) {
        return roleService.findRoleById(id);
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_PERMISSIONS')")
    @io.swagger.v3.oas.annotations.Operation(description = "Active Admin AND MANAGE_PERMISSIONS. Replaces the complete permission set for every account of this role. Empty is allowed only if an Active Admin with both MANAGE_USERS and MANAGE_PERMISSIONS remains. LAST_ADMIN_REQUIRED returns 409 without saving changes.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Complete permission set saved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="LAST_ADMIN_REQUIRED: preserve one Active Admin with MANAGE_USERS and MANAGE_PERMISSIONS", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public RoleResponse updateRolePermissions(@PathVariable Integer id, @Valid @RequestBody RolePermissionUpdateRequest request) {
        return roleService.updateRolePermissions(id, request.permissionIds());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Permission catalog for Active Admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public List<PermissionResponse> getAllPermissions() {
        return roleService.findAllPermissions();
    }
}
