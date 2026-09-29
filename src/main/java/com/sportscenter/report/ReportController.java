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
    @PreAuthorize("hasAnyAuthority('VIEW_REPORTS', 'MANAGE_USERS', 'MANAGE_CLASSES')")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyAuthority('VIEW_REPORTS', 'PROCESS_PAYMENT')")
    public ResponseEntity<RevenueReportResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "month") String groupBy
    ) {
        return ResponseEntity.ok(service.revenueReport(startDate, endDate, groupBy));
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyAuthority('VIEW_REPORTS', 'MANAGE_USERS')")
    public ResponseEntity<MemberReportResponse> members() {
        return ResponseEntity.ok(service.memberReport());
    }
}
