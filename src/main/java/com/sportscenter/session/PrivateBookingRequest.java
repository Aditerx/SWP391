package com.sportscenter.session;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record PrivateBookingRequest(
        @NotNull(message = "subscriptionId is required")
        Integer subscriptionId,

        @NotNull(message = "coachId is required")
        Integer coachId,

        @NotNull(message = "roomId is required")
        Integer roomId,

        @NotNull(message = "sessionDate is required")
        LocalDate sessionDate,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime,

        Integer classId,
        Integer subjectId
) {
}

