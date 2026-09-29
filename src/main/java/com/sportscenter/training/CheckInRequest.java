package com.sportscenter.training;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull(message = "memberId is required")
        Integer memberId,

        Integer recordedBy,

        String notes
) {
}
