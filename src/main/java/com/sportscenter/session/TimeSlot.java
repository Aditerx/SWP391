package com.sportscenter.session;

import lombok.Getter;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
public enum TimeSlot {
    SLOT_1(1, "Ca 1", LocalTime.of(6, 0), LocalTime.of(7, 30), true),
    SLOT_2(2, "Ca 2", LocalTime.of(7, 45), LocalTime.of(9, 15), true),
    SLOT_3(3, "Ca 3", LocalTime.of(9, 30), LocalTime.of(11, 0), false),
    SLOT_4(4, "Ca 4", LocalTime.of(13, 0), LocalTime.of(14, 30), false),
    SLOT_5(5, "Ca 5", LocalTime.of(14, 45), LocalTime.of(16, 15), true),
    SLOT_6(6, "Ca 6", LocalTime.of(16, 30), LocalTime.of(18, 0), true),
    SLOT_7(7, "Ca 7", LocalTime.of(18, 15), LocalTime.of(19, 45), true);

    private final int slotNumber;
    private final String name;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final boolean outdoorAllowed;

    TimeSlot(int slotNumber, String name, LocalTime startTime, LocalTime endTime, boolean outdoorAllowed) {
        this.slotNumber = slotNumber;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.outdoorAllowed = outdoorAllowed;
    }

    public String getDisplayName() {
        return String.format("%s (%02d:%02d - %02d:%02d)",
                name, startTime.getHour(), startTime.getMinute(), endTime.getHour(), endTime.getMinute());
    }

    public static Optional<TimeSlot> findByTime(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(slot -> matchesTime(slot.startTime, start) && matchesTime(slot.endTime, end))
                .findFirst();
    }

    public static Optional<TimeSlot> findBySlotNumber(int slotNumber) {
        return Arrays.stream(values())
                .filter(slot -> slot.slotNumber == slotNumber)
                .findFirst();
    }

    public static Optional<TimeSlot> findByStartTime(LocalTime start) {
        if (start == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(slot -> matchesTime(slot.startTime, start))
                .findFirst();
    }

    public static List<TimeSlot> getAvailableSlots(boolean isOutdoor) {
        if (!isOutdoor) {
            return Arrays.asList(values());
        }
        return Arrays.stream(values())
                .filter(TimeSlot::isOutdoorAllowed)
                .toList();
    }

    private static boolean matchesTime(LocalTime standard, LocalTime target) {
        if (target == null) return false;
        return standard.getHour() == target.getHour() && standard.getMinute() == target.getMinute();
    }
}

