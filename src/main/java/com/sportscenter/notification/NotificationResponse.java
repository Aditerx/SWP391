package com.sportscenter.notification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record NotificationResponse(
        Integer id,
        String type,
        String title,
        String message,
        String relatedType,
        Integer relatedId,
        boolean isRead,
        OffsetDateTime createdAt) {
    static NotificationResponse from(Notification notification, boolean isRead) {
        String type = notification.getNotificationType() != null
                ? notification.getNotificationType() : notification.getType();
        return new NotificationResponse(notification.getId(), type, notification.getTitle(),
                notification.getContent(), notification.getRelatedType(), notification.getRelatedId(),
                isRead, notification.getCreatedAt() == null ? null : notification.getCreatedAt().atOffset(ZoneOffset.ofHours(7)));
    }

    static NotificationResponse from(NotificationRepository.NotificationRow row) {
        return new NotificationResponse(row.getId(), row.getType(), row.getTitle(), row.getMessage(),
                row.getRelatedType(), row.getRelatedId(), row.getIsRead(),
                row.getCreatedAt() == null ? null : row.getCreatedAt().atOffset(ZoneOffset.ofHours(7)));
    }
}
