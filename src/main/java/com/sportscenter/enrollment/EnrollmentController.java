package com.sportscenter.enrollment;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER') or hasRole('MEMBER')")
    public ResponseEntity<List<EnrollmentResponse>> findAll(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        return ResponseEntity.ok(enrollmentService.findAll(classId, memberId, status, authentication.getName()));
    }

    @GetMapping("/classes/{classId}/enrollments")
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER')")
    public ResponseEntity<List<EnrollmentResponse>> findByClassId(@PathVariable Integer classId) {
        return ResponseEntity.ok(enrollmentService.findByClassId(classId));
    }

    @GetMapping({"/members/{memberId}/enrollments", "/members/{memberId}/classes"})
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_CLASSES', 'REGISTER_MEMBER') or hasRole('MEMBER')")
    public ResponseEntity<List<EnrollmentResponse>> findByMemberId(@PathVariable Integer memberId,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(enrollmentService.findByMemberId(memberId, authentication.getName()));
    }

    @PostMapping("/classes/{classId}/enroll")
    @PreAuthorize("hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')")
    public ResponseEntity<EnrollmentResponse> enrollInClass(
            @PathVariable Integer classId,
            @RequestBody(required = false) EnrollmentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        Integer memberId = request != null ? request.memberId() : null;
        EnrollmentResponse response = enrollmentService.enroll(classId, memberId, userEmail,
                request != null ? request.paymentMethod() : "Cash", clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/enrollments")
    @PreAuthorize("hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')")
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @Valid @RequestBody EnrollmentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        if (request.classId() == null) {
            throw new IllegalArgumentException("classId is required");
        }
        EnrollmentResponse response = enrollmentService.enroll(request.classId(), request.memberId(), userEmail,
                request.paymentMethod(), clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/classes/{classId}/cancel-enrollment")
    @PreAuthorize("hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable Integer classId,
            @RequestBody(required = false) EnrollmentRequest request,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        Integer memberId = request != null ? request.memberId() : null;
        return ResponseEntity.ok(enrollmentService.cancelEnrollment(classId, memberId, userEmail));
    }

    @PatchMapping("/enrollments/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')")
    public ResponseEntity<EnrollmentResponse> cancelById(
            @PathVariable Integer id,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(enrollmentService.cancelById(id, userEmail));
    }

    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize("hasAnyAuthority('REGISTER_MEMBER', 'MANAGE_CLASSES') or hasRole('MEMBER')")
    public ResponseEntity<EnrollmentResponse> deleteEnrollment(
            @PathVariable Integer id,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(enrollmentService.cancelById(id, userEmail));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
