package com.sportscenter.room;

import java.time.LocalDate;
import java.time.LocalTime;

public interface RoomScheduleConflict {
    Integer getSessionId();

    LocalDate getSessionDate();

    LocalTime getStartTime();

    String getClassName();
}
