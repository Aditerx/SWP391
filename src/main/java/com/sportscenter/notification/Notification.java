package com.sportscenter.notification;

import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id")
    private User sender;
    @Column(name = "title", nullable = false, length = 150)
    private String title;
    @Column(name = "content")
    private String content;
    @Column(name = "type", nullable = false, length = 20)
    private String type = "Other";
    @Column(name = "is_read", nullable = false)
    private boolean read;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
