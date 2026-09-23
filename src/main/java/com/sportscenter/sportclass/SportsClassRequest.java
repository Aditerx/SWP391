package com.sportscenter.sportclass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record SportsClassRequest(
        @NotBlank String name,
        @NotNull Integer subjectId,
        Integer coachId,
        @NotNull @Positive Integer maxCapacity,
        LocalDate startDate,
        LocalDate endDate,
        String status) {
}
