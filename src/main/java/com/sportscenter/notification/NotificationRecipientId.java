package com.sportscenter.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class NotificationRecipientId implements Serializable {
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "user_id")
    private Integer userId;

    public NotificationRecipientId(Integer notificationId, Integer userId) {
        this.notificationId = notificationId;
        this.userId = userId;
    }
}
