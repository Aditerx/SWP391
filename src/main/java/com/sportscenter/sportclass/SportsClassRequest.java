package com.sportscenter.sportclass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SportsClassRequest(
        @NotBlank String name,
        @NotNull Integer subjectId,
        Integer coachId,
        @NotNull @Positive Integer maxCapacity,
        @DecimalMin(value = "0.00", message = "tuitionFee must not be negative") BigDecimal tuitionFee,
        LocalDate startDate,
        LocalDate endDate,
        String status) {

    public SportsClassRequest(String name, Integer subjectId, Integer coachId, Integer maxCapacity,
                              LocalDate startDate, LocalDate endDate, String status) {
        this(name, subjectId, coachId, maxCapacity, null, startDate, endDate, status);
    }
}
