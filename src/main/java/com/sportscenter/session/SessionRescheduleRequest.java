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

        Integer extendValidityDays,
        String cause,
        boolean memberAgreed,
        String requestKey,
        Integer incidentId,
        String extraValidityReason
) {
    public SessionRescheduleRequest(LocalDate date,LocalTime start,LocalTime end,Integer room,String reason,Integer days){
        this(date,start,end,room,reason,days,null,false,null,null,null);
    }
}

