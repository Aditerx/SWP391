package com.sportscenter.session;

import java.time.LocalTime;

public record TimeSlotResponse(
        int slotNumber,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean outdoorAllowed,
        String displayName
) {
    public static TimeSlotResponse from(TimeSlot slot) {
        return new TimeSlotResponse(
                slot.getSlotNumber(),
                slot.getName(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.isOutdoorAllowed(),
                slot.getDisplayName()
        );
    }
}

