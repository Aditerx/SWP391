package com.sportscenter.subject;

import java.math.BigDecimal;

public record SubjectResponse(
        Integer id,
        String name,
        String description,
        BigDecimal normalPrice,
        BigDecimal advancedPrice,
        String status,
        String slug,
        Boolean outdoor) {

    public SubjectResponse(Integer id, String name, String description) {
        this(id, name, description, BigDecimal.ZERO, BigDecimal.ZERO, "Active", null, false);
    }

    public static SubjectResponse from(Subject subject) {
        if (subject == null) return null;
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getDescription(),
                subject.getNormalPrice() != null ? subject.getNormalPrice() : BigDecimal.ZERO,
                subject.getAdvancedPrice() != null ? subject.getAdvancedPrice() : BigDecimal.ZERO,
                subject.getStatus(),
                subject.getSlug(),
                subject.getOutdoor()
        );
    }
}
