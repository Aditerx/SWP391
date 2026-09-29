package com.sportscenter.training;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EvaluationResponse(
        Integer evaluationId,
        Integer memberId,
        String memberName,
        String memberCode,
        Integer coachId,
        String coachName,
        LocalDate evaluationDate,
        String comment,
        BigDecimal progressScore
) {
    public static EvaluationResponse from(Evaluation ev) {
        if (ev == null) return null;

        Integer memberId = ev.getMember() != null ? ev.getMember().getId() : null;
        String memberName = ev.getMember() != null ? ev.getMember().getFullName() : null;
        String memberCode = memberId != null ? "MB-" + (1000 + memberId) : null;

        Integer coachId = ev.getCoach() != null ? ev.getCoach().getId() : null;
        String coachName = ev.getCoach() != null ? ev.getCoach().getFullName() : null;

        return new EvaluationResponse(
                ev.getEvaluationId(),
                memberId,
                memberName,
                memberCode,
                coachId,
                coachName,
                ev.getEvaluationDate(),
                ev.getComment(),
                ev.getProgressScore()
        );
    }
}
