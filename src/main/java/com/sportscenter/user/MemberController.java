package com.sportscenter.user;

import com.sportscenter.user.dto.MemberRequest;
import com.sportscenter.user.dto.MemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import com.sportscenter.training.AttendanceService;
import com.sportscenter.training.CheckinCodeResponse;
import com.sportscenter.report.AtRiskMemberResponse;
import com.sportscenter.report.ChurnRiskService;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final AttendanceService attendanceService;
    private final BookingBlockService bookingBlockService;
    private final ChurnRiskService churnRiskService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'REGISTER_MEMBER')")
    public List<MemberResponse> getAllMembers() {
        return memberService.findAllMembers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_USERS', 'REGISTER_MEMBER')")
    public MemberResponse getMemberById(@PathVariable Integer id) {
        return memberService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public MemberResponse createMember(@Valid @RequestBody MemberRequest request) {
        return memberService.createMember(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('REGISTER_MEMBER')")
    public MemberResponse registerMember(@Valid @RequestBody MemberRequest request) {
        return memberService.createMember(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public MemberResponse updateMember(@PathVariable Integer id, @Valid @RequestBody MemberRequest request) {
        return memberService.updateMember(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public MemberResponse updateMemberStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return memberService.updateMemberStatus(id, status);
    }

    @GetMapping("/me/checkin-code")
    @PreAuthorize("hasRole('MEMBER')")
    public CheckinCodeResponse checkinCode(Authentication authentication) {
        return attendanceService.createCheckinCode(authentication.getName());
    }

    @GetMapping("/at-risk")
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public List<AtRiskMemberResponse> atRiskMembers() {
        return churnRiskService.forCenter();
    }

    @PatchMapping("/{id}/booking-block/lift")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liftBookingBlock(@PathVariable Integer id, @Valid @RequestBody BookingBlockLiftRequest request,
                                 org.springframework.security.core.Authentication authentication) {
        bookingBlockService.lift(id, request.reason(), authentication.getName());
    }
}
