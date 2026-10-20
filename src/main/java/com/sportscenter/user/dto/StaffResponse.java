package com.sportscenter.user.dto;

import com.sportscenter.user.Coach;
import com.sportscenter.user.User;

public record StaffResponse(
        Integer id,
        String code,
        String name,
        String email,
        String phone,
        String role,
        String specialization,
        String specializationVi,
        String status,
        Integer activeClassesCount,
        Boolean emailSent,
        String warning
) {
    public static StaffResponse fromUser(User user, Coach coach, Integer classCount) {
        String roleName = user.getRole() != null ? user.getRole().getName() : "Staff";
        String normalizedRole = roleName.toLowerCase();
        if ("centermanager".equals(normalizedRole)) normalizedRole = "manager";

        String prefix = "coach".equals(normalizedRole) ? "COA" : "receptionist".equals(normalizedRole) ? "REC" : "STF";
        String code = prefix + "-" + String.format("%03d", user.getId());

        String spec = coach != null ? coach.getSpecialization() : null;

        return new StaffResponse(
                user.getId(),
                code,
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                normalizedRole,
                spec,
                spec,
                user.getStatus() != null ? user.getStatus().toLowerCase() : "active",
                classCount != null ? classCount : 0,
                null,
                null
        );
    }

    public StaffResponse withEmailDelivery(boolean sent, String warning) {
        return new StaffResponse(id, code, name, email, phone, role, specialization,
                specializationVi, status, activeClassesCount, sent, warning);
    }
}
