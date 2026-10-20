package com.sportscenter.training;

import jakarta.validation.constraints.NotBlank;

public record AbsenceRequest(@NotBlank String reason) { }
