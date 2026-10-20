package com.sportscenter.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HolidayRequest(
        @NotNull(message = "holidayDate is required")
        LocalDate holidayDate,

        @NotBlank(message = "holidayName is required")
        String holidayName,

        String tag,
        Boolean active
) {}

