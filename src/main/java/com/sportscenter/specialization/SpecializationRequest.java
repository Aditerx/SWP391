package com.sportscenter.specialization;

import jakarta.validation.constraints.NotBlank;

public record SpecializationRequest(@NotBlank String name, Integer subjectId,
                                   String description, String status) {}
