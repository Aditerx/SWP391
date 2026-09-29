package com.sportscenter.training;

import jakarta.validation.constraints.NotNull;

public record TrainingResultRequest(
        @NotNull(message = "sessionId is required")
        Integer sessionId,

        @NotNull(message = "memberId is required")
        Integer memberId,

        @NotNull(message = "coachId is required")
        Integer coachId,

        String content,

        String attendanceStatus
) {
}
