package com.sportscenter.training;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingPlanResponse(
        Integer planId,
        Integer coachId,
        String coachName,
        @Schema(nullable = true) Integer classId,
        @Schema(nullable = true) String className,
        @Schema(nullable = true) Integer memberId,
        @Schema(nullable = true) String memberName,
        String title,
        @Schema(nullable = true) String content,
        @Schema(nullable = true) String goal,
        @Schema(nullable = true) LocalDate startDate,
        @Schema(nullable = true) LocalDate endDate,
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
