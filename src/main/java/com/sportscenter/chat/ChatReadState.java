package com.sportscenter.chat;

import com.sportscenter.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_read_state")
@Getter @Setter @NoArgsConstructor
public class ChatReadState {
    @EmbeddedId private ChatReadStateId id;
    @MapsId("conversationId") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;
    @MapsId("userId") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "last_read_message_id") private Integer lastReadMessageId;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public ChatReadState(ChatConversation conversation, User user) {
        this.conversation = conversation; this.user = user;
        this.id = new ChatReadStateId(conversation.getId(), user.getId());
    }
}
