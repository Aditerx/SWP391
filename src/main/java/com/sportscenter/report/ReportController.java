package com.sportscenter.report;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService service;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('VIEW_REPORTS')")
    public ResponseEntity<DashboardResponse> dashboard(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(service.dashboard(auth));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('VIEW_REPORTS')")
    public ResponseEntity<RevenueReportResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "month") String groupBy,org.springframework.security.core.Authentication auth
    ) {
        return ResponseEntity.ok(service.revenueReport(startDate, endDate, groupBy,auth));
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('VIEW_REPORTS')")
    public ResponseEntity<MemberReportResponse> members(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(service.memberReport(auth));
    }
}
