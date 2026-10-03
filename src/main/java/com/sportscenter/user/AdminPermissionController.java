package com.sportscenter.user;

import com.sportscenter.user.dto.PermissionAssignmentRequest;
import com.sportscenter.user.dto.PermissionMatrixResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_PERMISSIONS')")
public class AdminPermissionController {
    private final AdminPermissionService service;

    @GetMapping
    public ResponseEntity<PermissionMatrixResponse> getMatrix() {
        return ResponseEntity.ok(service.getMatrix());
    }

    @PutMapping
    public ResponseEntity<PermissionMatrixResponse> updatePermission(
            @Valid @RequestBody PermissionAssignmentRequest request) {
        return ResponseEntity.ok(service.updatePermission(request));
    }
}
