package com.sportscenter.session;

import java.time.LocalDate;
import java.time.LocalTime;

/** Published Normal timetable; deliberately contains no member or subscription data. */
public record CatalogSessionResponse(Integer id, LocalDate sessionDate, LocalTime startTime,
        LocalTime endTime, String coachName, String roomName) {
    public static CatalogSessionResponse from(Session s) {
        var coach = s.getCoach() != null ? s.getCoach() : s.getSportsClass().getCoach();
        return new CatalogSessionResponse(s.getId(),s.getSessionDate(),s.getStartTime(),s.getEndTime(),
                coach == null ? null : coach.getFullName(),s.getRoom() == null ? null : s.getRoom().getName());
    }
}
