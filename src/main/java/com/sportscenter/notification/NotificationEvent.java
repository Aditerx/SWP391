package com.sportscenter.notification;

import java.util.List;

public record NotificationEvent(
        List<Integer> recipientIds,
        NotificationType type,
        String title,
        String message,
        String relatedType,
        Integer relatedId,
        String dedupeKey) {
    public NotificationEvent {
        recipientIds = recipientIds == null ? List.of() : recipientIds.stream().distinct().toList();
    }
}
