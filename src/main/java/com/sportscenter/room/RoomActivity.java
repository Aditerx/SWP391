package com.sportscenter.room;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.subject.Subject;
import java.util.Arrays;

/** Physical court type, independent of editable display codes/names and subject IDs. */
public enum RoomActivity {
    TENNIS("quan-vot-tennis"), FOOTBALL("bong-da"), ARCHERY("ban-cung"),
    BASKETBALL("bong-ro"), VOLLEYBALL("bong-chuyen"), PICKLEBALL("pickleball"),
    BADMINTON("cau-long"), TABLE_TENNIS("bong-ban"), SWIMMING("boi-loi");

    private final String subjectSlug;
    RoomActivity(String subjectSlug) { this.subjectSlug = subjectSlug; }

    public static RoomActivity parse(String value) {
        return Arrays.stream(values()).filter(v -> v.name().equals(value)).findFirst()
                .orElseThrow(() -> new BusinessException("INVALID_ROOM_ACTIVITY: Supply a supported activityType from the room contract"));
    }
    public static boolean supports(Room room, Subject subject) {
        return room.getActivityType() != null && subject != null
                && room.getActivityType().subjectSlug.equals(subject.getSlug());
    }
    public static void require(Room room, Subject subject) {
        if (room.getActivityType() == null)
            throw new BusinessException("ROOM_ACTIVITY_UNCONFIGURED: Configure the physical activityType before scheduling this room");
        if (!supports(room, subject))
            throw new BusinessException("ROOM_SUBJECT_MISMATCH: This room does not serve the selected subject");
    }
}
