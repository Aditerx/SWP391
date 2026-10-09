package com.sportscenter.feedback;

import java.time.LocalDateTime;

public record ClassFeedbackResponse(
        Integer feedbackId,
        Integer classId,
        Integer coachId,
        Short classRating,
        Short coachRating,
        String comment,
        LocalDateTime createdAt
) {
    public static ClassFeedbackResponse from(ClassFeedback feedback) {
        return new ClassFeedbackResponse(
                feedback.getFeedbackId(),
                feedback.getSportsClass().getId(),
                feedback.getCoach().getId(),
                feedback.getClassRating(),
                feedback.getCoachRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}
