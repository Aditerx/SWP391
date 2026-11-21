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
public class MembershipPackageController {
    private final MembershipPackageService service;

    @GetMapping
    public ResponseEntity<List<MembershipPackageResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    @io.swagger.v3.oas.annotations.Operation(deprecated=true, description="Shared membership mutation retired. Valid authorized requests return400 LEGACY_MEMBERSHIP_RETIRED without writes. Use Normal enrollment or Advanced registration; authentication, permissions and validation still apply. No Deprecation or Sunset response header is promised.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="LEGACY_MEMBERSHIP_RETIRED or invalid request", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<MembershipPackageResponse> create(
            @Valid @RequestBody MembershipPackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    @io.swagger.v3.oas.annotations.Operation(deprecated=true, description="Shared membership mutation retired. Valid authorized requests return400 LEGACY_MEMBERSHIP_RETIRED without writes. Use Normal enrollment or Advanced registration; authentication, permissions and validation still apply. No Deprecation or Sunset response header is promised.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="LEGACY_MEMBERSHIP_RETIRED or invalid request", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<MembershipPackageResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody MembershipPackageRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_PACKAGES')")
    @io.swagger.v3.oas.annotations.Operation(deprecated=true, description="Shared membership mutation retired. Valid authorized requests return400 LEGACY_MEMBERSHIP_RETIRED without writes. Use Normal enrollment or Advanced registration; authentication, permissions and validation still apply. No Deprecation or Sunset response header is promised.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="LEGACY_MEMBERSHIP_RETIRED or invalid request", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<MembershipPackageResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody PackageStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request.status()));
    }
}
