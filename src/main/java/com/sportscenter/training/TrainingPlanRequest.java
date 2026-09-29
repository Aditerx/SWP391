package com.sportscenter.training;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TrainingPlanRequest(
        @NotNull(message = "coachId is required")
        Integer coachId,

        Integer classId,

        Integer memberId,

        @NotBlank(message = "title is required")
        String title,

        String content,

        String goal,

        LocalDate startDate,

        LocalDate endDate
) {
}
