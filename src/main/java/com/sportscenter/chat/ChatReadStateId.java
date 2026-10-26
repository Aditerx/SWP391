package com.sportscenter.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ChatReadStateId implements Serializable {
    @Column(name = "conversation_id") private Integer conversationId;
    @Column(name = "user_id") private Integer userId;
}
