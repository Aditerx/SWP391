package com.sportscenter.notification;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("Không tìm thấy thông báo");
    }
}
