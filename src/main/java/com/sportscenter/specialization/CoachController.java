package com.sportscenter.specialization;

import com.sportscenter.report.AtRiskMemberResponse;
import com.sportscenter.report.ChurnRiskService;
import com.sportscenter.session.CoachAvailableSlotResponse;
import com.sportscenter.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/coaches")
@RequiredArgsConstructor
public class CoachController {
    private final CoachDirectoryService coachDirectoryService;
    private final ChurnRiskService churnRiskService;
    private final SessionService sessionService;

    @GetMapping("/me/at-risk-members")
    @PreAuthorize("hasRole('COACH')")
    public List<AtRiskMemberResponse> atRiskMembers(Authentication authentication) {
        return churnRiskService.forCoach(authentication);
    }

    @GetMapping("/{coachId}/available-slots")
    public List<CoachAvailableSlotResponse> getAvailableSlots(
            @PathVariable Integer coachId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer subjectId,
            @RequestParam(required = false) Integer roomId) {
        return sessionService.getCoachAvailableSlots(coachId, date, subjectId, roomId);
    }

    @GetMapping
    public List<CoachSummaryResponse> findAll(@RequestParam(required = false) Integer specializationId,
                                              @RequestParam(required = false) String keyword,
                                              Authentication authentication) {
        return coachDirectoryService.findAll(specializationId, keyword, authentication);
    }
}
