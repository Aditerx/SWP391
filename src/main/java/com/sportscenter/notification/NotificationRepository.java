package com.sportscenter.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Optional<Notification> findByDedupeKey(String dedupeKey);

    @Query(value = "SELECT n.notification_id AS id, COALESCE(n.notification_type, n.type) AS type, " +
            "n.title AS title, n.content AS message, n.related_type AS relatedType, n.related_id AS relatedId, " +
            "COALESCE(r.is_read, n.is_read, FALSE) AS isRead, n.created_at AS createdAt " +
            "FROM notifications n LEFT JOIN notification_recipients r " +
            "ON r.notification_id = n.notification_id AND r.user_id = :userId " +
            "WHERE (r.user_id IS NOT NULL OR (n.user_id = :userId AND r.user_id IS NULL)) " +
            "AND (:unreadOnly = FALSE OR COALESCE(r.is_read, n.is_read, FALSE) = FALSE) " +
            "AND (:type IS NULL OR COALESCE(n.notification_type, n.type) = :type) " +
            "ORDER BY n.created_at DESC, n.notification_id DESC",
            countQuery = "SELECT COUNT(DISTINCT n.notification_id) FROM notifications n " +
                    "LEFT JOIN notification_recipients r ON r.notification_id = n.notification_id AND r.user_id = :userId " +
                    "WHERE (r.user_id IS NOT NULL OR (n.user_id = :userId AND r.user_id IS NULL)) " +
                    "AND (:unreadOnly = FALSE OR COALESCE(r.is_read, n.is_read, FALSE) = FALSE) " +
                    "AND (:type IS NULL OR COALESCE(n.notification_type, n.type) = :type)",
            nativeQuery = true)
    Page<NotificationRow> findMine(@Param("userId") Integer userId,
                                   @Param("unreadOnly") boolean unreadOnly,
                                   @Param("type") String type,
                                   Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM notifications n LEFT JOIN notification_recipients r " +
            "ON r.notification_id = n.notification_id AND r.user_id = :userId " +
            "WHERE (r.user_id IS NOT NULL OR (n.user_id = :userId AND r.user_id IS NULL)) " +
            "AND COALESCE(r.is_read, n.is_read, FALSE) = FALSE", nativeQuery = true)
    long countUnreadForUser(@Param("userId") Integer userId);

    @Query(value = "SELECT r.notification_id AS notificationId, r.user_id AS userId, r.is_read AS isRead " +
            "FROM notification_recipients r WHERE r.notification_id = :notificationId AND r.user_id = :userId",
            nativeQuery = true)
    Optional<RecipientReadRow> findRecipientRead(@Param("notificationId") Integer notificationId,
                                                @Param("userId") Integer userId);

    @Query(value = "SELECT (EXISTS (SELECT 1 FROM notification_recipients r WHERE r.notification_id = :notificationId AND r.user_id = :userId) " +
            "OR EXISTS (SELECT 1 FROM notifications n WHERE n.notification_id = :notificationId AND n.user_id = :userId))", nativeQuery = true)
    boolean isOwnedByUser(@Param("notificationId") Integer notificationId, @Param("userId") Integer userId);

    @Modifying
    @Query(value = "UPDATE notification_recipients SET is_read = TRUE, read_at = :readAt " +
            "WHERE notification_id = :notificationId AND user_id = :userId AND is_read = FALSE", nativeQuery = true)
    int markRecipientRead(@Param("notificationId") Integer notificationId, @Param("userId") Integer userId,
                          @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query(value = "UPDATE notifications SET is_read = TRUE WHERE notification_id = :notificationId AND user_id = :userId " +
            "AND NOT EXISTS (SELECT 1 FROM notification_recipients r WHERE r.notification_id = notifications.notification_id AND r.user_id = :userId)",
            nativeQuery = true)
    int markDirectRead(@Param("notificationId") Integer notificationId, @Param("userId") Integer userId);

    @Modifying
    @Query(value = "UPDATE notification_recipients SET is_read = TRUE, read_at = :readAt " +
            "WHERE user_id = :userId AND is_read = FALSE", nativeQuery = true)
    int markAllRecipientsRead(@Param("userId") Integer userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query(value = "UPDATE notifications SET is_read = TRUE WHERE user_id = :userId AND is_read = FALSE " +
            "AND NOT EXISTS (SELECT 1 FROM notification_recipients r WHERE r.notification_id = notifications.notification_id AND r.user_id = :userId)",
            nativeQuery = true)
    int markAllDirectRead(@Param("userId") Integer userId);

    @Modifying
    @Query(value = "INSERT INTO notifications (user_id, title, content, type, notification_type, related_type, related_id, dedupe_key, created_at, is_read) " +
            "VALUES (NULL, :title, :message, :legacyType, :notificationType, :relatedType, :relatedId, :dedupeKey, :createdAt, FALSE) " +
            "ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertEventIfAbsent(@Param("title") String title, @Param("message") String message,
                            @Param("legacyType") String legacyType, @Param("notificationType") String notificationType,
                            @Param("relatedType") String relatedType, @Param("relatedId") Integer relatedId,
                            @Param("dedupeKey") String dedupeKey, @Param("createdAt") LocalDateTime createdAt);

    interface NotificationRow {
        Integer getId();
        String getType();
        String getTitle();
        String getMessage();
        String getRelatedType();
        Long getRelatedId();
        boolean getIsRead();
        LocalDateTime getCreatedAt();
    }

    interface RecipientReadRow { Integer getNotificationId(); Integer getUserId(); boolean getIsRead(); }
}
