package com.sportscenter.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RolePermissionUpdateRequest(
        @NotNull List<Integer> permissionIds
) {}
