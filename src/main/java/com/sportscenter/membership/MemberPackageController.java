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
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    public SubscriptionOrderResponse subscribe(@PathVariable Integer memberId, @Valid @RequestBody MemberPackageRequest request, HttpServletRequest httpRequest) {
        return memberPackageService.subscribe(memberId, request, clientIp(httpRequest));
    }

    @PostMapping("/members/me/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEMBER')")
    public SubscriptionOrderResponse subscribeMe(@Valid @RequestBody MemberPackageRequest request, Authentication authentication, HttpServletRequest httpRequest) {
        return memberPackageService.subscribeMe(request, authentication, clientIp(httpRequest));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    @PatchMapping({"/member-packages/{id}/status", "/subscriptions/{id}/status"})
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    public MemberPackageResponse updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return memberPackageService.updateStatus(id, status);
    }
}
