package com.sportscenter.sportclass;

import java.time.LocalDate;
import java.math.BigDecimal;

public record SportsClassResponse(
        Integer id,
        String name,
        Integer subjectId,
        String subjectName,
        Integer coachId,
        String coachName,
        Integer maxCapacity,
        BigDecimal tuitionFee,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer enrolledCount,
        String classType,
        Integer totalSessions) {

    public SportsClassResponse(Integer id, String name, Integer subjectId, Integer coachId,
                               Integer maxCapacity, BigDecimal tuitionFee, LocalDate startDate,
                               LocalDate endDate, String status, Integer enrolledCount) {
        this(id, name, subjectId, null, coachId, null, maxCapacity, tuitionFee, startDate, endDate, status, enrolledCount, "Normal", 12);
    }

    public static SportsClassResponse from(SportsClass sportsClass) {
        return from(sportsClass, null);
    }

    public static SportsClassResponse from(SportsClass sportsClass, Integer enrolledCount) {
        if (sportsClass == null) return null;
        String subjectName = sportsClass.getSubject() != null ? sportsClass.getSubject().getName() : null;
        String coachName = sportsClass.getCoach() != null ? sportsClass.getCoach().getFullName() : null;
        return new SportsClassResponse(
                sportsClass.getId(),
                sportsClass.getName(),
                sportsClass.getSubject() == null ? null : sportsClass.getSubject().getId(),
                subjectName,
                sportsClass.getCoach() == null ? null : sportsClass.getCoach().getId(),
                coachName,
                sportsClass.getMaxCapacity(),
                sportsClass.getTuitionFee(),
                sportsClass.getStartDate(),
                sportsClass.getEndDate(),
                sportsClass.getStatus(),
                enrolledCount,
                sportsClass.getClassType(),
                sportsClass.getTotalSessions());
    }
}
