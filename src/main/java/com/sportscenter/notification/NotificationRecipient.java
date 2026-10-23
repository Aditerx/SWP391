package com.sportscenter.notification;

import com.sportscenter.user.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_recipients")
@Getter
@Setter
@NoArgsConstructor
public class NotificationRecipient {
    @EmbeddedId
    private NotificationRecipientId id;

    @MapsId("notificationId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public NotificationRecipient(Notification notification, User user) {
        this.notification = notification;
        this.user = user;
        this.id = new NotificationRecipientId(notification.getId(), user.getId());
    }
}
