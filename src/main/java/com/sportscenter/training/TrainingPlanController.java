package com.sportscenter.training;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {
    private final TrainingPlanService trainingPlanService;

    @GetMapping
    public ResponseEntity<List<TrainingPlanResponse>> searchPlans(
            @RequestParam(required = false) Integer coachId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer memberId
    ) {
        return ResponseEntity.ok(trainingPlanService.searchPlans(coachId, classId, memberId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlanResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(trainingPlanService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES')")
    public ResponseEntity<TrainingPlanResponse> createPlan(@Valid @RequestBody TrainingPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainingPlanService.createPlan(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES')")
    public ResponseEntity<TrainingPlanResponse> updatePlan(
            @PathVariable Integer id,
            @Valid @RequestBody TrainingPlanRequest request
    ) {
        return ResponseEntity.ok(trainingPlanService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES')")
    public ResponseEntity<Void> deletePlan(@PathVariable Integer id) {
        trainingPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
