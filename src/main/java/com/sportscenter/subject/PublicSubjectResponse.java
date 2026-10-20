package com.sportscenter.subject;

public record PublicSubjectResponse(Integer id, String slug, String name, String description) {
    public static PublicSubjectResponse from(Subject subject) {
        return new PublicSubjectResponse(subject.getId(), subject.getSlug(), subject.getName(), subject.getDescription());
    }
}
