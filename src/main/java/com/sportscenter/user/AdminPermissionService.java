package com.sportscenter.user;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.dto.PermissionAssignmentRequest;
import com.sportscenter.user.dto.PermissionMatrixResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {
    private static final String ADMIN_ROLE = "ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public PermissionMatrixResponse getMatrix() {
        List<Role> roles = roleRepository.findAll().stream()
                .filter(role -> !ADMIN_ROLE.equals(role.getName()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
        List<Permission> permissions = permissionRepository.findAllByOrderByModuleAscNameAsc();

        List<PermissionMatrixResponse.RoleInfo> roleInfos = roles.stream()
                .map(role -> new PermissionMatrixResponse.RoleInfo(role.getName(), roleDisplayName(role.getName())))
                .toList();

        Map<String, List<Permission>> grouped = permissions.stream()
                .collect(Collectors.groupingBy(Permission::getModule, LinkedHashMap::new, Collectors.toList()));
        List<PermissionMatrixResponse.PermissionGroup> groups = grouped.entrySet().stream()
                .map(entry -> new PermissionMatrixResponse.PermissionGroup(
                        entry.getKey(), moduleDisplayName(entry.getKey()),
                        entry.getValue().stream().map(permission -> permissionInfo(permission, roles)).toList()))
                .toList();
        return new PermissionMatrixResponse(roleInfos, groups);
    }

    @Transactional
    public PermissionMatrixResponse updatePermission(PermissionAssignmentRequest request) {
        if (ADMIN_ROLE.equalsIgnoreCase(request.roleCode())) {
            throw new BusinessException("ADMIN permissions are managed outside the business role matrix");
        }
        Permission permission = permissionRepository.findByCode(request.permissionCode())
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + request.permissionCode()));
        Role role = roleRepository.findByName(request.roleCode().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleCode()));

        if ("MANAGE_PERMISSIONS".equals(permission.getCode()) && request.enabled()) {
            throw new BusinessException("MANAGE_PERMISSIONS is reserved for ADMIN");
        }

        if (request.enabled()) {
            role.getPermissions().add(permission);
        } else {
            role.getPermissions().remove(permission);
        }
        roleRepository.save(role);
        return getMatrix();
    }

    private PermissionMatrixResponse.PermissionInfo permissionInfo(Permission permission, List<Role> roles) {
        Map<String, Boolean> assignments = new LinkedHashMap<>();
        for (Role role : roles) {
            assignments.put(role.getName(), role.getPermissions().contains(permission));
        }
        return new PermissionMatrixResponse.PermissionInfo(permission.getCode(), permission.getName(), assignments);
    }

    private String roleDisplayName(String roleName) {
        return switch (roleName) {
            case "CENTER_MANAGER" -> "Center Manager";
            case "RECEPTIONIST" -> "Receptionist";
            case "COACH" -> "Coach";
            default -> "Member";
        };
    }

    private String moduleDisplayName(String module) {
        return switch (module) {
            case "USER" -> "Người dùng";
            case "CLASS" -> "Lớp học";
            case "SUBJECT" -> "Môn học";
            case "ROOM" -> "Phòng";
            case "SESSION" -> "Buổi tập";
            case "PACKAGE" -> "Gói tập";
            case "SUBSCRIPTION" -> "Đăng ký hội viên";
            case "CLASS_REGISTRATION" -> "Đăng ký lớp";
            case "INVOICE" -> "Hóa đơn";
            case "SUPPORT_REQUEST" -> "Yêu cầu hỗ trợ";
            case "ATTENDANCE" -> "Điểm danh";
            case "TRAINING_PLAN" -> "Kế hoạch tập luyện";
            case "TRAINING_RESULT" -> "Kết quả tập luyện";
            case "EVALUATION" -> "Đánh giá";
            case "HOMEWORK" -> "Bài tập";
            case "NOTIFICATION" -> "Thông báo";
            case "REPORT" -> "Báo cáo";
            case "AUDIT" -> "Nhật ký hệ thống";
            default -> "Hệ thống";
        };
    }
}
