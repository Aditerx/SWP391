package com.sportscenter.feedback;

import java.math.BigDecimal;
import java.util.List;

public record CoachFeedbackSummaryResponse(
        long totalFeedback,
        BigDecimal averageCoachRating,
        List<CoachFeedbackReviewResponse> feedback
) {
}
