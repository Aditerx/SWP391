package com.sportscenter.feedback;

import java.math.BigDecimal;
import java.util.List;

public record ClassFeedbackSummaryResponse(
        Integer classId,
        long totalFeedback,
        BigDecimal averageClassRating,
        BigDecimal averageCoachRating,
        List<ClassFeedbackReviewResponse> feedback
) {
}
