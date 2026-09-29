package com.sportscenter.membership;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public List<MemberPackageResponse> getAllSubscriptions() {
        return memberPackageService.findAll();
    }

    @GetMapping({"/members/{memberId}/packages", "/members/{memberId}/subscriptions"})
    public List<MemberPackageResponse> getSubscriptionsByMember(@PathVariable Integer memberId) {
        return memberPackageService.findByMemberId(memberId);
    }

    @PostMapping({"/members/{memberId}/packages", "/members/{memberId}/subscriptions"})
    @ResponseStatus(HttpStatus.CREATED)
    public MemberPackageResponse subscribe(@PathVariable Integer memberId, @Valid @RequestBody MemberPackageRequest request) {
        return memberPackageService.subscribe(memberId, request);
    }

    @PatchMapping({"/member-packages/{id}/status", "/subscriptions/{id}/status"})
    public MemberPackageResponse updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return memberPackageService.updateStatus(id, status);
    }
}
