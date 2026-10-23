package com.sportscenter.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationEvent(NotificationEvent event) {
        try {
            notificationService.create(event);
        } catch (RuntimeException error) {
            log.warn("Unable to create in-app notification type={} relatedType={} relatedId={}: {}",
                    event.type(), event.relatedType(), event.relatedId(), error.getMessage());
        }
    }
}
