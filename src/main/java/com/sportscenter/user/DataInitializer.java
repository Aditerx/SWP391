package com.sportscenter.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() > 0) {
            return;
        }

        List<Permission> permissions = permissionRepository.saveAll(defaultPermissions());
        Map<String, Role> roles = new LinkedHashMap<>();
        for (String roleName : List.of("ADMIN", "CENTER_MANAGER", "RECEPTIONIST", "COACH", "MEMBER")) {
            Role role = roleRepository.findByName(roleName).orElseGet(Role::new);
            role.setName(roleName);
            role.setDescription(roleDescription(roleName));
            roles.put(roleName, role);
        }

        for (Permission permission : permissions) {
            for (String roleName : defaultRoles(permission.getCode())) {
                roles.get(roleName).getPermissions().add(permission);
            }
        }
        roles.get("ADMIN").getPermissions().add(
                permissions.stream().filter(p -> "MANAGE_PERMISSIONS".equals(p.getCode())).findFirst().orElseThrow());
        roleRepository.saveAll(roles.values());

        seedUser(roles.get("ADMIN"), "admin@sportscenter.com", "System Administrator");
        seedUser(roles.get("CENTER_MANAGER"), "manager@sportscenter.com", "Center Manager");
        seedUser(roles.get("RECEPTIONIST"), "receptionist@sportscenter.com", "Receptionist");
        seedUser(roles.get("COACH"), "coach@sportscenter.com", "Coach");
        seedUser(roles.get("MEMBER"), "member@sportscenter.com", "Member");
    }

    private List<Permission> defaultPermissions() {
        List<Permission> permissions = new ArrayList<>();
        add(permissions, "MANAGE_USERS", "Quản lý người dùng", "USER", "Quản lý tài khoản người dùng");
        add(permissions, "MANAGE_CLASSES", "Quản lý lớp học", "CLASS", "Quản lý lớp thể thao");
        add(permissions, "MANAGE_SUBJECTS", "Quản lý môn học", "SUBJECT", "Quản lý môn thể thao");
        add(permissions, "MANAGE_ROOMS", "Quản lý phòng", "ROOM", "Quản lý phòng tập");
        add(permissions, "MANAGE_SESSIONS", "Quản lý buổi tập", "SESSION", "Quản lý buổi tập");
        add(permissions, "MANAGE_PACKAGES", "Quản lý gói tập", "PACKAGE", "Quản lý gói hội viên");
        add(permissions, "MANAGE_SUBSCRIPTIONS", "Quản lý đăng ký hội viên", "SUBSCRIPTION", "Quản lý đăng ký gói tập");
        add(permissions, "MANAGE_CLASS_REGISTRATIONS", "Quản lý đăng ký lớp", "CLASS_REGISTRATION", "Quản lý đăng ký lớp học");
        add(permissions, "MANAGE_INVOICES", "Quản lý hóa đơn", "INVOICE", "Quản lý hóa đơn");
        add(permissions, "MANAGE_SUPPORT_REQUESTS", "Quản lý yêu cầu hỗ trợ", "SUPPORT_REQUEST", "Quản lý yêu cầu hỗ trợ");
        add(permissions, "MANAGE_ATTENDANCE", "Quản lý điểm danh", "ATTENDANCE", "Quản lý điểm danh");
        add(permissions, "MANAGE_TRAINING_PLANS", "Quản lý kế hoạch tập luyện", "TRAINING_PLAN", "Quản lý kế hoạch tập luyện");
        add(permissions, "MANAGE_TRAINING_RESULTS", "Quản lý kết quả tập luyện", "TRAINING_RESULT", "Quản lý kết quả tập luyện");
        add(permissions, "MANAGE_EVALUATIONS", "Quản lý đánh giá", "EVALUATION", "Quản lý đánh giá hội viên");
        add(permissions, "MANAGE_HOMEWORK", "Quản lý bài tập", "HOMEWORK", "Quản lý bài tập");
        add(permissions, "MANAGE_NOTIFICATIONS", "Quản lý thông báo", "NOTIFICATION", "Quản lý thông báo");
        add(permissions, "VIEW_REPORTS", "Xem báo cáo", "REPORT", "Xem báo cáo tổng hợp");
        add(permissions, "VIEW_AUDIT_LOG", "Xem nhật ký hệ thống", "AUDIT", "Xem nhật ký thao tác");
        add(permissions, "MANAGE_PERMISSIONS", "Quản lý phân quyền", "SYSTEM", "Cấu hình quyền của các vai trò");
        return permissions;
    }

    private void add(List<Permission> permissions, String code, String name, String module, String description) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setModule(module);
        permission.setDescription(description);
        permissions.add(permission);
    }

    private Set<String> defaultRoles(String permissionCode) {
        return switch (permissionCode) {
            case "MANAGE_USERS", "MANAGE_CLASSES", "MANAGE_SUBJECTS", "MANAGE_ROOMS",
                 "MANAGE_SESSIONS", "MANAGE_PACKAGES", "VIEW_REPORTS", "VIEW_AUDIT_LOG" -> Set.of("CENTER_MANAGER");
            case "MANAGE_SUBSCRIPTIONS", "MANAGE_CLASS_REGISTRATIONS", "MANAGE_INVOICES",
                 "MANAGE_SUPPORT_REQUESTS" -> Set.of("RECEPTIONIST");
            case "MANAGE_ATTENDANCE" -> Set.of("RECEPTIONIST", "COACH");
            case "MANAGE_TRAINING_PLANS", "MANAGE_TRAINING_RESULTS", "MANAGE_EVALUATIONS",
                 "MANAGE_HOMEWORK" -> Set.of("COACH");
            case "MANAGE_NOTIFICATIONS" -> Set.of("CENTER_MANAGER", "COACH");
            default -> Set.of();
        };
    }

    private String roleDescription(String roleName) {
        return switch (roleName) {
            case "ADMIN" -> "Quản trị cấu hình phân quyền hệ thống";
            case "CENTER_MANAGER" -> "Quản lý vận hành trung tâm";
            case "RECEPTIONIST" -> "Tiếp tân và quản lý đăng ký";
            case "COACH" -> "Huấn luyện viên";
            default -> "Hội viên";
        };
    }

    private void seedUser(Role role, String email, String fullName) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setStatus("ACTIVE");
        user.setRole(role);
        userRepository.save(user);
    }
}
