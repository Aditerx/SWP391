package com.sportscenter.user.dto;

import com.sportscenter.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Integer id,
        String code,
        String fullName,
        String email,
        String phone,
        String address,
        String gender,
        LocalDate dateOfBirth,
        String status,
        Integer roleId,
        String roleName,
        LocalDateTime createdAt,
        Set<String> permissions
) {
    public static UserResponse from(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
        Set<String> perms = (user.getRole() != null && user.getRole().getPermissions() != null)
                ? user.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .collect(Collectors.toSet())
                : Set.of();

        String prefix = "USR-";
        if ("Admin".equalsIgnoreCase(roleName)) prefix = "ADM-";
        else if ("CenterManager".equalsIgnoreCase(roleName)) prefix = "MGR-";
        else if ("Coach".equalsIgnoreCase(roleName)) prefix = "COA-";
        else if ("Receptionist".equalsIgnoreCase(roleName)) prefix = "REC-";
        else if ("Member".equalsIgnoreCase(roleName)) prefix = "MB-";

        String code = prefix + (1000 + user.getId());

        return new UserResponse(
                user.getId(),
                code,
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getStatus() != null ? user.getStatus() : "Active",
                roleId,
                roleName,
                user.getCreatedAt(),
                perms
        );
    }
}
