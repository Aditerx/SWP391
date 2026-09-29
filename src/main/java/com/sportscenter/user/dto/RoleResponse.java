package com.sportscenter.user.dto;

import com.sportscenter.user.Role;

import java.util.List;

public record RoleResponse(
        Integer id,
        String name,
        String description,
        List<PermissionResponse> permissions
) {
    public static RoleResponse from(Role role) {
        List<PermissionResponse> perms = role.getPermissions() != null
                ? role.getPermissions().stream().map(PermissionResponse::from).toList()
                : List.of();
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                perms
        );
    }
}
