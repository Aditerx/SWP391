package com.sportscenter.enrollment;

public record EnrollmentRequest(
        Integer memberId,
        Integer classId
) {}
