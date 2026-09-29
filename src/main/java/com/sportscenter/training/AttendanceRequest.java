package com.sportscenter.training;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AttendanceRequest(
        Integer sessionId,

        @NotNull(message = "memberId is required")
        Integer memberId,

        Integer recordedBy,

        String state,

        LocalDateTime checkInTime,

        LocalDateTime checkOutTime
) {
}
