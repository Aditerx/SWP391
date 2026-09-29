package com.sportscenter.sportclass;

import java.time.LocalDate;

public record SportsClassResponse(
        Integer id,
        String name,
        Integer subjectId,
        Integer coachId,
        Integer maxCapacity,
        LocalDate startDate,
        LocalDate endDate,
        String status) {
        String status,
        Integer enrolledCount) {

    static SportsClassResponse from(SportsClass sportsClass) {
    public static SportsClassResponse from(SportsClass sportsClass) {
        return from(sportsClass, null);
    }

    public static SportsClassResponse from(SportsClass sportsClass, Integer enrolledCount) {
        return new SportsClassResponse(
                sportsClass.getId(),
                sportsClass.getName(),
                sportsClass.getSubject() == null ? null : sportsClass.getSubject().getId(),
                sportsClass.getCoach() == null ? null : sportsClass.getCoach().getId(),
                sportsClass.getMaxCapacity(),
                sportsClass.getStartDate(),
                sportsClass.getEndDate(),
                sportsClass.getStatus());
                sportsClass.getStatus(),
                enrolledCount);
    }
}
