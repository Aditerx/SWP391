package com.sportscenter.sportclass;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record NormalClassCreationRequest(@NotNull UUID requestId,
        @NotNull @Valid SportsClassRequest sportsClass, @NotNull Integer roomId,
        @NotEmpty List<@Min(1) @Max(7) Integer> daysOfWeek,
        @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
