package com.sportscenter.chat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
public record ChatMessageResponse(Integer id, Integer conversationId, ChatSenderType senderType, String senderName,
                                 boolean isMine, String content, String contentType, String metadata,
                                 String clientMessageId, OffsetDateTime createdAt) {
    static ChatMessageResponse from(ChatMessage m, Integer viewerId) {
        return new ChatMessageResponse(m.getId(), m.getConversation().getId(), m.getSenderType(),
                m.getSender() == null ? null : m.getSender().getFullName(),
                m.getSender() != null && m.getSender().getId().equals(viewerId), m.getContent(),
                m.getContentType(), m.getMetadata(), m.getClientMessageId(), offset(m.getCreatedAt()));
    }
    private static OffsetDateTime offset(LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime(); }
}
