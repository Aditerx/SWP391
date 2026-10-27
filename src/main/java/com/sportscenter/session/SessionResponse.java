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
        Integer maxCapacity,
        Integer bookedMemberId,
        String bookedMemberName,
        Boolean isPrivate,
        boolean centerInvalidated,
        Integer makeupForSessionId,
        boolean creditReturned,
        Integer createdBy,
        java.time.LocalDateTime agreementConfirmedAt
) {
    public SessionResponse(
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
        this(id, classId, className, subjectId, subjectName, coachId, coachName,
                roomId, roomName, roomLocation, sessionDate, startTime, endTime,
                status, enrolledCount, maxCapacity, null, null, false,false,null,false,null,null);
    }

    public static SessionResponse from(Session session) {
        return from(session, null);
    }

    public static SessionResponse from(Session session, Integer enrolledCount) {
        if (session == null) return null;
        var sc = session.getSportsClass();
        var room = session.getRoom();
        var coach = session.getCoach() != null ? session.getCoach() : (sc != null ? sc.getCoach() : null);
        var bookedMember = session.getBookedMember();
        boolean isPrivate = session.isPrivateSession();
        return new SessionResponse(
                session.getId(),
                sc != null ? sc.getId() : null,
                sc != null ? sc.getName() : null,
                sc != null && sc.getSubject() != null ? sc.getSubject().getId() : null,
                sc != null && sc.getSubject() != null ? sc.getSubject().getName() : null,
                coach != null ? coach.getId() : null,
                coach != null ? coach.getFullName() : null,
                room != null ? room.getId() : null,
                room != null ? room.getName() : null,
                room != null ? room.getLocation() : null,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                enrolledCount,
                sc != null ? sc.getMaxCapacity() : (room != null ? room.getCapacity() : null),
                bookedMember != null ? bookedMember.getId() : null,
                bookedMember != null ? bookedMember.getFullName() : null,
                isPrivate,session.isCenterInvalidated(),session.getMakeupFor()==null?null:session.getMakeupFor().getId(),session.isCreditReturned(),session.getCreatedBy(),session.getAgreementConfirmedAt()
        );
    }
}
