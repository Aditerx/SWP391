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
@RequestMapping("/api/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {
    private final TrainingPlanService trainingPlanService;
    private static final String READ_SCOPE = "Members read own private plans and shared Normal class plans with Registered or Completed enrollment and valid Paid effective-class entitlement. Reconciliation, reversed cash confirmation or Completed Refund excludes shared access. Cancelled/Pending/Waitlisted do not grant shared access. Existing Coach own-author and center scopes remain; stored plan content is not a historical version snapshot.";

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "List training plans in the current account scope", description = READ_SCOPE)
    @PreAuthorize("hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('MEMBER')")
    public ResponseEntity<List<TrainingPlanResponse>> searchPlans(
            @RequestParam(required = false) Integer coachId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer memberId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(trainingPlanService.searchPlans(coachId, classId, memberId, authentication));
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Read one training plan in the current account scope", description = READ_SCOPE)
    @PreAuthorize("hasAnyAuthority('MANAGE_TRAINING_PLAN', 'MANAGE_CLASSES', 'MANAGE_USERS') or hasRole('MEMBER')")
    public ResponseEntity<TrainingPlanResponse> findById(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(trainingPlanService.findById(id, authentication));
    }

    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('COACH') and hasAuthority('MANAGE_TRAINING_PLAN')")
    public ResponseEntity<TrainingPlanResponse> createPlan(@Valid @RequestBody TrainingPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainingPlanService.createPlan(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COACH') and hasAuthority('MANAGE_TRAINING_PLAN')")
    public ResponseEntity<TrainingPlanResponse> updatePlan(
            @PathVariable Integer id,
            @Valid @RequestBody TrainingPlanRequest request
    ) {
        return ResponseEntity.ok(trainingPlanService.updatePlan(id, request));
    }

    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COACH') and hasAuthority('MANAGE_TRAINING_PLAN')")
    public ResponseEntity<Void> deletePlan(@PathVariable Integer id) {
        trainingPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
