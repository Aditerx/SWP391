package com.sportscenter.specialization;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/specializations")
@RequiredArgsConstructor
public class SpecializationController {
    private final SpecializationService service;

    @GetMapping
    public List<SpecializationResponse> findAll(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) Integer subjectId) {
        return service.findAll(status, subjectId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_SPECIALIZATIONS')")
    public SpecializationResponse create(@Valid @RequestBody SpecializationRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SPECIALIZATIONS')")
    public SpecializationResponse update(@PathVariable Integer id, @Valid @RequestBody SpecializationRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_SPECIALIZATIONS')")
    public SpecializationResponse updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return service.updateStatus(id, body == null ? null : body.get("status"));
    }
}
