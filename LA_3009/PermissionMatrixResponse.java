package com.sportscenter.user.dto;

import java.util.List;
import java.util.Map;

public record PermissionMatrixResponse(
        List<RoleInfo> roles,
        List<PermissionGroup> groups
) {
    public record RoleInfo(String code, String name) {}

    public record PermissionGroup(
            String code,
            String name,
            List<PermissionInfo> permissions
    ) {}

    public record PermissionInfo(
            String code,
            String name,
            Map<String, Boolean> roles
    ) {}
}
