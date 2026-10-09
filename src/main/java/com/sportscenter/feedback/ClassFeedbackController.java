package com.sportscenter.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ClassFeedbackController {
    private final ClassFeedbackService feedbackService;

    @PostMapping("/api/classes/{classId}/feedback")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ClassFeedbackResponse> create(
            @PathVariable Integer classId,
            @Valid @RequestBody ClassFeedbackRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.create(classId, request, authentication));
    }

    @GetMapping("/api/classes/{classId}/feedback/me")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ClassFeedbackResponse> findMine(
            @PathVariable Integer classId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(feedbackService.findMine(classId, authentication));
    }

    @GetMapping("/api/classes/{classId}/feedback")
    @PreAuthorize("hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS')")
    public ResponseEntity<ClassFeedbackSummaryResponse> findForClass(@PathVariable Integer classId) {
        return ResponseEntity.ok(feedbackService.findForClass(classId));
    }

    @GetMapping("/api/coaches/me/feedback")
    @PreAuthorize("hasRole('COACH')")
    public ResponseEntity<CoachFeedbackSummaryResponse> findForCurrentCoach(
            @RequestParam(required = false) Integer classId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(feedbackService.findForCurrentCoach(classId, authentication));
    }
}
