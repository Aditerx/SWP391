package com.sportscenter.enrollment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @GetMapping("/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> findAll(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(enrollmentService.findAll(classId, memberId, status));
    }

    @GetMapping("/classes/{classId}/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> findByClassId(@PathVariable Integer classId) {
        return ResponseEntity.ok(enrollmentService.findByClassId(classId));
    }

    @GetMapping({"/members/{memberId}/enrollments", "/members/{memberId}/classes"})
    public ResponseEntity<List<EnrollmentResponse>> findByMemberId(@PathVariable Integer memberId) {
        return ResponseEntity.ok(enrollmentService.findByMemberId(memberId));
    }

    @PostMapping("/classes/{classId}/enroll")
    public ResponseEntity<EnrollmentResponse> enrollInClass(
            @PathVariable Integer classId,
            @RequestBody(required = false) EnrollmentRequest request,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        Integer memberId = request != null ? request.memberId() : null;
        EnrollmentResponse response = enrollmentService.enroll(classId, memberId, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @Valid @RequestBody EnrollmentRequest request,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        if (request.classId() == null) {
            throw new IllegalArgumentException("classId is required");
        }
        EnrollmentResponse response = enrollmentService.enroll(request.classId(), request.memberId(), userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/classes/{classId}/cancel-enrollment")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable Integer classId,
            @RequestBody(required = false) EnrollmentRequest request,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        Integer memberId = request != null ? request.memberId() : null;
        return ResponseEntity.ok(enrollmentService.cancelEnrollment(classId, memberId, userEmail));
    }

    @PatchMapping("/enrollments/{id}/cancel")
    public ResponseEntity<EnrollmentResponse> cancelById(
            @PathVariable Integer id,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(enrollmentService.cancelById(id, userEmail));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<EnrollmentResponse> deleteEnrollment(
            @PathVariable Integer id,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(enrollmentService.cancelById(id, userEmail));
    }
}
