package com.sportscenter.training;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceCorrectionRequest(
        @NotNull(message = "attendanceId is required")
        Integer attendanceId,

        @NotBlank(message = "newState is required")
        String newState,

        @NotBlank(message = "reason is required for attendance correction")
        String reason
) {
}
