package com.sportscenter.training;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** An explicit content replacement; attendance and record identity are not writable here. */
public record TrainingResultContentRequest(
        @NotNull(message = "content is required")
        @Schema(description = "Required non-null replacement. Empty string clears content; whitespace is preserved.")
        String content
) { }
