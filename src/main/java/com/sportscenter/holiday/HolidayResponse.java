package com.sportscenter.holiday;

import java.time.LocalDate;

public record HolidayResponse(
        Integer id,
        LocalDate holidayDate,
        String holidayName,
        String tag,
        boolean active
) {
    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getHolidayDate(),
                holiday.getHolidayName(),
                holiday.getTag(),
                holiday.isActive()
        );
    }
}

