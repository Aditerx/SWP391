package com.sportscenter.membership;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberPackageController {
    private final MemberPackageService memberPackageService;

    @GetMapping({"/member-packages", "/subscriptions"})
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_SUBSCRIPTIONS')")
    public List<MemberPackageResponse> getAllSubscriptions() {
        return memberPackageService.findAll();
    }

    @GetMapping({"/members/{memberId}/packages", "/members/{memberId}/subscriptions"})
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'MANAGE_SUBSCRIPTIONS') or hasRole('MEMBER')")
    public List<MemberPackageResponse> getSubscriptionsByMember(@PathVariable Integer memberId,
                                                                Authentication authentication) {
        return memberPackageService.findByMemberId(memberId, authentication);
    }

    @PostMapping({"/members/{memberId}/packages", "/members/{memberId}/subscriptions"})
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    @io.swagger.v3.oas.annotations.Operation(deprecated=true, description="Shared membership mutation retired. Valid authorized requests return400 LEGACY_MEMBERSHIP_RETIRED without writes. Use Normal enrollment or Advanced registration; authentication, permissions and validation still apply. No Deprecation or Sunset response header is promised.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="LEGACY_MEMBERSHIP_RETIRED or invalid request", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public SubscriptionOrderResponse subscribe(@PathVariable Integer memberId, @Valid @RequestBody MemberPackageRequest request, HttpServletRequest httpRequest) {
        return memberPackageService.subscribe(memberId, request, clientIp(httpRequest));
    }

    @PostMapping({"/members/me/subscriptions", "/member-packages/subscribe-me"})
    @PreAuthorize("hasRole('MEMBER')")
    @io.swagger.v3.oas.annotations.Operation(deprecated=true, description="Shared membership mutation retired. Valid authorized requests return400 LEGACY_MEMBERSHIP_RETIRED without writes. Use Normal enrollment or Advanced registration; authentication, permissions and validation still apply. No Deprecation or Sunset response header is promised.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="LEGACY_MEMBERSHIP_RETIRED or invalid request", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public SubscriptionOrderResponse subscribeMe(@Valid @RequestBody MemberPackageRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        return memberPackageService.subscribeMe(request, authentication, clientIp(httpRequest));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    @PatchMapping({"/member-packages/{id}/status", "/subscriptions/{id}/status"})
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    @io.swagger.v3.oas.annotations.Operation(description="This shared route remains available for Advanced subscriptions; not deprecated. Shared membership returns400 LEGACY_MEMBERSHIP_RETIRED; Advanced changes require payment/expiry/attendance/incident workflow and cannot grant rights through generic status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Current Advanced status retained (no-op)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public MemberPackageResponse updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return memberPackageService.updateStatus(id, status);
    }

    @PostMapping({"/member-packages/{id}/extend", "/subscriptions/{id}/extend"})
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    @io.swagger.v3.oas.annotations.Operation(description="This route is not retired for Advanced workflows. Shared membership returns400 LEGACY_MEMBERSHIP_RETIRED; Advanced generic extension returns400 USE_INCIDENT_RECOVERY. Missing or out-of-scope target retains404/403. Use the verified incident workflow.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public MemberPackageResponse extendValidity(@PathVariable Integer id,
                                                @Valid @RequestBody PackageExtensionRequest request,
                                                Authentication authentication) {
        return memberPackageService.extendValidity(id, request, authentication);
    }
}
