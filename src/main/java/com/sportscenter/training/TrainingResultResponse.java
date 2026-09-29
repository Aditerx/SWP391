package com.sportscenter.training;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingResultResponse(
        Integer resultId,
        Integer sessionId,
        LocalDate sessionDate,
        Integer classId,
        String className,
        Integer memberId,
        String memberName,
        Integer coachId,
        String coachName,
        String content,
        String attendanceStatus,
        LocalDateTime recordedAt
) {
    public static TrainingResultResponse from(TrainingResult tr) {
        if (tr == null) return null;

        Integer sessionId = tr.getSession() != null ? tr.getSession().getId() : null;
        LocalDate sessionDate = tr.getSession() != null ? tr.getSession().getSessionDate() : null;
        Integer classId = (tr.getSession() != null && tr.getSession().getSportsClass() != null)
                ? tr.getSession().getSportsClass().getId() : null;
        String className = (tr.getSession() != null && tr.getSession().getSportsClass() != null)
                ? tr.getSession().getSportsClass().getName() : null;

        Integer memberId = tr.getMember() != null ? tr.getMember().getId() : null;
        String memberName = tr.getMember() != null ? tr.getMember().getFullName() : null;

        Integer coachId = tr.getCoach() != null ? tr.getCoach().getId() : null;
        String coachName = tr.getCoach() != null ? tr.getCoach().getFullName() : null;

        return new TrainingResultResponse(
                tr.getResultId(),
                sessionId,
                sessionDate,
                classId,
                className,
                memberId,
                memberName,
                coachId,
                coachName,
                tr.getContent(),
                tr.getAttendanceStatus(),
                tr.getRecordedAt()
        );
    }
}
