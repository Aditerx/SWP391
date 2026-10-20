package com.sportscenter.session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RecurringPreviewResponse(
        int totalSessions,
        Integer classId,
        String className,
        Integer roomId,
        String roomName,
        boolean hasConflict,
        List<SessionPreviewItem> sessions,
        List<SkippedHolidayItem> skippedHolidays
) {
    public record SessionPreviewItem(
            int sessionNumber,
            LocalDate date,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            boolean hasConflict,
            List<String> conflictDetails
    ) {}

    public record SkippedHolidayItem(
            LocalDate date,
            String holidayName,
            String tag
    ) {}
}

