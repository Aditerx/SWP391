package com.sportscenter.sportclass;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SportsClassController {
    private final SportsClassService service;
    private final NormalClassCreationService normalClassCreationService;

    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PostMapping("/normal-with-sessions")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SportsClassResponse> createNormal(@Valid @RequestBody NormalClassCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(normalClassCreationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SportsClassResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SportsClassResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SportsClassResponse> create(@Valid @RequestBody SportsClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SportsClassResponse> update(@PathVariable Integer id,
                                                       @Valid @RequestBody SportsClassRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(service.update(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/coach")
    @PreAuthorize("hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<SportsClassResponse> assignCoach(@PathVariable Integer id,
                                                            @Valid @RequestBody AssignCoachRequest request) {
        return ResponseEntity.ok(service.assignCoach(id, request.coachId()));
    }
}
