package com.sportscenter.training;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> searchAttendances(
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer recordedBy,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(attendanceService.searchAttendances(sessionId, memberId, recordedBy, state, start, end));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(attendanceService.checkIn(request.memberId(), request.recordedBy()));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'REGISTER_MEMBER', 'MANAGE_USERS')")
    public ResponseEntity<AttendanceResponse> checkOut(@RequestParam Integer memberId) {
        return ResponseEntity.ok(attendanceService.checkOut(memberId));
    }

    @PostMapping("/correct")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES', 'MANAGE_USERS')")
    public ResponseEntity<AttendanceResponse> correctAttendance(@Valid @RequestBody AttendanceCorrectionRequest request) {
        return ResponseEntity.ok(attendanceService.correctAttendance(request));
    }
}
