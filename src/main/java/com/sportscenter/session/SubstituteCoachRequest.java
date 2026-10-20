package com.sportscenter.session;

import jakarta.validation.constraints.NotNull;

public record SubstituteCoachRequest(
        @NotNull(message = "newCoachId is required")
        Integer newCoachId,

        String reason
) {}

