package com.sportscenter.session;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record SessionRescheduleRequest(
        @NotNull(message = "newDate is required")
        LocalDate newDate,

        @NotNull(message = "newStartTime is required")
        LocalTime newStartTime,

        @NotNull(message = "newEndTime is required")
        LocalTime newEndTime,

        Integer newRoomId,

        String reason,

        Integer extendValidityDays
) {}

