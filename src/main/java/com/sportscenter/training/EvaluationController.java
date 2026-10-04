package com.sportscenter.training;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService evaluationService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('COACH') or hasRole('MEMBER')")
    public ResponseEntity<List<EvaluationResponse>> searchEvaluations(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer coachId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(evaluationService.searchEvaluations(memberId, coachId, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('COACH') or hasRole('MEMBER')")
    public ResponseEntity<EvaluationResponse> findById(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(evaluationService.findById(id, authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    public ResponseEntity<EvaluationResponse> createEvaluation(@Valid @RequestBody EvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluationService.createEvaluation(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    public ResponseEntity<EvaluationResponse> updateEvaluation(
            @PathVariable Integer id,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return ResponseEntity.ok(evaluationService.updateEvaluation(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    public ResponseEntity<Void> deleteEvaluation(@PathVariable Integer id) {
        evaluationService.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }
}
