package com.sportscenter.chat;

import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversations")
@Getter @Setter @NoArgsConstructor
public class ChatConversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_user_id", nullable = false)
    private User member;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 16)
    private ChatConversationStatus status = ChatConversationStatus.OPEN;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_staff_user_id")
    private User assignedStaff;
    @Enumerated(EnumType.STRING) @Column(name = "handler_type", nullable = false, length = 16)
    private ChatHandlerType handlerType = ChatHandlerType.STAFF;
    @Column(name = "subject", length = 200) private String subject;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "last_message_at") private LocalDateTime lastMessageAt;
    @Column(name = "closed_at") private LocalDateTime closedAt;
}
