package com.sportscenter.notification;

import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final NotificationRecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** Legacy sender retained for existing booking and attendance messages. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Integer userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setCreatedAt(LocalDateTime.now(clock));
        repository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean create(NotificationEvent event) {
        if (event == null || event.type() == null || event.recipientIds().isEmpty()) return false;
        String legacyType = legacyCategory(event.type());
        Notification notification;
        if (event.dedupeKey() != null && !event.dedupeKey().isBlank()) {
            int inserted = repository.insertEventIfAbsent(event.title(), event.message(), legacyType,
                    event.type().name(), event.relatedType(), event.relatedId(), event.dedupeKey(), LocalDateTime.now(clock));
            if (inserted == 0) return false;
            notification = repository.findByDedupeKey(event.dedupeKey()).orElse(null);
            if (notification == null) return false;
        } else {
            notification = new Notification();
            notification.setTitle(event.title());
            notification.setContent(event.message());
            notification.setType(legacyType);
            notification.setNotificationType(event.type().name());
            notification.setRelatedType(event.relatedType());
            notification.setRelatedId(event.relatedId() == null ? null : event.relatedId().longValue());
            notification.setCreatedAt(LocalDateTime.now(clock));
            notification = repository.save(notification);
        }

        LocalDateTime createdAt = notification.getCreatedAt() == null ? LocalDateTime.now(clock) : notification.getCreatedAt();
        Notification persistedNotification = notification;
        List<NotificationRecipient> recipients = event.recipientIds().stream().map(userId -> {
            NotificationRecipient recipient = new NotificationRecipient(persistedNotification, userRepository.getReferenceById(userId));
            recipient.setCreatedAt(createdAt);
            recipient.setRead(false);
            return recipient;
        }).toList();
        recipientRepository.saveAll(recipients);
        return true;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listMine(Integer userId, boolean unreadOnly, NotificationType type, Pageable pageable) {
        Page<NotificationRepository.NotificationRow> rows = repository.findMine(
                userId, unreadOnly, type == null ? null : type.name(), pageable);
        List<NotificationResponse> content = rows.getContent().stream().map(NotificationResponse::from).toList();
        return new PageImpl<>(content, pageable, rows.getTotalElements());
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        return repository.countUnreadForUser(userId);
    }

    @Transactional
    public void markRead(Integer userId, Integer notificationId) {
        if (repository.findRecipientRead(notificationId, userId).isPresent()) {
            repository.markRecipientRead(notificationId, userId, LocalDateTime.now(clock));
            return;
        }
        if (!repository.isOwnedByUser(notificationId, userId)) throw new NotificationNotFoundException();
        repository.markDirectRead(notificationId, userId);
    }

    @Transactional
    public void markAllRead(Integer userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.markAllRecipientsRead(userId, now);
        repository.markAllDirectRead(userId);
    }

    private String legacyCategory(NotificationType type) {
        return switch (type) {
            case ENROLLMENT_CONFIRMED, PRIVATE_BOOKING_CONFIRMED, PRIVATE_BOOKING_CANCELLED,
                    SESSION_RESCHEDULED, COACH_SUBSTITUTED, SESSION_REMINDER, NO_SHOW_RECORDED -> "Schedule";
            case PAYMENT_SUCCESS, PAYMENT_EXPIRED -> "Payment";
            default -> "System";
        };
    }
}
