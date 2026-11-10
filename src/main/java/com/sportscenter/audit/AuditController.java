package com.sportscenter.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditController {
    private final AuditQueryService service;

    @GetMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scoped array, newest first; pagination totals are response headers",
            content = @io.swagger.v3.oas.annotations.media.Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SystemLog.class))),
            headers = {
                    @io.swagger.v3.oas.annotations.headers.Header(name = "X-Total-Count", description = "Total after authorization scope and filters", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer", format = "int64")),
                    @io.swagger.v3.oas.annotations.headers.Header(name = "X-Page", description = "Zero-based page", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer")),
                    @io.swagger.v3.oas.annotations.headers.Header(name = "X-Page-Size", description = "Requested size, 1..100", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "integer"))})
    @PreAuthorize("hasAnyRole('ADMIN','CENTERMANAGER') and hasAuthority('VIEW_AUDIT_LOG')")
    public ResponseEntity<List<SystemLog>> findAll(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetEntity,
            @RequestParam(required = false) Integer actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = service.search(auth, startDate, endDate, action, targetEntity, actorId, page, size);
        return ResponseEntity.ok().header("X-Total-Count", Long.toString(result.getTotalElements()))
                .header("X-Page", Integer.toString(page)).header("X-Page-Size", Integer.toString(size))
                .body(result.getContent());
    }
}
