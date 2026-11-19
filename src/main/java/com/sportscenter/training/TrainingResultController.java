package com.sportscenter.training;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-results")
@RequiredArgsConstructor
public class TrainingResultController {
    private final TrainingResultService trainingResultService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_CLASSES', 'MANAGE_USERS') or hasAnyRole('COACH', 'MEMBER')")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Results within current owner, author or center scope"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<List<TrainingResultResponse>> searchResults(
            @RequestParam(required = false) Integer sessionId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer coachId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(trainingResultService.searchResults(sessionId, memberId, coachId, authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    @io.swagger.v3.oas.annotations.Operation(description="Attendance support retains current role/authority rules. Non-null professional content requires the assigned Active Coach with RECORD_RESULT; omitted/null content preserves existing content. Writes require the session attendance window.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Result and permitted attendance fields saved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<TrainingResultResponse> recordResult(@Valid @RequestBody TrainingResultRequest request, Authentication authentication) {
        return ResponseEntity.ok(trainingResultService.recordResult(request, authentication));
    }

    @PatchMapping("/{resultId}/content")
    @PreAuthorize("hasRole('COACH') and hasAuthority('RECORD_RESULT')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Replace existing result content after the session",
            description = "Active assigned Coach and result author with RECORD_RESULT, same center. From session end inclusive until next session-day midnight exclusive in Asia/Ho_Chi_Minh. Changes content only; no result or attendance creation. Identical content is a no-op.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Existing result with saved content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid content, session state/time or session eligibility", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Coach permission, assignment, author, center or deadline denied", content = @io.swagger.v3.oas.annotations.media.Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Existing result not found", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<TrainingResultResponse> updateContent(@PathVariable Integer resultId,
            @Valid @RequestBody TrainingResultContentRequest request, Authentication authentication) {
        return ResponseEntity.ok(trainingResultService.updateContent(resultId, request, authentication));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('RECORD_RESULT', 'MANAGE_CLASSES')")
    @io.swagger.v3.oas.annotations.Operation(description="Same rules as single result. A denied or invalid item rolls back the entire request, including attendance, content and audit.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="All results saved atomically"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<List<TrainingResultResponse>> recordBulkResults(@Valid @RequestBody BulkTrainingResultRequest request, Authentication authentication) {
        return ResponseEntity.ok(trainingResultService.recordBulkResults(request, authentication));
    }
}
