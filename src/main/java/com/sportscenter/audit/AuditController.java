package com.sportscenter.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditController {
    private final SystemLogRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")
    public ResponseEntity<List<SystemLog>> findAll() {
        return ResponseEntity.ok(repository.findAllByOrderByCreatedAtDesc());
    }
}
