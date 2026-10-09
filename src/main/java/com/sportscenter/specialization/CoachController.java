package com.sportscenter.specialization;

import com.sportscenter.user.CoachRepository;
import com.sportscenter.center.CenterContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coaches")
@RequiredArgsConstructor
public class CoachController {
    private final CoachRepository coachRepository;
    private final CoachSpecializationRepository coachSpecializationRepository;
    private final CenterContext centerContext;

    @GetMapping
    public List<CoachSummaryResponse> findAll(@RequestParam(required = false) Integer specializationId,
                                              @RequestParam(required = false) String keyword,
                                              Authentication authentication) {
        boolean showContacts = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "MANAGE_USERS".equals(authority.getAuthority()));
        String search = keyword == null ? null : keyword.trim().toLowerCase();
        Integer centerId = centerContext.currentCenterId();
        return coachRepository.findAllWithUser().stream().map(coach -> {
                    List<CoachSpecialization> assignments = coachSpecializationRepository.findByCoachUserId(coach.getUserId());
                    List<String> names = assignments.stream().map(item -> item.getSpecialization().getName()).toList();
                    boolean inCenter = centerId == null || centerId.equals(coach.getUser().getCenterId());
                    boolean hasSpecialization = specializationId == null || assignments.stream()
                            .anyMatch(item -> specializationId.equals(item.getSpecialization().getId()));
                    boolean matchesKeyword = search == null || search.isBlank()
                            || (coach.getUser().getFullName() != null
                                && coach.getUser().getFullName().toLowerCase().contains(search))
                            || names.stream().anyMatch(name -> name.toLowerCase().contains(search));
                    return new CoachFilter(coach, names, inCenter && hasSpecialization && matchesKeyword);
                })
                .filter(CoachFilter::matches)
                .map(item -> CoachSummaryResponse.from(item.coach(), item.specializations(), showContacts))
                .toList();
    }

    private record CoachFilter(com.sportscenter.user.Coach coach, List<String> specializations, boolean matches) {}
}
