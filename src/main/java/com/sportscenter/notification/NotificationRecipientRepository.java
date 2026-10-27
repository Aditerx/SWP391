package com.sportscenter.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, NotificationRecipientId> { }
