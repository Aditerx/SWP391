package com.sportscenter.sportclass;

import jakarta.validation.constraints.NotNull;

public record AssignCoachRequest(@NotNull Integer coachId) {
}
