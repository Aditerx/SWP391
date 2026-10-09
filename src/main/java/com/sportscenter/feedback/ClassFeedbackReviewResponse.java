package com.sportscenter.feedback;

import java.time.LocalDateTime;

public record ClassFeedbackReviewResponse(
        Integer feedbackId,
        String memberName,
        Short classRating,
        Short coachRating,
        String comment,
        LocalDateTime createdAt
) {
    public static ClassFeedbackReviewResponse from(ClassFeedback feedback) {
        return new ClassFeedbackReviewResponse(
                feedback.getFeedbackId(),
                feedback.getMember().getFullName(),
                feedback.getClassRating(),
                feedback.getCoachRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}
