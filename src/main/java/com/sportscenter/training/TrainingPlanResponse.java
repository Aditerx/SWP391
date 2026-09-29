package com.sportscenter.training;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingPlanResponse(
        Integer planId,
        Integer coachId,
        String coachName,
        Integer classId,
        String className,
        Integer memberId,
        String memberName,
        String title,
        String content,
        String goal,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {
    public static TrainingPlanResponse from(TrainingPlan plan) {
        if (plan == null) return null;

        Integer coachId = plan.getCoach() != null ? plan.getCoach().getId() : null;
        String coachName = plan.getCoach() != null ? plan.getCoach().getFullName() : null;

        Integer classId = plan.getSportsClass() != null ? plan.getSportsClass().getId() : null;
        String className = plan.getSportsClass() != null ? plan.getSportsClass().getName() : null;

        Integer memberId = plan.getMember() != null ? plan.getMember().getId() : null;
        String memberName = plan.getMember() != null ? plan.getMember().getFullName() : null;

        return new TrainingPlanResponse(
                plan.getPlanId(),
                coachId,
                coachName,
                classId,
                className,
                memberId,
                memberName,
                plan.getTitle(),
                plan.getContent(),
                plan.getGoal(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getCreatedAt()
        );
    }
}
