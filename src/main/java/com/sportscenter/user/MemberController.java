package com.sportscenter.user;

import com.sportscenter.user.dto.MemberRequest;
import com.sportscenter.user.dto.MemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping
    public List<MemberResponse> getAllMembers() {
        return memberService.findAllMembers();
    }

    @GetMapping("/{id}")
    public MemberResponse getMemberById(@PathVariable Integer id) {
        return memberService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse createMember(@Valid @RequestBody MemberRequest request) {
        return memberService.createMember(request);
    }

    @PutMapping("/{id}")
    public MemberResponse updateMember(@PathVariable Integer id, @Valid @RequestBody MemberRequest request) {
        return memberService.updateMember(id, request);
    }

    @PatchMapping("/{id}/status")
    public MemberResponse updateMemberStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return memberService.updateMemberStatus(id, status);
    }
}
