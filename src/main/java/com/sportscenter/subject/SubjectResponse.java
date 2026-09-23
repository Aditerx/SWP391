package com.sportscenter.subject;

public record SubjectResponse(Integer id, String name, String description) {
    static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.getDescription());
    }
}
