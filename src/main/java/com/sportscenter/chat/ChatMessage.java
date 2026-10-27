package com.sportscenter.chat;

import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id") private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;
    @Enumerated(EnumType.STRING) @Column(name = "sender_type", nullable = false, length = 16)
    private ChatSenderType senderType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_user_id") private User sender;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "content_type", nullable = false, length = 16) private String contentType = "TEXT";
    @Column(name = "metadata", columnDefinition = "TEXT") private String metadata;
    @Column(name = "client_message_id", length = 100) private String clientMessageId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
