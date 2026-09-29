package com.sportscenter.session;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionResponse(
        Integer id,
        Integer classId,
        String className,
        Integer subjectId,
        String subjectName,
        Integer coachId,
        String coachName,
        Integer roomId,
        String roomName,
        String roomLocation,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        Integer enrolledCount,
        Integer maxCapacity
) {
    public static SessionResponse from(Session session) {
        return from(session, null);
    }

    public static SessionResponse from(Session session, Integer enrolledCount) {
        if (session == null) return null;
        var sc = session.getSportsClass();
        var room = session.getRoom();
        return new SessionResponse(
                session.getId(),
                sc != null ? sc.getId() : null,
                sc != null ? sc.getName() : null,
                sc != null && sc.getSubject() != null ? sc.getSubject().getId() : null,
                sc != null && sc.getSubject() != null ? sc.getSubject().getName() : null,
                sc != null && sc.getCoach() != null ? sc.getCoach().getId() : null,
                sc != null && sc.getCoach() != null ? sc.getCoach().getFullName() : null,
                room != null ? room.getId() : null,
                room != null ? room.getName() : null,
                room != null ? room.getLocation() : null,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                enrolledCount,
                sc != null ? sc.getMaxCapacity() : (room != null ? room.getCapacity() : null)
        );
    }
}
