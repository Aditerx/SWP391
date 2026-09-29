package com.sportscenter.training;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Integer attendanceId,
        Integer sessionId,
        LocalDate sessionDate,
        Integer classId,
        String className,
        Integer memberId,
        String memberName,
        String memberCode,
        Integer recordedBy,
        String recordedByName,
        String state,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime
) {
    public static AttendanceResponse from(Attendance att) {
        if (att == null) return null;

        Integer sessionId = att.getSession() != null ? att.getSession().getId() : null;
        LocalDate sessionDate = att.getSession() != null ? att.getSession().getSessionDate() : null;
        Integer classId = (att.getSession() != null && att.getSession().getSportsClass() != null)
                ? att.getSession().getSportsClass().getId() : null;
        String className = (att.getSession() != null && att.getSession().getSportsClass() != null)
                ? att.getSession().getSportsClass().getName() : null;

        Integer memberId = att.getMember() != null ? att.getMember().getId() : null;
        String memberName = att.getMember() != null ? att.getMember().getFullName() : null;
        String memberCode = memberId != null ? "MB-" + (1000 + memberId) : null;

        Integer recordedBy = att.getRecordedBy() != null ? att.getRecordedBy().getId() : null;
        String recordedByName = att.getRecordedBy() != null ? att.getRecordedBy().getFullName() : null;

        return new AttendanceResponse(
                att.getAttendanceId(),
                sessionId,
                sessionDate,
                classId,
                className,
                memberId,
                memberName,
                memberCode,
                recordedBy,
                recordedByName,
                att.getState(),
                att.getCheckInTime(),
                att.getCheckOutTime()
        );
    }
}
