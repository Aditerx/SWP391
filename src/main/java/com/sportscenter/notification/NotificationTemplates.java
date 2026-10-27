package com.sportscenter.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class NotificationTemplates {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private NotificationTemplates() { }

    public static NotificationEvent event(NotificationType type, List<Integer> recipients, String detail,
                                          String relatedType, Integer relatedId, String dedupeKey) {
        return new NotificationEvent(recipients, type, title(type), detail, relatedType, relatedId, dedupeKey);
    }

    public static String title(NotificationType type) {
        return switch (type) {
            case CHAT_MEMBER_MESSAGE -> "New member support message";
            case CHAT_STAFF_REPLY -> "New support reply";
            case ENROLLMENT_CONFIRMED -> "Ghi danh lớp học thành công";
            case PRIVATE_BOOKING_CONFIRMED -> "Đặt buổi học thành công";
            case PRIVATE_BOOKING_CANCELLED -> "Buổi học đã được hủy";
            case SESSION_RESCHEDULED -> "Buổi học được dời lịch";
            case COACH_SUBSTITUTED -> "Huấn luyện viên được thay đổi";
            case PAYMENT_SUCCESS -> "Thanh toán thành công";
            case PAYMENT_EXPIRED -> "Thanh toán đã hết hạn";
            case PACKAGE_EXTENDED -> "Gói học được gia hạn";
            case PACKAGE_EXPIRING_SOON -> "Gói học sắp hết hạn";
            case PACKAGE_EXPIRED -> "Gói học đã hết hạn";
            case CREDIT_LOW -> "Số buổi học sắp hết";
            case NO_SHOW_RECORDED -> "Đã ghi nhận vắng mặt";
            case SESSION_REMINDER -> "Nhắc lịch buổi học";
        };
    }

    public static String enrollmentConfirmed(String className) {
        return "Bạn đã được ghi danh lớp " + className + " thành công sau khi thanh toán.";
    }

    public static String privateBookingConfirmed(String subject, LocalDate date, LocalTime start,
                                                LocalTime end, String coach) {
        return String.format("Buổi học %s ngày %s, %s–%s với HLV %s đã được đặt thành công.",
                subject, DATE.format(date), TIME.format(start), TIME.format(end), coach);
    }

    public static String privateBookingCancelled(String subject, LocalDate date, LocalTime start,
                                                LocalTime end, boolean refunded) {
        return String.format("Buổi học %s ngày %s, %s–%s đã bị hủy. %s", subject, DATE.format(date),
                TIME.format(start), TIME.format(end), refunded ? "1 credit đã được hoàn lại." : "Không có credit nào được hoàn lại.");
    }

    public static String sessionRescheduled(String subject, LocalDate date, LocalTime start,
                                           LocalTime end, String room) {
        return String.format("Buổi học %s đã được dời sang %s, %s–%s, %s.", subject,
                DATE.format(date), TIME.format(start), TIME.format(end), room);
    }

    public static String coachSubstituted(String subject, LocalDate date, LocalTime start,
                                         LocalTime end, String oldCoach, String newCoach) {
        return String.format("Buổi học %s ngày %s, %s–%s: HLV %s được thay bằng HLV %s.", subject,
                DATE.format(date), TIME.format(start), TIME.format(end), oldCoach, newCoach);
    }

    public static String paymentSuccess(String invoiceCode) {
        return "Hóa đơn " + invoiceCode + " đã được thanh toán thành công.";
    }

    public static String paymentExpired(String invoiceCode) {
        return "Hóa đơn " + invoiceCode + " đã hết thời hạn giữ chỗ thanh toán.";
    }

    public static String packageExtended(String name, int days, LocalDate endDate) {
        return String.format("Gói %s của bạn được gia hạn %d ngày, hạn mới đến %s.", name, days, DATE.format(endDate));
    }

    public static String packageExpiring(String name, LocalDate endDate) {
        return String.format("Gói %s của bạn sẽ hết hạn ngày %s.", name, DATE.format(endDate));
    }

    public static String packageExpired(String name, LocalDate endDate) {
        return String.format("Gói %s của bạn đã hết hạn ngày %s.", name, DATE.format(endDate));
    }

    public static String creditLow(int remaining) {
        return "Bạn còn " + remaining + " buổi học trong gói 1:1.";
    }

    public static String noShow(LocalDate date, LocalTime start, LocalTime end) {
        return String.format("Buổi học ngày %s, %s–%s đã được chốt vắng mặt.",
                DATE.format(date), TIME.format(start), TIME.format(end));
    }

    public static String sessionReminder(String subject, LocalDateTime start) {
        return "Nhắc bạn có buổi học " + subject + " ngày " + DATE_TIME.format(start) + ".";
    }
}
