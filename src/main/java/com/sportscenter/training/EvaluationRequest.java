package com.sportscenter.training;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationRequest(
        @NotNull(message = "memberId is required")
        Integer memberId,

        @NotNull(message = "coachId is required")
        Integer coachId,

        LocalDate evaluationDate,

        String comment,

        @DecimalMin(value = "0.0", message = "progressScore must be >= 0.0")
        @DecimalMax(value = "10.0", message = "progressScore must be <= 10.0")
        BigDecimal progressScore
) {
}
