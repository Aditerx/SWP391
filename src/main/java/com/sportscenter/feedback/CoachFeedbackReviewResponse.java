package com.sportscenter.feedback;

import java.time.LocalDateTime;

public record CoachFeedbackReviewResponse(
        Integer classId,
        String className,
        Short classRating,
        Short coachRating,
        String comment,
        LocalDateTime createdAt
) {
    public static CoachFeedbackReviewResponse from(ClassFeedback feedback) {
        return new CoachFeedbackReviewResponse(
                feedback.getSportsClass().getId(),
                feedback.getSportsClass().getName(),
                feedback.getClassRating(),
                feedback.getCoachRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}
