package com.sportscenter.user;

import com.sportscenter.user.dto.MemberSelfProfilePatch;
import com.sportscenter.user.dto.MemberSelfProfileResponse;
import com.sportscenter.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class MemberSelfProfileController {
    private final MemberSelfProfileService service;

    @GetMapping
    public MemberSelfProfileResponse get(Authentication authentication) {
        return service.get(authentication.getName());
    }

    @PatchMapping
    public MemberSelfProfileResponse patch(@RequestBody(required = false) MemberSelfProfilePatch patch,
                                            Authentication authentication) {
        if (patch == null) throw new BusinessException("INVALID_PROFILE_BODY: request body must be a JSON object");
        return service.patch(authentication.getName(), patch);
    }
}
