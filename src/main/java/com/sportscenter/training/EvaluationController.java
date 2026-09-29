package com.sportscenter.training;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService evaluationService;

    @GetMapping
    public ResponseEntity<List<EvaluationResponse>> searchEvaluations(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer coachId
    ) {
        return ResponseEntity.ok(evaluationService.searchEvaluations(memberId, coachId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(evaluationService.findById(id));
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
