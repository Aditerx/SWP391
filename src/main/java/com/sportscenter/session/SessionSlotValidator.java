package com.sportscenter.session;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.room.Room;
import com.sportscenter.sportclass.SportsClass;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class SessionSlotValidator {

    public static final String OUTDOOR_HEAT_RESTRICTION_MSG =
            "Chính sách chống sốc nhiệt/nắng gắt: %s (%s - %s) bị khóa đối với sân tập hoặc môn học ngoài trời (Outdoor).";

    public static final String INVALID_SLOT_MSG =
            "Khung giờ học (%s - %s) không hợp lệ. SCMS chỉ vận hành theo 7 ca học chuẩn 90 phút/ca: " +
                    "Ca 1 (06:00-07:30), Ca 2 (07:45-09:15), Ca 3 (09:30-11:00), " +
                    "Ca 4 (13:00-14:30), Ca 5 (14:45-16:15), Ca 6 (16:30-18:00), Ca 7 (18:15-19:45).";

    /**
     * Xác thực khung giờ học phải khớp với 1 trong 7 ca chuẩn.
     * Nếu là sân/bộ môn ngoài trời (Outdoor), kiểm tra thêm ràng buộc khóa Ca 3 và Ca 4.
     */
    public TimeSlot validateSlot(LocalTime startTime, LocalTime endTime, boolean isOutdoor) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("Giờ bắt đầu và giờ kết thúc không được để trống");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("End time must be strictly after start time");
        }

        TimeSlot matchedSlot = TimeSlot.findByTime(startTime, endTime)
                .orElseThrow(() -> new BusinessException(
                        String.format(INVALID_SLOT_MSG, startTime, endTime)));

        if (isOutdoor && !matchedSlot.isOutdoorAllowed()) {
            throw new BusinessException(
                    String.format(OUTDOOR_HEAT_RESTRICTION_MSG,
                            matchedSlot.getName(), matchedSlot.getStartTime(), matchedSlot.getEndTime()));
        }

        return matchedSlot;
    }

    /**
     * Xác thực buổi học dựa trên thông tin lớp học và phòng tập.
     */
    public TimeSlot validateSession(SportsClass sportsClass, Room room, LocalTime startTime, LocalTime endTime) {
        boolean outdoor = isOutdoor(sportsClass, room);
        return validateSlot(startTime, endTime, outdoor);
    }

    /**
     * Kiểm tra xem sân bãi hoặc bộ môn của lớp có thuộc loại ngoài trời (Outdoor) hay không.
     */
    public boolean isOutdoor(SportsClass sportsClass, Room room) {
        if (room != null && room.isOutdoor()) {
            return true;
        }
        if (sportsClass != null && sportsClass.getSubject() != null && sportsClass.getSubject().isOutdoor()) {
            return true;
        }
        return false;
    }
}

