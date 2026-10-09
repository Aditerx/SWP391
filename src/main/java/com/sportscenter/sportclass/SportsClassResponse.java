package com.sportscenter.sportclass;

import java.time.LocalDate;
import java.math.BigDecimal;

public record SportsClassResponse(
        Integer id,
        String name,
        Integer subjectId,
        Integer coachId,
        Integer maxCapacity,
        BigDecimal tuitionFee,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer enrolledCount) {

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
                sportsClass.getTuitionFee(),
                sportsClass.getStartDate(),
                sportsClass.getEndDate(),
                sportsClass.getStatus(),
                enrolledCount);
    }
}
