package com.sportscenter.session;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConflictCheckRequest(
        Integer classId,
        Integer coachId,
        Integer roomId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer excludeSessionId
) {}
