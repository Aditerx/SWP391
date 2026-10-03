package com.sportscenter.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionAssignmentRequest(
        @NotBlank String permissionCode,
        @NotBlank String roleCode,
        boolean enabled
) {}
