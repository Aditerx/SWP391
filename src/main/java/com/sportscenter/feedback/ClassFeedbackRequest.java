package com.sportscenter.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClassFeedbackRequest(
        @NotNull(message = "classId is required")
        @Min(value = 1, message = "classId must be positive")
        Integer classId,

        @NotNull(message = "coachId is required")
        @Min(value = 1, message = "coachId must be positive")
        Integer coachId,

        @NotNull(message = "classRating is required")
        @Min(value = 1, message = "classRating must be between 1 and 5")
        @Max(value = 5, message = "classRating must be between 1 and 5")
        Short classRating,

        @NotNull(message = "coachRating is required")
        @Min(value = 1, message = "coachRating must be between 1 and 5")
        @Max(value = 5, message = "coachRating must be between 1 and 5")
        Short coachRating,

        @Size(max = 1000, message = "comment must not exceed 1000 characters")
        String comment
) {
}
