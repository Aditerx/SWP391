package com.sportscenter.session;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> findAll(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer coachId,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(sessionService.findAll(classId, coachId, roomId, memberId, startDate, endDate, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(sessionService.findById(id));
    }

    @PostMapping("/check-conflict")
    public ResponseEntity<ConflictCheckResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return ResponseEntity.ok(sessionService.checkConflict(request));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(request));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<List<SessionResponse>> generateRecurring(@Valid @RequestBody SessionGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.generateRecurringSessions(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SessionResponse> update(@PathVariable Integer id,
                                                  @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(sessionService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SessionResponse> updateStatus(@PathVariable Integer id,
                                                        @Valid @RequestBody SessionStatusRequest request) {
        return ResponseEntity.ok(sessionService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
