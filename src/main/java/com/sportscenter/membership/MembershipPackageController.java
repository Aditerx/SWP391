package com.sportscenter.membership;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
public class MembershipPackageController {
    private final MembershipPackageService service;

    @GetMapping
    public ResponseEntity<List<MembershipPackageResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<MembershipPackageResponse> create(
            @Valid @RequestBody MembershipPackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembershipPackageResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody MembershipPackageRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MembershipPackageResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody PackageStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request.status()));
    }
}
