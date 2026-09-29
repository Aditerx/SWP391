package com.sportscenter.training;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkTrainingResultRequest(
        @NotNull(message = "sessionId is required")
        Integer sessionId,

        Integer coachId,

        List<Item> items
) {
    public record Item(
            Integer memberId,
            String attendanceStatus,
            String content,
            String reason
    ) {}
}
