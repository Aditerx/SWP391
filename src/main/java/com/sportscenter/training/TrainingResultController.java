package com.sportscenter.training;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-results")
@RequiredArgsConstructor
public class TrainingResultController {
    private final TrainingResultService trainingResultService;

    @GetMapping
    public ResponseEntity<List<TrainingResultResponse>> searchResults(
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer coachId
    ) {
        return ResponseEntity.ok(trainingResultService.searchResults(sessionId, memberId, coachId));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    public ResponseEntity<TrainingResultResponse> recordResult(@Valid @RequestBody TrainingResultRequest request) {
        return ResponseEntity.ok(trainingResultService.recordResult(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    public ResponseEntity<List<TrainingResultResponse>> recordBulkResults(@Valid @RequestBody BulkTrainingResultRequest request) {
        return ResponseEntity.ok(trainingResultService.recordBulkResults(request));
    }
}
