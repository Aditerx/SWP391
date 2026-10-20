package com.sportscenter.session;

import java.time.LocalTime;

public record CoachAvailableSlotResponse(
        int slotNumber,
        String slotName,
        LocalTime startTime,
        LocalTime endTime,
        boolean isAvailable,
        String reason
) {
}

