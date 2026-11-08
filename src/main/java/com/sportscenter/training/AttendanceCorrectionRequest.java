package com.sportscenter.training;

import io.swagger.v3.oas.annotations.media.Schema;

public record AttendanceCorrectionRequest(
        @Schema(description = "Exactly one target: attendanceId OR sessionId + memberId") Integer attendanceId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"Present", "Absent"}) String newState,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Internal audit reason; trimmed, required, at most 1000 characters") String reason,
        Integer sessionId,
        Integer memberId,
        @Schema(nullable = true, description = "Plain text, at most 1000 characters after trim; omitted/null preserves, blank clears") String note
) {
    public AttendanceCorrectionRequest(Integer attendanceId, String newState, String reason) {
        this(attendanceId, newState, reason, null, null, null);
    }
}
