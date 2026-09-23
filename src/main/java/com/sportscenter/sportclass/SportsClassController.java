package com.sportscenter.sportclass;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_CLASSES')")
public class SportsClassController {
    private final SportsClassService service;

    @GetMapping
    public ResponseEntity<List<SportsClassResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SportsClassResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<SportsClassResponse> create(@Valid @RequestBody SportsClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SportsClassResponse> update(@PathVariable Integer id,
                                                       @Valid @RequestBody SportsClassRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/coach")
    public ResponseEntity<SportsClassResponse> assignCoach(@PathVariable Integer id,
                                                            @Valid @RequestBody AssignCoachRequest request) {
        return ResponseEntity.ok(service.assignCoach(id, request.coachId()));
    }
}
