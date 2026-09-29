package com.sportscenter.user.dto;

import com.sportscenter.user.Permission;

public record PermissionResponse(
        Integer id,
        String name,
        String description
) {
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getDescription()
        );
    }
}
