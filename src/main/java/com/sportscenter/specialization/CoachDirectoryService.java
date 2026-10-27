package com.sportscenter.specialization;

import com.sportscenter.center.CenterContext;
import com.sportscenter.user.Coach;
import com.sportscenter.user.CoachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachDirectoryService {
    private final CoachRepository coachRepository;
    private final CoachSpecializationRepository coachSpecializationRepository;
    private final CenterContext centerContext;

    @Transactional(readOnly = true)
    public List<CoachSummaryResponse> findAll(Integer specializationId, String keyword, Authentication authentication) {
        Integer centerId = centerContext.currentCenterId();
        List<Coach> coaches = coachRepository.findAllWithUserByCenterId(centerId);
        if (coaches.isEmpty()) return List.of();

        Collection<Integer> coachIds = coaches.stream().map(Coach::getUserId).toList();
        Map<Integer, List<CoachSpecialization>> assignmentsByCoach = coachSpecializationRepository
                .findAllWithSpecializationsByCoachIds(coachIds).stream()
                .collect(Collectors.groupingBy(item -> item.getId().getCoachId()));
        String search = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        boolean showContacts = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "MANAGE_USERS".equals(authority.getAuthority()));

        return coaches.stream().filter(coach -> {
                    List<CoachSpecialization> assignments = assignmentsByCoach.getOrDefault(coach.getUserId(), List.of());
                    boolean hasSpecialization = specializationId == null || assignments.stream()
                            .anyMatch(item -> specializationId.equals(item.getSpecialization().getId()));
                    List<String> names = assignments.stream().map(item -> item.getSpecialization().getName()).toList();
                    boolean matchesKeyword = search == null || search.isBlank()
                            || (coach.getUser().getFullName() != null
                                && coach.getUser().getFullName().toLowerCase(Locale.ROOT).contains(search))
                            || names.stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(search));
                    return hasSpecialization && matchesKeyword;
                })
                .map(coach -> CoachSummaryResponse.from(coach,
                        assignmentsByCoach.getOrDefault(coach.getUserId(), List.of()).stream()
                                .map(item -> item.getSpecialization().getName()).toList(), showContacts))
                .toList();
    }
}
