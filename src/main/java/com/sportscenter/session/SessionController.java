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
import com.sportscenter.training.AttendanceService;
import com.sportscenter.training.AbsenceRequest;
import com.sportscenter.training.AttendanceResponse;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessionService;
    private final AttendanceService attendanceService;
    private final SessionAbsenceService sessionAbsenceService;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> findAll(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer coachId,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status, Authentication authentication) {
        return ResponseEntity.ok(sessionService.findAll(classId, coachId, roomId, memberId, startDate, endDate, status, authentication));
    }

    @GetMapping("/time-slots")
    public ResponseEntity<List<TimeSlotResponse>> getTimeSlots(
            @RequestParam(required = false) Boolean isOutdoor) {
        return ResponseEntity.ok(sessionService.getTimeSlots(isOutdoor));
    }

    @GetMapping("/catalog/{classId}")
    public ResponseEntity<List<CatalogSessionResponse>> publicTimetable(@PathVariable Integer classId) {
        return ResponseEntity.ok(sessionService.publicTimetable(classId));
    }

    @GetMapping("/coaches/{coachId}/available-slots")
    public ResponseEntity<List<CoachAvailableSlotResponse>> getCoachAvailableSlots(
            @PathVariable Integer coachId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) Integer roomId) {
        return ResponseEntity.ok(sessionService.getCoachAvailableSlots(coachId, date, subjectId, roomId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(sessionService.findById(id, authentication));
    }

    @PostMapping("/book-private")
    @PreAuthorize("hasRole('COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))")
    public ResponseEntity<SessionResponse> bookPrivate(
            @Valid @RequestBody PrivateBookingRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.bookPrivateSession(request, authentication));
    }

    @PostMapping("/{sessionId}/cancel-private")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<SessionResponse> cancelPrivate(
            @PathVariable Integer sessionId,
            Authentication authentication) {
        return ResponseEntity.ok(sessionService.cancelPrivateSession(sessionId, authentication));
    }

    @PostMapping("/{sessionId}/absence")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<SessionAbsenceService.Notice> requestAbsence(@PathVariable Integer sessionId,
            @Valid @RequestBody AbsenceRequest request, Authentication authentication) {
        return ResponseEntity.ok(sessionAbsenceService.report(sessionId, request.reason(), authentication));
    }
    @GetMapping("/{sessionId}/absence-notices") @PreAuthorize("isAuthenticated()")
    public List<SessionAbsenceService.Notice> absenceHistory(@PathVariable Integer sessionId,Authentication auth){return sessionAbsenceService.history(sessionId,auth);}

    @PostMapping("/check-conflict")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<ConflictCheckResponse> checkConflict(@Valid @RequestBody ConflictCheckRequest request) {
        return ResponseEntity.ok(sessionService.checkConflict(request));
    }

    @PostMapping("/preview-recurring")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<RecurringPreviewResponse> previewRecurring(@Valid @RequestBody RecurringPreviewRequest request) {
        return ResponseEntity.ok(sessionService.previewRecurringSessions(request));
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
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SessionResponse> updateStatus(@PathVariable Integer id,
                                                        @Valid @RequestBody SessionStatusRequest request) {
        return ResponseEntity.ok(sessionService.updateStatus(id, request.status()));
    }

    @PostMapping("/{sessionId}/reschedule")
    @PreAuthorize("hasRole('COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))")
    public ResponseEntity<SessionResponse> reschedule(
            @PathVariable Integer sessionId,
            @Valid @RequestBody SessionRescheduleRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(sessionService.rescheduleSession(sessionId, request, authentication));
    }

    @PostMapping("/{sessionId}/substitute-coach")
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SessionResponse> substituteCoach(
            @PathVariable Integer sessionId,
            @Valid @RequestBody SubstituteCoachRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(sessionService.substituteCoach(sessionId, request, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
