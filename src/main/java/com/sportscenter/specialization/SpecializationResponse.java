package com.sportscenter.specialization;

public record SpecializationResponse(Integer id, String name, Integer subjectId,
                                     String subjectName, String description, String status) {
    public static SpecializationResponse from(Specialization item) {
        return new SpecializationResponse(item.getId(), item.getName(),
                item.getSubject() == null ? null : item.getSubject().getId(),
                item.getSubject() == null ? null : item.getSubject().getName(),
                item.getDescription(), item.getStatus());
    }
}
