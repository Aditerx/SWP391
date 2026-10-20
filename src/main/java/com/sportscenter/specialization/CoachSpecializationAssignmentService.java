package com.sportscenter.specialization;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.Coach;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CoachSpecializationAssignmentService {
    private final CoachSpecializationRepository assignmentRepository;
    private final SpecializationRepository specializationRepository;

    @Transactional
    public void assign(Coach coach, Set<Integer> specializationIds) {
        assignmentRepository.deleteByCoachUserId(coach.getUserId());
        if (specializationIds == null || specializationIds.isEmpty()) return;
        var items = specializationRepository.findAllById(specializationIds);
        if (items.size() != specializationIds.size() || items.stream()
                .anyMatch(item -> !"Active".equalsIgnoreCase(item.getStatus()))) {
            throw new BusinessException("Only active specializations can be assigned to a coach");
        }
        items.forEach(item -> assignmentRepository.save(new CoachSpecialization(coach, item)));
    }
}
