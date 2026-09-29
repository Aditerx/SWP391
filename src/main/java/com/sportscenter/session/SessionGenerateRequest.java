package com.sportscenter.session;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SessionGenerateRequest(
        @NotNull(message = "classId is required")
        Integer classId,

        @NotNull(message = "roomId is required")
        Integer roomId,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotEmpty(message = "daysOfWeek cannot be empty")
        List<Integer> daysOfWeek, // 1 for Monday, ..., 7 for Sunday

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime
) {}
