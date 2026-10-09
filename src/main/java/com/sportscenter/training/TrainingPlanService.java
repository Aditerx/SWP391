package com.sportscenter.training;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {
    private final TrainingPlanRepository trainingPlanRepository;
    private final UserRepository userRepository;
    private final SportsClassRepository sportsClassRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TrainingPlanResponse> searchPlans(Integer coachId, Integer classId, Integer memberId,
                                                  Authentication authentication) {
        User currentUser = null;
        boolean memberRole = hasRole(authentication, "ROLE_MEMBER");
        boolean coachRole = hasRole(authentication, "ROLE_COACH");
        if (memberRole || coachRole) {
            currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
        }
        if (memberRole) {
            if (memberId != null && !memberId.equals(currentUser.getId())) {
                throw new AccessDeniedException("Members may only view their own training plans");
            }
            memberId = currentUser.getId();
        } else if (coachRole) {
            if (coachId != null && !coachId.equals(currentUser.getId())) {
                throw new AccessDeniedException("Coaches may only view their own training plans");
            }
            coachId = currentUser.getId();
        }
        return trainingPlanRepository.searchPlans(coachId, classId, memberId).stream()
                .map(TrainingPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse findById(Integer id, Authentication authentication) {
        TrainingPlan plan = trainingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training plan not found: " + id));
        if (hasRole(authentication, "ROLE_MEMBER") && (plan.getMember() == null
                || !plan.getMember().getId().equals(currentUserId(authentication)))) {
            throw new AccessDeniedException("Members may only view their own training plans");
        }
        if (hasRole(authentication, "ROLE_COACH") && (plan.getCoach() == null
                || !plan.getCoach().getId().equals(currentUserId(authentication)))) {
            throw new AccessDeniedException("Coaches may only view their own training plans");
        }
        return TrainingPlanResponse.from(plan);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private Integer currentUserId(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user account was not found"));
    }

    @Transactional
    public TrainingPlanResponse createPlan(TrainingPlanRequest request) {
        if ((request.classId() == null && request.memberId() == null) ||
            (request.classId() != null && request.memberId() != null)) {
            throw new BusinessException("Training plan must target EITHER a class OR a member, not both and not neither");
        }

        User coach = userRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found: " + request.coachId()));

        SportsClass sportsClass = null;
        if (request.classId() != null) {
            sportsClass = sportsClassRepository.findById(request.classId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + request.classId()));
        }

        User member = null;
        if (request.memberId() != null) {
            member = userRepository.findById(request.memberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + request.memberId()));
        }

        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("End date cannot be before start date");
        }

        TrainingPlan plan = new TrainingPlan();
        plan.setCoach(coach);
        plan.setSportsClass(sportsClass);
        plan.setMember(member);
        plan.setTitle(request.title());
        plan.setContent(request.content());
        plan.setGoal(request.goal());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        plan.setCreatedAt(LocalDateTime.now());

        TrainingPlan saved = trainingPlanRepository.save(plan);

        String target = sportsClass != null ? "Class: " + sportsClass.getName() : "Member: " + member.getFullName();
        auditService.log(coach.getId(), "MANAGE_TRAINING_PLAN", "training_plans", saved.getPlanId(),
                "Created plan '" + saved.getTitle() + "' for " + target);

        return TrainingPlanResponse.from(saved);
    }

    @Transactional
    public TrainingPlanResponse updatePlan(Integer id, TrainingPlanRequest request) {
        TrainingPlan plan = trainingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training plan not found: " + id));

        if (request.title() != null && !request.title().isBlank()) {
            plan.setTitle(request.title());
        }
        if (request.content() != null) {
            plan.setContent(request.content());
        }
        if (request.goal() != null) {
            plan.setGoal(request.goal());
        }
        if (request.startDate() != null) {
            plan.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            plan.setEndDate(request.endDate());
        }

        if (plan.getStartDate() != null && plan.getEndDate() != null && plan.getEndDate().isBefore(plan.getStartDate())) {
            throw new BusinessException("End date cannot be before start date");
        }

        TrainingPlan saved = trainingPlanRepository.save(plan);

        auditService.log(plan.getCoach() != null ? plan.getCoach().getId() : null,
                "MANAGE_TRAINING_PLAN", "training_plans", saved.getPlanId(),
                "Updated plan '" + saved.getTitle() + "'");

        return TrainingPlanResponse.from(saved);
    }

    @Transactional
    public void deletePlan(Integer id) {
        TrainingPlan plan = trainingPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training plan not found: " + id));

        trainingPlanRepository.delete(plan);

        auditService.log(plan.getCoach() != null ? plan.getCoach().getId() : null,
                "DELETE_TRAINING_PLAN", "training_plans", id,
                "Deleted plan '" + plan.getTitle() + "'");
    }
}
