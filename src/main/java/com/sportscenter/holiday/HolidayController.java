package com.sportscenter.holiday;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService holidayService;

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> findAll() {
        return ResponseEntity.ok(holidayService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<HolidayResponse>> findActive() {
        return ResponseEntity.ok(holidayService.findActive());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<HolidayResponse> toggleActive(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Boolean> body) {
        Boolean active = body != null ? body.get("active") : null;
        return ResponseEntity.ok(holidayService.toggleActive(id, active));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<HolidayResponse> create(@Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.create(request));
    }
}

